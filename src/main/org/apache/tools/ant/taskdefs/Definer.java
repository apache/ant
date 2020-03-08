/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.FileUtils;

/**
 * Base class for Taskdef and Typedef - handles all
 * the attributes for Typedef. The uri and class
 * handling is handled by DefBase
 *
 * @since Ant 1.4
 */
public abstract class Definer extends DefBase {

    /**
     * the extension of an antlib file for autoloading.
     * {@value /antlib.xml}
     */
    private static final String ANTLIB_XML = "/antlib.xml";

    private static final ThreadLocal<Map<URL, Location>> RESOURCE_STACK = ThreadLocal.withInitial(HashMap::new);

    private String name;
    private String classname;
    private File file;
    private String resource;
    private boolean restrict = false;

    private   int    format = Format.PROPERTIES;
    private   boolean definerSet = false;
    private   int         onError = OnError.FAIL;
    private   String      adapter;
    private   String      adaptTo;

    private   Class<?>       adapterClass;
    private   Class<?>       adaptToClass;

    /**
     * Enumerated type for onError attribute
     *
     * @see EnumeratedAttribute
     */
    public static class OnError extends EnumeratedAttribute {
        /** Enumerated values */
        public static final int  FAIL = 0, REPORT = 1, IGNORE = 2, FAIL_ALL = 3;

        /**
         * text value of onerror option {@value}
         */
        public static final String POLICY_FAIL = "fail";
        /**
         * text value of onerror option {@value}
         */
        public static final String POLICY_REPORT = "report";
        /**
         * text value of onerror option {@value}
         */
        public static final String POLICY_IGNORE = "ignore";
        /**
         * text value of onerror option {@value}
         */
        public static final String POLICY_FAILALL = "failall";

        /**
         * Constructor
         */
        public OnError() {
            super();
        }

        /**
         * Constructor using a string.
         * @param value the value of the attribute
         */
        public OnError(String value) {
            setValue(value);
        }

        /**
         * get the values
         * @return an array of the allowed values for this attribute.
         */
        @Override
        public String[] getValues() {
            return new String[] {POLICY_FAIL, POLICY_REPORT, POLICY_IGNORE,
                POLICY_FAILALL};
        }
    }

    /**
     * Enumerated type for format attribute
     *
     * @see EnumeratedAttribute
     */
    public static class Format extends EnumeratedAttribute {
        /** Enumerated values */
        public static final int PROPERTIES = 0, XML = 1;

        /**
         * get the values
         * @return an array of the allowed values for this attribute.
         */
        @Override
        public String[] getValues() {
            return new String[] {"properties", "xml"};
        }
    }

    /**
     * The restrict attribute.
     * If this is true, only use this definition in add(X).
     * @param restrict the value to set.
     */
     protected void setRestrict(boolean restrict) {
         this.restrict = restrict;
     }


    /**
     * What to do if there is an error in loading the class.
     * <ul>
     *   <li>error - throw build exception</li>
     *   <li>report - output at warning level</li>
     *   <li>ignore - output at debug level</li>
     * </ul>
     *
     * @param onError an <code>OnError</code> value
     */
    public void setOnError(OnError onError) {
        this.onError = onError.getIndex();
    }

    /**
     * Sets the format of the file or resource
     * @param format the enumerated value - xml or properties
     */
    public void setFormat(Format format) {
        this.format = format.getIndex();
    }

    /**
     * @return the name for this definition
     */
    public String getName() {
        return name;
    }

    /**
     * @return the file containing definitions
     */
    public File getFile() {
        return file;
    }

    /**
     * @return the resource containing definitions
     */
    public String getResource() {
        return resource;
    }


    /**
     * Run the definition.
     *
     * @exception BuildException if an error occurs
     */
    @Override
    public void execute() throws BuildException {
        ClassLoader al = createLoader();

        if (!definerSet) {
            //we arent fully defined yet. this is an error unless
            //we are in an antlib, in which case the resource name is determined
            //automatically.
            //NB: URIs in the ant core package will be "" at this point.
            if (getURI() == null) {
                throw new BuildException(
                        "name, file or resource attribute of "
                                + getTaskName() + " is undefined",
                        getLocation());
            }

            if (getURI().startsWith(MagicNames.ANTLIB_PREFIX)) {
                //convert the URI to a resource
                String uri1 = getURI();
                setResource(makeResourceFromURI(uri1));
            } else {
                throw new BuildException(
                    "Only antlib URIs can be located from the URI alone, not the URI '"
                        + getURI() + "'");
            }
        }

        if (name != null) {
            if (classname == null) {
                throw new BuildException("classname attribute of "
                    + getTaskName() + " element is undefined", getLocation());
            }
            addDefinition(al, name, classname);
        } else {
            if (classname != null) {
                throw new BuildException(
                    "You must not specify classname together with file or resource.",
                    getLocation());
            }
            final Enumeration<URL> urls;
            if (file == null) {
                urls = resourceToURLs(al);
            } else {
                final URL url = fileToURL();
                if (url == null) {
                    return;
                }
                urls = Collections.enumeration(Collections.singleton(url));
            }

            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();

                int fmt = this.format;
                if (url.getPath().toLowerCase(Locale.ENGLISH).endsWith(".xml")) {
                    fmt = Format.XML;
                }

                if (fmt == Format.PROPERTIES) {
                    loadProperties(al, url);
                    break;
                } else if (RESOURCE_STACK.get().get(url) != null) {
                    log("Warning: Recursive loading of " + url
                        + " ignored"
                        + " at " + getLocation()
                        + " originally loaded at "
                        + RESOURCE_STACK.get().get(url),
                        Project.MSG_WARN);
                } else {
                    try {
                        RESOURCE_STACK.get().put(url, getLocation());
                        loadAntlib(al, url);
                    } finally {
                        RESOURCE_STACK.get().remove(url);
                    }
                }
            }
        }
    }

    /**
     * This is where the logic to map from a URI to an antlib resource
     * is kept.
     * @param uri the xml namespace uri that to convert.
     * @return the name of a resource. It may not exist
     */
    public static String makeResourceFromURI(String uri) {
        String path = uri.substring(MagicNames.ANTLIB_PREFIX.length());
        String resource;
        if (path.startsWith("//")) {
            //handle new style full paths to an antlib, in which
            //all but the forward slashes are allowed.
            resource = path.substring("//".length());
            if (!resource.endsWith(".xml")) {
                //if we haven't already named an XML file, it gets antlib.xml
                resource += ANTLIB_XML;
            }
        } else {
            //convert from a package to a path
            resource = path.replace('.', '/') + ANTLIB_XML;
        }
        return resource;
    }

    /**
     * Convert a file to a file: URL.
     *
     * @return the URL, or null if it isn't valid and the active error policy
     * is not to raise a fault
     * @throws BuildException if the file is missing/not a file and the
     * policy requires failure at this point.
     */
    private URL fileToURL() {
        String message = null;
        if (!file.exists()) {
            message = "File " + file + " does not exist";
        }
        if (message == null && !(file.isFile())) {
            message = "File " + file + " is not a file";
        }
        if (message == null) {
            try {
                return FileUtils.getFileUtils().getFileURL(file);
            } catch (Exception ex) {
                message =
                    "File " + file + " cannot use as URL: "
                    + ex.toString();
            }
        }
        // Here if there is an error
        switch (onError) {
            case OnError.FAIL_ALL:
                throw new BuildException(message);
            case OnError.FAIL:
                // Fall Through
            case OnError.REPORT:
                log(message, Project.MSG_WARN);
                break;
            case OnError.IGNORE:
                // log at a lower level
                log(message, Project.MSG_VERBOSE);
                break;
            default:
                // Ignore the problem
                break;
        }
        return null;
    }

    private Enumeration<URL> resourceToURLs(ClassLoader classLoader) {
        Enumeration<URL> ret;
        try {
            ret = classLoader.getResources(resource);
        } catch (IOException e) {
            throw new BuildException(
                "Could not fetch resources named " + resource,
                e, getLocation());
        }
        if (!ret.hasMoreElements()) {
            String message = "Could not load definitions from resource "
                + resource + ". It could not be found.";
            switch (onError) {
                case OnError.FAIL_ALL:
                    throw new BuildException(message);
                case OnError.FAIL:
                case OnError.REPORT:
                    log(message, Project.MSG_WARN);
                    break;
                case OnError.IGNORE:
                    log(message, Project.MSG_VERBOSE);
                    break;
                default:
                    // Ignore the problem
                    break;
            }
        }
        return ret;
    }

    /**
     * Load type definitions as properties from a URL.
     *
     * @param al the classloader to use
     * @param url the url to get the definitions from
     */
    protected void loadProperties(ClassLoader al, URL url) {
        try (InputStream is = url.openStream()) {
            if (is == null) {
                log("Could not load definitions from " + url,
                    Project.MSG_WARN);
                return;
            }
            Properties props = new Properties();
            props.load(is);
            for (String key : props.stringPropertyNames()) {
                name = key;
                classname = props.getProperty(name);
                addDefinition(al, name, classname);
            }
        } catch (IOException ex) {
            throw new BuildException(ex, getLocation());
        }
    }

    /**
     * Load an antlib from a URL.
     *
     * @param classLoader the classloader to use.
     * @param url the url to load the definitions from.
     */
    private void loadAntlib(ClassLoader classLoader, URL url) {
        try {
            Antlib antlib = Antlib.createAntlib(getProject(), url, getURI());
            antlib.setClassLoader(classLoader);
            antlib.setURI(getURI());
            antlib.execute();
        } catch (BuildException ex) {
            throw ProjectHelper.addLocationToBuildException(
                ex, getLocation());
        }
    }

    /**
     * Name of the property file  to load
     * ant name/classname pairs from.
     * @param file the file
     */
    public void setFile(File file) {
        if (definerSet) {
            tooManyDefinitions();
        }
        definerSet = true;
        this.file = file;
    }

    /**
     * Name of the property resource to load
     * ant name/classname pairs from.
     * @param res the resource to use
     */
    public void setResource(String res) {
        if (definerSet) {
            tooManyDefinitions();
        }
        definerSet = true;
        this.resource = res;
    }

    /**
     * Antlib attribute, sets resource and uri.
     * uri is set the antlib value and, resource is set
     * to the antlib.xml resource in the classpath.
     * For example antlib="antlib:org.acme.bland.cola"
     * corresponds to uri="antlib:org.acme.bland.cola"
     * resource="org/acme/bland/cola/antlib.xml".
     * ASF Bugzilla Bug 31999
     * @param antlib the value to set.
     */
    public void setAntlib(String antlib) {
        if (definerSet) {
            tooManyDefinitions();
        }
        if (!antlib.startsWith(MagicNames.ANTLIB_PREFIX)) {
            throw new BuildException(
                "Invalid antlib attribute - it must start with " + MagicNames.ANTLIB_PREFIX);
        }
        setURI(antlib);
        this.resource = antlib.substring(MagicNames.ANTLIB_PREFIX.length()).replace('.', '/')
            + "/antlib.xml";
        definerSet = true;
    }

    /**
     * Name of the definition
     * @param name the name of the definition
     */
    public void setName(String name) {
        if (definerSet) {
            tooManyDefinitions();
        }
        definerSet = true;
        this.name = name;
    }

    /**
     * Returns the classname of the object we are defining.
     * May be <code>null</code>.
     * @return the class name
     */
    public String getClassname() {
        return classname;
    }

    /**
     * The full class name of the object being defined.
     * Required, unless file or resource have
     * been specified.
     * @param classname the name of the class
     */
    public void setClassname(String classname) {
        this.classname = classname;
    }

    /**
     * Set the class name of the adapter class.
     * An adapter class is used to proxy the
     * definition class. It is used if the
     * definition class is not assignable to
     * the adaptto class, or if the adaptto
     * class is not present.
     *
     * @param adapter the name of the adapter class
     */

    public void setAdapter(String adapter) {
        this.adapter = adapter;
    }

    /**
     * Set the adapter class.
     *
     * @param adapterClass the class to use to adapt the definition class
     */
    protected void setAdapterClass(Class<?> adapterClass) {
        this.adapterClass = adapterClass;
    }

    /**
     * Set the classname of the class that the definition
     * must be compatible with, either directly or
     * by use of the adapter class.
     *
     * @param adaptTo the name of the adaptto class
     */
    public void setAdaptTo(String adaptTo) {
        this.adaptTo = adaptTo;
    }

    /**
     * Set the class for adaptToClass, to be
     * used by derived classes, used instead of
     * the adaptTo attribute.
     *
     * @param adaptToClass the class for adaptor.
     */
    protected void setAdaptToClass(Class<?> adaptToClass) {
        this.adaptToClass = adaptToClass;
    }

    /**
     * Add a definition using the attributes of Definer
     *
     * @param al the ClassLoader to use
     * @param name the name of the definition
     * @param classname the classname of the definition
     * @exception BuildException if an error occurs
     */
    protected void addDefinition(ClassLoader al, String name, String classname)
        throws BuildException {
        Class<?> cl = null;
        try {
            try {
                name = ProjectHelper.genComponentName(getURI(), name);

                if (onError != OnError.IGNORE) {
                    cl = Class.forName(classname, true, al);
                }

                if (adapter != null) {
                    adapterClass = Class.forName(adapter, true, al);
                }

                if (adaptTo != null) {
                    adaptToClass = Class.forName(adaptTo, true, al);
                }

                AntTypeDefinition def = new AntTypeDefinition();
                def.setName(name);
                def.setClassName(classname);
                def.setClass(cl);
                def.setAdapterClass(adapterClass);
                def.setAdaptToClass(adaptToClass);
                def.setRestrict(restrict);
                def.setClassLoader(al);
                if (cl != null) {
                    def.checkClass(getProject());
                }
                ComponentHelper.getComponentHelper(getProject())
                        .addDataTypeDefinition(def);
            } catch (ClassNotFoundException cnfe) {
                throw new BuildException(
                    getTaskName() + " class " + classname
                        + " cannot be found\n using the classloader " + al,
                    cnfe, getLocation());
            } catch (NoClassDefFoundError ncdfe) {
                throw new BuildException(
                    getTaskName() + " A class needed by class " + classname
                        + " cannot be found: " + ncdfe.getMessage()
                        + "\n using the classloader " + al,
                    ncdfe, getLocation());
            }
        } catch (BuildException ex) {
            switch (onError) {
                case OnError.FAIL_ALL:
                case OnError.FAIL:
                    throw ex;
                case OnError.REPORT:
                    log(ex.getLocation() + "Warning: " + ex.getMessage(),
                        Project.MSG_WARN);
                    break;
                default:
                    log(ex.getLocation() + ex.getMessage(),
                        Project.MSG_DEBUG);
            }
        }
    }

    /**
     * handle too many definitions by raising an exception.
     * @throws BuildException always.
     */
    private void tooManyDefinitions() {
        throw new BuildException(
            "Only one of the attributes name, file and resource can be set",
            getLocation());
    }
}
