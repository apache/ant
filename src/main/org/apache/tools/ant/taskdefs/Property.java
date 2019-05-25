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
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.property.ResolvePropertyMap;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileUtils;

/**
 * <p>Sets a property by name, or set of properties (from file or
 * resource) in the project.  </p>
 * <p>Properties are immutable: whoever sets a property first freezes it for the
 * rest of the build; they are most definitely not variable.
 * <p>There are seven ways to set properties:</p>
 * <ul>
 *   <li>By supplying both the <i>name</i> and <i>value</i> attribute.</li>
 *   <li>By supplying the <i>name</i> and nested text.</li>
 *   <li>By supplying both the <i>name</i> and <i>refid</i> attribute.</li>
 *   <li>By setting the <i>file</i> attribute with the filename of the property
 *     file to load. This property file has the format as defined by the file used
 *     in the class java.util.Properties.</li>
 *   <li>By setting the <i>url</i> attribute with the url from which to load the
 *     properties. This url must be directed to a file that has the format as defined
 *     by the file used in the class java.util.Properties.</li>
 *   <li>By setting the <i>resource</i> attribute with the resource name of the
 *     property file to load. This property file has the format as defined by the
 *     file used in the class java.util.Properties.</li>
 *   <li>By setting the <i>environment</i> attribute with a prefix to use.
 *     Properties will be defined for every environment variable by
 *     prefixing the supplied name and a period to the name of the variable.</li>
 *   <li>By setting the <i>runtime</i> attribute with a prefix to use.
 *     Properties <code>prefix.availableProcessors</code>,
 *     <code>prefix.freeMemory</code>, <code>prefix.totalMemory</code>
 *     and <code>prefix.maxMemory</code> will be defined with values
 *     that correspond to the corresponding methods of the {@link
 *     Runtime} class.</li>
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
    private String runtime;
    private Project fallback;
    private Object untypedValue;
    private boolean valueAttributeUsed = false;
    private boolean relative = false;
    private File basedir;
    private boolean prefixValues = false;

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
     * Sets 'relative' attribute.
     * @param relative new value
     * @since Ant 1.8.0
     */
    public void setRelative(boolean relative) {
        this.relative = relative;
    }

    /**
     * Sets 'basedir' attribute.
     * @param basedir new value
     * @since Ant 1.8.0
     */
    public void setBasedir(File basedir) {
        this.basedir = basedir;
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
        if (relative) {
            internalSetValue(location);
        } else {
            setValue(location.getAbsolutePath());
        }
    }

    /* the following method is first in source so IH will pick it up first:
     * Hopefully we'll never get any classes compiled by wise-guy compilers that behave otherwise...
     */

    /**
     * Set the value of the property.
     * @param value the value to use.
     */
    public void setValue(Object value) {
        valueAttributeUsed = true;
        internalSetValue(value);
    }

    private void internalSetValue(Object value) {
        this.untypedValue = value;
        //preserve protected string value for subclasses :(
        this.value = value == null ? null : value.toString();
    }

    /**
     * Set the value of the property as a String.
     * @param value value to assign
     *
     * @ant.attribute group="name"
     */
    public void setValue(String value) {
        setValue((Object) value);
    }

    /**
     * Set a (multiline) property as nested text.
     * @param msg the text to append to the output text
     * @since Ant 1.8.0
     */
    public void addText(String msg) {
        if (!valueAttributeUsed) {
            msg = getProject().replaceProperties(msg);
            String currentValue = getValue();
            if (currentValue != null) {
                msg = currentValue + msg;
            }
            internalSetValue(msg);
        } else if (!msg.trim().isEmpty()) {
            throw new BuildException("can't combine nested text with value attribute");
        }
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
        if (prefix != null && !prefix.endsWith(".")) {
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
     * Whether to apply the prefix when expanding properties on the
     * right hand side of a properties file as well.
     *
     * @param b boolean
     * @since Ant 1.8.2
     */
    public void setPrefixValues(boolean b) {
        prefixValues = b;
    }

    /**
     * Whether to apply the prefix when expanding properties on the
     * right hand side of a properties file as well.
     *
     * @return boolean
     * @since Ant 1.8.2
     */
    public boolean getPrefixValues() {
        return prefixValues;
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
     * this functionality is supported on ;).
     * </p>
     * Note also that properties are case sensitive, even if the
     * environment variables on your operating system are not, e.g. it
     * will be ${env.Path} not ${env.PATH} on Windows 2000.
     *
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
     * Prefix to use when retrieving Runtime properties.
     *
     * <p>Properties <code>prefix.availableProcessors</code>,
     * <code>prefix.freeMemory</code>, <code>prefix.totalMemory</code>
     * and <code>prefix.maxMemory</code> will be defined with values
     * that correspond to the corresponding methods of the {@link
     * Runtime} class.</p>
     *
     * <p>Note that if you supply a prefix name with a final
     * &quot;.&quot; it will not be doubled. ie
     * runtime=&quot;myrt.&quot; will still allow access of property
     * through &quot;myrt.availableProcessors&quot; and
     * &quot;myrt.freeMemory&quot;.</p>
     *
     * <p>The property values are snapshots taken at the point in time
     * when the <code>property</code> has been executed.</p>
     *
     * @param prefix prefix
     *
     * @ant.attribute group="noname"
     * @since Ant 1.10.4
     */
    public void setRuntime(String prefix) {
        this.runtime = prefix;
    }

    /**
     * Get the runtime attribute.
     * @return the runtime attribute
     * @since Ant 1.10.4
     */
    public String getRuntime() {
        return runtime;
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
    @Deprecated
    public void setUserProperty(boolean userProperty) {
        log("DEPRECATED: Ignoring request to set user property in Property"
            + " task.", Project.MSG_WARN);
    }

    /**
     * get the value of this property
     * @return the current value or the empty string
     */
    @Override
    public String toString() {
        return value == null ? "" : value;
    }

    /**
     * set the property in the project to the value.
     * if the task was give a file, resource, env or runtime attribute
     * here is where it is loaded
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        if (getProject() == null) {
            throw new IllegalStateException("project has not been set");
        }

        if (name != null) {
            if (untypedValue == null && ref == null) {
                throw new BuildException(
                    "You must specify value, location or refid with the name attribute",
                    getLocation());
            }
        } else {
            if (url == null && file == null && resource == null
                && env == null && runtime == null) {
                throw new BuildException(
                    "You must specify url, file, resource, environment or runtime when not using the name attribute",
                    getLocation());
            }
        }

        if (url == null && file == null && resource == null && prefix != null) {
            throw new BuildException(
                "Prefix is only valid when loading from a url, file or resource",
                getLocation());
        }

        if (name != null && untypedValue != null) {
            if (relative) {
                try {
                    File from =
                        untypedValue instanceof File ? (File) untypedValue
                            : new File(untypedValue.toString());
                    File to = basedir != null ? basedir : getProject().getBaseDir();
                    String relPath = FileUtils.getRelativePath(to, from);
                    relPath = relPath.replace('/', File.separatorChar);
                    addProperty(name, relPath);
                } catch (Exception e) {
                    throw new BuildException(e, getLocation());
                }
            } else {
                addProperty(name, untypedValue);
            }
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

        if (runtime != null) {
            loadRuntime(runtime);
        }

        if (name != null && ref != null) {
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
            try (InputStream is = url.openStream()) {
                loadProperties(props, is, url.getFile().endsWith(".xml"));
            }
            addProperties(props);
        } catch (IOException ex) {
            throw new BuildException(ex, getLocation());
        }
    }

    /**
     * Loads the properties defined in the InputStream into the given
     * property. On Java5+ it supports reading from XML based property
     * definition.
     * @param props The property object to load into
     * @param is    The input stream from where to load
     * @param isXml <code>true</code> if we should try to load from xml
     * @throws IOException if something goes wrong
     * @since 1.8.0
     * @see "http://java.sun.com/dtd/properties.dtd"
     * @see java.util.Properties#loadFromXML(InputStream)
     */
    private void loadProperties(
                                Properties props, InputStream is, boolean isXml) throws IOException {
        if (isXml) {
            // load the xml based property definition
            props.loadFromXML(is);
        } else {
            // load ".properties" format
            props.load(is);
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
                try (InputStream fis = Files.newInputStream(file.toPath())) {
                    loadProperties(props, fis, file.getName().endsWith(".xml"));
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
        ClassLoader cL = (classpath == null) ? this.getClass().getClassLoader()
                : getProject().createClassLoader(classpath);
        try (InputStream is = (cL == null) ? ClassLoader.getSystemResourceAsStream(name)
                : cL.getResourceAsStream(name)) {
            if (is == null) {
                log("Unable to find resource " + name, Project.MSG_WARN);
            } else {
                loadProperties(props, is, name.endsWith(".xml"));
                addProperties(props);
            }
        } catch (IOException ex) {
            throw new BuildException(ex, getLocation());
        } finally {
            if (classpath != null && cL != null) {
                ((AntClassLoader) cL).cleanup();
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
        Map<String, String> osEnv = Execute.getEnvironmentVariables();
        for (Map.Entry<String, String> entry : osEnv.entrySet()) {
            props.put(prefix + entry.getKey(), entry.getValue());
        }
        addProperties(props);
    }

    /**
     * load the runtime values
     * @param prefix prefix to place before them
     * @since 1.10.4
     */
    protected void loadRuntime(String prefix) {
        Properties props = new Properties();
        if (!prefix.endsWith(".")) {
            prefix += ".";
        }
        log("Loading Runtime properties " + prefix, Project.MSG_VERBOSE);
        Runtime r = Runtime.getRuntime();
        props.put(prefix + "availableProcessors", String.valueOf(r.availableProcessors()));
        props.put(prefix + "freeMemory", String.valueOf(r.freeMemory()));
        props.put(prefix + "maxMemory", String.valueOf(r.maxMemory()));
        props.put(prefix + "totalMemory", String.valueOf(r.totalMemory()));
        addProperties(props);
    }

    /**
     * iterate through a set of properties,
     * resolve them then assign them
     * @param props the properties to iterate over
     */
    protected void addProperties(Properties props) {
        Map<String, Object> m = new HashMap<>();
        props.forEach((k, v) -> {
            if (k instanceof String) {
                m.put((String) k, v);
            }
        });
        resolveAllProperties(m);
        m.forEach((k, v) -> {
            String propertyName = prefix == null ? k : prefix + k;
            addProperty(propertyName, v);
        });
    }

    /**
     * add a name value pair to the project property set
     * @param n name of property
     * @param v value to set
     */
    protected void addProperty(String n, String v) {
        addProperty(n, (Object) v);
    }

    /**
     * add a name value pair to the project property set
     * @param n name of property
     * @param v value to set
     * @since Ant 1.8
     */
    protected void addProperty(String n, Object v) {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(getProject());
        if (userProperty) {
            if (ph.getUserProperty(n) == null) {
                ph.setInheritedProperty(n, v);
            } else {
                log("Override ignored for " + n, Project.MSG_VERBOSE);
            }
        } else {
            ph.setNewProperty(n, v);
        }
    }

    /**
     * resolve properties inside a properties hashtable
     * @param props properties object to resolve
     */
    private void resolveAllProperties(Map<String, Object> props) throws BuildException {
        PropertyHelper propertyHelper
            = PropertyHelper.getPropertyHelper(getProject());
        new ResolvePropertyMap(
                               getProject(),
                               propertyHelper,
                               propertyHelper.getExpanders())
            .resolveAllProperties(props, getPrefix(), getPrefixValues());
    }

}
