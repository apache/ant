/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Base class for Taskdef and Typedef - does all the classpath
 * handling and and class loading.
 *
 * @author Costin Manolache
 * @author Stefan Bodewig
 *
 * @since Ant 1.4
 */
public abstract class Definer extends Task {
    private String name;
    private String classname;
    private File file;
    private String resource;
    private ClasspathUtils.Delegate cpDelegate;

    private   boolean definerSet = false;
    private   ClassLoader internalClassLoader;
    private   int         onError = OnError.FAIL;
    private   String      adapter;
    private   String      adaptTo;

    private   Class       adapterClass;
    private   Class       adaptToClass;

    /**
     * Enumerated type for onError attribute
     *
     * @see EnumeratedAttribute
     */
    public static class OnError extends EnumeratedAttribute {
        /** Enumerated values */
        public static final int  FAIL = 0, REPORT = 1, IGNORE = 2;
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
        public String[] getValues() {
            return new String[] {"fail", "report", "ignore"};
        }
    }

    /**
     * What to do if there is an error in loading the class.
     * <dl>
     *   <li>error - throw build exception</li>
     *   <li>report - output at warning level</li>
     *   <li>ignore - output at debug level</li>
     * </dl>
     *
     * @param onError an <code>OnError</code> value
     */
    public void setOnError(OnError onError) {
        this.onError = onError.getIndex();
    }

    /**
     * @param reverseLoader if true a delegated loader will take precedence over
     *                      the parent
     * @deprecated stop using this attribute
     * @ant.attribute ignore="true"
     */
    public void setReverseLoader(boolean reverseLoader) {
        this.cpDelegate.setReverseLoader(reverseLoader);
        log("The reverseloader attribute is DEPRECATED. It will be removed",
            Project.MSG_WARN);
    }

    /**
     * @return the name for this definition
     */
    public String getName() {
        return name;
    }

    /**
     * @return the class path path for this definition
     */
    public Path getClasspath() {
        return cpDelegate.getClasspath();
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
     * @return the reverse loader attribute of the classpath delegate.
     */
    public boolean isReverseLoader() {
        return cpDelegate.isReverseLoader();
    }

    /**
     * Returns the loader id of the class path Delegate.
     * @return the loader id
     */
    public String getLoaderId() {
        return cpDelegate.getClassLoadId();
    }

    /**
     * Returns the class path id of the class path delegate.
     * @return the class path id
     */
    public String getClasspathId() {
        return cpDelegate.getClassLoadId();
    }

    /**
     * Set the classpath to be used when searching for component being defined
     *
     * @param classpath an Ant Path object containing the classpath.
     */
    public void setClasspath(Path classpath) {
        this.cpDelegate.setClasspath(classpath);
    }

    /**
     * Create the classpath to be used when searching for component being
     * defined
     * @return the classpath of the this definition
     */
    public Path createClasspath() {
        return this.cpDelegate.createClasspath();
    }

    /**
     * reference to a classpath to use when loading the files.
     * To actually share the same loader, set loaderref as well
     * @param r the reference to the classpath
     */
    public void setClasspathRef(Reference r) {
        this.cpDelegate.setClasspathref(r);
    }

    /**
     * Use the reference to locate the loader. If the loader is not
     * found, taskdef will use the specified classpath and register it
     * with the specified name.
     *
     * This allow multiple taskdef/typedef to use the same class loader,
     * so they can be used together. It eliminate the need to
     * put them in the CLASSPATH.
     *
     * @param r the reference to locate the loader.
     * @since Ant 1.5
     */
    public void setLoaderRef(Reference r) {
        this.cpDelegate.setLoaderRef(r);
    }


    /**
     * Run the definition.
     *
     * @exception BuildException if an error occurs
     */
    public void execute() throws BuildException {
        ClassLoader al = createLoader();

        if (!definerSet) {
            throw new BuildException(
                "name, file or resource attribute of "
                + getTaskName() + " is undefined", getLocation());
        }

        if (name != null) {
            if (classname == null) {
                throw new BuildException(
                    "classname attribute of " + getTaskName() + " element "
                    + "is undefined", getLocation());
            }
            addDefinition(al, name, classname);
        } else {
            if (classname != null) {
                String msg = "You must not specify classname "
                    + "together with file or resource.";
                throw new BuildException(msg, getLocation());
            }
            URL url = null;
            if (file != null) {
                url = fileToURL();
            }
            if (resource != null) {
                url = resourceToURL(al);
            }

            if (url == null) {
                return;
            }

            loadProperties(al, url);
        }
    }

    private URL fileToURL() {
        if (!(file.exists())) {
            log("File " + file + " does not exist", Project.MSG_WARN);
            return null;
        }
        if (!(file.isFile())) {
            log("File " + file + " is not a file", Project.MSG_WARN);
            return null;
        }
        try {
            return file.toURL();
        } catch (Exception ex) {
            log("File " + file + " cannot use as URL: "
                + ex.toString(), Project.MSG_WARN);
            return null;
        }
    }

    private URL resourceToURL(ClassLoader classLoader) {
        URL ret = classLoader.getResource(resource);
        if (ret == null) {
            if (onError != OnError.IGNORE) {
                log("Could not load definitions from resource "
                    + resource + ". It could not be found.",
                    Project.MSG_WARN);
            }
        }
        return ret;
    }

    /**
     * Load type definitions as properties from a url.
     *
     * @param al the classloader to use
     * @param url the url to get the definitions from
     */
    protected void loadProperties(ClassLoader al, URL url) {
        InputStream is = null;
        try {
            is = url.openStream();
            if (is == null) {
                log("Could not load definitions from " + url,
                    Project.MSG_WARN);
                return;
            }
            Properties props = new Properties();
            props.load(is);
            Enumeration keys = props.keys();
            while (keys.hasMoreElements()) {
                name = ((String) keys.nextElement());
                classname = props.getProperty(name);
                addDefinition(al, name, classname);
            }
        } catch (IOException ex) {
            throw new BuildException(ex, getLocation());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * create a classloader for this definition
     * @return the classloader from the cpDelegate
     */
    protected ClassLoader createLoader() {
        if (internalClassLoader != null) {
            return internalClassLoader;
        }
        ClassLoader al = this.cpDelegate.getClassLoader();
        // need to load Task via system classloader or the new
        // task we want to define will never be a Task but always
        // be wrapped into a TaskAdapter.
        ((AntClassLoader) al).addSystemPackageRoot("org.apache.tools.ant");

        return al;
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
    protected void setAdapterClass(Class adapterClass) {
        this.adapterClass = adapterClass;
    }

    /**
     * Set the classname of the class that the definition
     * must be compatible with, either directly or
     * by use of the adapeter class.
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
     * @param adaptToClass the class for adapto.
     */
    protected void setAdaptToClass(Class adaptToClass) {
        this.adaptToClass = adaptToClass;
    }


    /**
     * Set the class loader, overrides the cpDelagate
     * classloader.
     *
     * @param classLoader a <code>ClassLoader</code> value
     */
    protected void setInternalClassLoader(ClassLoader classLoader) {
        this.internalClassLoader = classLoader;
    }

    /**
     * @see org.apache.tools.ant.Task#init()
     * @since Ant 1.6
     */
    public void init() throws BuildException {
        this.cpDelegate = ClasspathUtils.getDelegate(this);
        super.init();
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
        Class cl = null;
        try {
            try {
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
                def.setClassLoader(al);
                if (cl != null) {
                    def.checkClass(getProject());
                }
                ComponentHelper.getComponentHelper(getProject())
                    .addDataTypeDefinition(def);
            } catch (ClassNotFoundException cnfe) {
                String msg = getTaskName() + " class " + classname
                    + " cannot be found";
                throw new BuildException(msg, cnfe, getLocation());
            } catch (NoClassDefFoundError ncdfe) {
                String msg = getTaskName() + "A class needed by class "
                    + classname + " cannot be found: " + ncdfe.getMessage();
                throw new BuildException(msg, ncdfe, location);
            }
        } catch (BuildException ex) {
            switch (onError) {
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

    private void tooManyDefinitions() {
        throw new BuildException(
            "Only one of the attributes name,file,resource"
            + " can be set", getLocation());
    }
}
