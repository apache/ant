/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Sets a property by name, or set of properties (from file or
 * resource) in the project.  </p>
 * Properties are immutable: whoever sets a property first freezes it for the
 * rest of the build; they are most definitely not variable.
 * <p>There are five ways to set properties:</p>
 * <ul>
 *   <li>By supplying both the <i>name</i> and <i>value</i> attribute.</li>
 *   <li>By supplying both the <i>name</i> and <i>refid</i> attribute.</li>
 *   <li>By setting the <i>file</i> attribute with the filename of the property
 *     file to load. This property file has the format as defined by the file used
 *     in the class java.util.Properties.</li>
 *   <li>By setting the <i>resource</i> attribute with the resource name of the
 *     property file to load. This property file has the format as defined by the
 *     file used in the class java.util.Properties.</li>
 *   <li>By setting the <i>environment</i> attribute with a prefix to use.
 *     Properties will be defined for every environment variable by
 *     prefixing the supplied name and a period to the name of the variable.</li>
 * </ul>
 * <p>Although combinations of these ways are possible, only one should be used
 * at a time. Problems might occur with the order in which properties are set, for
 * instance.</p>
 * <p>The value part of the properties being set, might contain references to other
 * properties. These references are resolved at the time these properties are set.
 * This also holds for properties loaded from a property file.</p>
 * Properties are case sensitive.
 *
 * @since Ant 1.1
 *
 * @ant.attribute.group name="name" description="One of these, when using the name attribute"
 * @ant.attribute.group name="noname" description="One of these, when not using the name attribute"
 * @ant.task category="property"
 */
public class Property extends Task {

    // CheckStyle:VisibilityModifier OFF - bc
    protected String name;
    protected String value;
    protected File file;
    protected URL url;
    protected String resource;
    protected Path classpath;
    protected String env;
    protected Reference ref;
    protected String prefix;
    private Project fallback;

    protected boolean userProperty; // set read-only properties
    // CheckStyle:VisibilityModifier ON

    /**
     * Constructor for Property.
     */
    public Property() {
        this(false);
    }

    /**
     * Constructor for Property.
     * @param userProperty if true this is a user property
     * @since Ant 1.5
     */
    protected Property(boolean userProperty) {
        this(userProperty, null);
    }

    /**
     * Constructor for Property.
     * @param userProperty if true this is a user property
     * @param fallback a project to use to look for references if the reference is
     *                 not in the current project
     * @since Ant 1.5
     */
    protected Property(boolean userProperty, Project fallback) {
        this.userProperty = userProperty;
        this.fallback = fallback;
    }

    /**
     * The name of the property to set.
     * @param name property name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the property name.
     * @return the property name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the property to the absolute filename of the
     * given file. If the value of this attribute is an absolute path, it
     * is left unchanged (with / and \ characters converted to the
     * current platforms conventions). Otherwise it is taken as a path
     * relative to the project's basedir and expanded.
     * @param location path to set
     *
     * @ant.attribute group="name"
     */
    public void setLocation(File location) {
        setValue(location.getAbsolutePath());
    }

    /**
     * The value of the property.
     * @param value value to assign
     *
     * @ant.attribute group="name"
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get the property value.
     * @return the property value
     */
    public String getValue() {
        return value;
    }

    /**
     * Filename of a property file to load.
     * @param file filename
     *
     * @ant.attribute group="noname"
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Get the file attribute.
     * @return the file attribute
     */
    public File getFile() {
        return file;
    }

    /**
     * The url from which to load properties.
     * @param url url string
     *
     * @ant.attribute group="noname"
     */
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
     * Get the url attribute.
     * @return the url attribute
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Prefix to apply to properties loaded using <code>file</code>
     * or <code>resource</code>.
     * A "." is appended to the prefix if not specified.
     * @param prefix prefix string
     * @since Ant 1.5
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
        if (!prefix.endsWith(".")) {
            this.prefix += ".";
        }
    }

    /**
     * Get the prefix attribute.
     * @return the prefix attribute
     * @since Ant 1.5
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets a reference to an Ant datatype
     * declared elsewhere.
     * Only yields reasonable results for references
     * PATH like structures or properties.
     * @param ref reference
     *
     * @ant.attribute group="name"
     */
    public void setRefid(Reference ref) {
        this.ref = ref;
    }

    /**
     * Get the refid attribute.
     * @return the refid attribute
     */
    public Reference getRefid() {
        return ref;
    }

    /**
     * The resource name of a property file to load
     * @param resource resource on classpath
     *
     * @ant.attribute group="noname"
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * Get the resource attribute.
     * @return the resource attribute
     */
    public String getResource() {
        return resource;
    }

    /**
     * Prefix to use when retrieving environment variables.
     * Thus if you specify environment=&quot;myenv&quot;
     * you will be able to access OS-specific
     * environment variables via property names &quot;myenv.PATH&quot; or
     * &quot;myenv.TERM&quot;.
     * <p>
     * Note that if you supply a property name with a final
     * &quot;.&quot; it will not be doubled. ie environment=&quot;myenv.&quot; will still
     * allow access of environment variables through &quot;myenv.PATH&quot; and
     * &quot;myenv.TERM&quot;. This functionality is currently only implemented
     * on select platforms. Feel free to send patches to increase the number of platforms
     * this functionality is supported on ;).<br>
     * Note also that properties are case sensitive, even if the
     * environment variables on your operating system are not, e.g. it
     * will be ${env.Path} not ${env.PATH} on Windows 2000.
     * @param env prefix
     *
     * @ant.attribute group="noname"
     */
    public void setEnvironment(String env) {
        this.env = env;
    }

    /**
     * Get the environment attribute.
     * @return the environment attribute
     * @since Ant 1.5
     */
    public String getEnvironment() {
        return env;
    }

    /**
     * The classpath to use when looking up a resource.
     * @param classpath to add to any existing classpath
     */
    public void setClasspath(Path classpath) {
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }

    /**
     * The classpath to use when looking up a resource.
     * @return a path to be configured
     */
    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        return this.classpath.createPath();
    }

    /**
     * the classpath to use when looking up a resource,
     * given as reference to a &lt;path&gt; defined elsewhere
     * @param r a reference to a classpath
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Get the classpath used when looking up a resource.
     * @return the classpath
     * @since Ant 1.5
     */
    public Path getClasspath() {
        return classpath;
    }

    /**
     * @param userProperty ignored
     * @deprecated since 1.5.x.
     *             This was never a supported feature and has been
     *             deprecated without replacement.
     * @ant.attribute ignore="true"
     */
    public void setUserProperty(boolean userProperty) {
        log("DEPRECATED: Ignoring request to set user property in Property"
            + " task.", Project.MSG_WARN);
    }

    /**
     * get the value of this property
     * @return the current value or the empty string
     */
    public String toString() {
        return value == null ? "" : value;
    }

    /**
     * set the property in the project to the value.
     * if the task was give a file, resource or env attribute
     * here is where it is loaded
     * @throws BuildException on error
     */
    public void execute() throws BuildException {
        if (getProject() == null) {
            throw new IllegalStateException("project has not been set");
        }

        if (name != null) {
            if (value == null && ref == null) {
                throw new BuildException("You must specify value, location or "
                                         + "refid with the name attribute",
                                         getLocation());
            }
        } else {
            if (url == null && file == null && resource == null && env == null) {
                throw new BuildException("You must specify url, file, resource or "
                                         + "environment when not using the "
                                         + "name attribute", getLocation());
            }
        }

        if (url == null && file == null && resource == null && prefix != null) {
            throw new BuildException("Prefix is only valid when loading from "
                                     + "a url, file or resource", getLocation());
        }

        if ((name != null) && (value != null)) {
            addProperty(name, value);
        }

        if (file != null) {
            loadFile(file);
        }

        if (url != null) {
            loadUrl(url);
        }

        if (resource != null) {
            loadResource(resource);
        }

        if (env != null) {
            loadEnvironment(env);
        }

        if ((name != null) && (ref != null)) {
            try {
                addProperty(name,
                            ref.getReferencedObject(getProject()).toString());
            } catch (BuildException be) {
                if (fallback != null) {
                    addProperty(name,
                                ref.getReferencedObject(fallback).toString());
                } else {
                    throw be;
                }
            }
        }
    }

    /**
     * load properties from a url
     * @param url url to load from
     * @throws BuildException on error
     */
    protected void loadUrl(URL url) throws BuildException {
        Properties props = new Properties();
        log("Loading " + url, Project.MSG_VERBOSE);
        try {
            InputStream is = url.openStream();
            try {
                props.load(is);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
            addProperties(props);
        } catch (IOException ex) {
            throw new BuildException(ex, getLocation());
        }
    }


    /**
     * load properties from a file
     * @param file file to load
     * @throws BuildException on error
     */
    protected void loadFile(File file) throws BuildException {
        Properties props = new Properties();
        log("Loading " + file.getAbsolutePath(), Project.MSG_VERBOSE);
        try {
            if (file.exists()) {
                FileInputStream  fis = null;
                try {
                    fis = new FileInputStream(file);
                    props.load(fis);
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }
                addProperties(props);
            } else {
                log("Unable to find property file: " + file.getAbsolutePath(),
                    Project.MSG_VERBOSE);
            }
        } catch (IOException ex) {
            throw new BuildException(ex, getLocation());
        }
    }

    /**
     * load properties from a resource in the current classpath
     * @param name name of resource to load
     */
    protected void loadResource(String name) {
        Properties props = new Properties();
        log("Resource Loading " + name, Project.MSG_VERBOSE);
        InputStream is = null;
        try {
            ClassLoader cL = null;

            if (classpath != null) {
                cL = getProject().createClassLoader(classpath);
            } else {
                cL = this.getClass().getClassLoader();
            }

            if (cL == null) {
                is = ClassLoader.getSystemResourceAsStream(name);
            } else {
                is = cL.getResourceAsStream(name);
            }

            if (is != null) {
                props.load(is);
                addProperties(props);
            } else {
                log("Unable to find resource " + name, Project.MSG_WARN);
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
     * load the environment values
     * @param prefix prefix to place before them
     */
    protected void loadEnvironment(String prefix) {
        Properties props = new Properties();
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }
        log("Loading Environment " + prefix, Project.MSG_VERBOSE);
        Vector osEnv = Execute.getProcEnvironment();
        for (Enumeration e = osEnv.elements(); e.hasMoreElements();) {
            String entry = (String) e.nextElement();
            int pos = entry.indexOf('=');
            if (pos == -1) {
                log("Ignoring: " + entry, Project.MSG_WARN);
            } else {
                props.put(prefix + entry.substring(0, pos),
                entry.substring(pos + 1));
            }
        }
        addProperties(props);
    }

    /**
     * iterate through a set of properties,
     * resolve them then assign them
     * @param props the properties to iterate over
     */
    protected void addProperties(Properties props) {
        resolveAllProperties(props);
        Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            String propertyName = (String) e.nextElement();
            String propertyValue = props.getProperty(propertyName);

            String v = getProject().replaceProperties(propertyValue);

            if (prefix != null) {
                propertyName = prefix + propertyName;
            }

            addProperty(propertyName, v);
        }
    }

    /**
     * add a name value pair to the project property set
     * @param n name of property
     * @param v value to set
     */
    protected void addProperty(String n, String v) {
        if (userProperty) {
            if (getProject().getUserProperty(n) == null) {
                getProject().setInheritedProperty(n, v);
            } else {
                log("Override ignored for " + n, Project.MSG_VERBOSE);
            }
        } else {
            getProject().setNewProperty(n, v);
        }
    }

    /**
     * resolve properties inside a properties hashtable
     * @param props properties object to resolve
     */
    private void resolveAllProperties(Properties props) throws BuildException {
        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            String propertyName = (String) e.nextElement();
            Stack referencesSeen = new Stack();
            resolve(props, propertyName, referencesSeen);
        }
    }

    /**
     * Recursively expand the named property using the project's
     * reference table and the given set of properties - fail if a
     * circular definition is detected.
     *
     * @param props properties object to resolve
     * @param name of the property to resolve
     * @param referencesSeen stack of all property names that have
     * been tried to expand before coming here.
     */
    private void resolve(Properties props, String name, Stack referencesSeen)
        throws BuildException {
        if (referencesSeen.contains(name)) {
            throw new BuildException("Property " + name + " was circularly "
                                     + "defined.");
        }

        String propertyValue = props.getProperty(name);
        Vector fragments = new Vector();
        Vector propertyRefs = new Vector();
        PropertyHelper.getPropertyHelper(
            this.getProject()).parsePropertyString(
                propertyValue, fragments, propertyRefs);

        if (propertyRefs.size() != 0) {
            referencesSeen.push(name);
            StringBuffer sb = new StringBuffer();
            Enumeration i = fragments.elements();
            Enumeration j = propertyRefs.elements();
            while (i.hasMoreElements()) {
                String fragment = (String) i.nextElement();
                if (fragment == null) {
                    String propertyName = (String) j.nextElement();
                    fragment = getProject().getProperty(propertyName);
                    if (fragment == null) {
                        if (props.containsKey(propertyName)) {
                            resolve(props, propertyName, referencesSeen);
                            fragment = props.getProperty(propertyName);
                        } else {
                            fragment = "${" + propertyName + "}";
                        }
                    }
                }
                sb.append(fragment);
            }
            propertyValue = sb.toString();
            props.put(name, propertyValue);
            referencesSeen.pop();
        }
    }
}
