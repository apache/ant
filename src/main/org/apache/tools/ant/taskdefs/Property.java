/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;

/**
 * Sets a property by name, or set of properties (from file or
 * resource) in the project.  </p>
 * Properties are immutable: whoever sets a property first freezes it for the
 * rest of the build; they are most definately not variable.
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
 * @author costin@dnt.ro
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:glennm@ca.ibm.com">Glenn McAllister</a>
 * @since Ant 1.1
 */
public class Property extends Task {

    protected String name;
    protected String value;
    protected File file;
    protected String resource;
    protected Path classpath;
    protected String env;
    protected Reference ref;
    protected String prefix;
    private Project fallback;

    protected boolean userProperty; // set read-only properties

    public Property() {
        this(false);
    }

    /**
     * @since Ant 1.5
     */
    protected Property(boolean userProperty) {
        this(userProperty, null);
    }

    /**
     * @since Ant 1.5
     */
    protected Property(boolean userProperty, Project fallback) {
        this.userProperty = userProperty;
        this.fallback = fallback;
    }

    /**
     * sets the name of the property to set.
     * @param name property name
     */
    public void setName(String name) {
        this.name = name;
    }

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
     */
    public void setLocation(File location) {
        setValue(location.getAbsolutePath());
    }

    /**
     * Sets the value of the property.
     * @param value value to assign
     */
    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * the filename of a property file to load.
     *@param file filename
     */
    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
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
     */
    public void setRefid(Reference ref) {
        this.ref = ref;
    }

    public Reference getRefid() {
        return ref;
    }

    /**
     * the resource name of a property file to load
     * @param resource resource on classpath
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }

    /**
    * the prefix to use when retrieving environment variables.
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
    */
    public void setEnvironment(String env) {
        this.env = env;
    }

    /**
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
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * @since Ant 1.5
     */
    public Path getClasspath() {
        return classpath;
    }

    /**
     * @deprecated This was never a supported feature and has been
     * deprecated without replacement
     * @ant.setter skip="true"
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
            if (file == null && resource == null && env == null) {
                throw new BuildException("You must specify file, resource or "
                                         + "environment when not using the "
                                         + "name attribute", getLocation());
            }
        }

        if (file == null && resource == null && prefix != null) {
            throw new BuildException("Prefix is only valid when loading from "
                                     + "a file or resource", getLocation());
        }

        if ((name != null) && (value != null)) {
            addProperty(name, value);
        }

        if (file != null) {
            loadFile(file);
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
     * load properties from a file
     * @param file file to load
     */
    protected void loadFile(File file) throws BuildException {
        Properties props = new Properties();
        log("Loading " + file.getAbsolutePath(), Project.MSG_VERBOSE);
        try {
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                try {
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
                cL = new AntClassLoader(getProject(), classpath);
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
                } catch (IOException e) {}
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
     */
    protected void addProperties(Properties props) {
        resolveAllProperties(props);
        Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = props.getProperty(name);

            String v = getProject().replaceProperties(value);

            if (prefix != null) {
                name = prefix + name;
            }

            addProperty(name, v);
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
            String name = (String) e.nextElement();
            String value = props.getProperty(name);

            boolean resolved = false;
            while (!resolved) {
                Vector fragments = new Vector();
                Vector propertyRefs = new Vector();
                ProjectHelper.parsePropertyString(value, fragments,
                                                  propertyRefs);

                resolved = true;
                if (propertyRefs.size() != 0) {
                    StringBuffer sb = new StringBuffer();
                    Enumeration i = fragments.elements();
                    Enumeration j = propertyRefs.elements();
                    while (i.hasMoreElements()) {
                        String fragment = (String) i.nextElement();
                        if (fragment == null) {
                            String propertyName = (String) j.nextElement();
                            if (propertyName.equals(name)) {
                                throw new BuildException("Property " + name
                                                         + " was circularly "
                                                         + "defined.");
                            }
                            fragment = getProject().getProperty(propertyName);
                            if (fragment == null) {
                                if (props.containsKey(propertyName)) {
                                    fragment = props.getProperty(propertyName);
                                    resolved = false;
                                } else {
                                    fragment = "${" + propertyName + "}";
                                }
                            }
                        }
                        sb.append(fragment);
                    }
                    value = sb.toString();
                    props.put(name, value);
                }
            }
        }
    }
}
