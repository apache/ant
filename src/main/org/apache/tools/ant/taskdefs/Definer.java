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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ClasspathUtils;

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
    private String value;
    private File file;
    private String resource;
    private ClasspathUtils.Delegate cpDelegate;

    /**
     * @deprecated stop using this attribute
     * @ant.attribute ignore="true"
     */
    public void setReverseLoader(boolean reverseLoader) {
        this.cpDelegate.setReverseLoader(reverseLoader);
        log("The reverseloader attribute is DEPRECATED. It will be removed",
            Project.MSG_WARN);
    }

    public String getName() {
        return name;
    }

    public Path getClasspath() {
        return cpDelegate.getClasspath();
    }

    public File getFile() {
        return file;
    }

    public String getResource() {
        return resource;
    }

    public boolean isReverseLoader() {
        return cpDelegate.isReverseLoader();
    }

    public String getLoaderId() {
        return cpDelegate.getClassLoadId();
    }

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
     * Create the classpath to be used when searching for component being defined
     */
    public Path createClasspath() {
        return this.cpDelegate.createClasspath();
    }

    /**
     * reference to a classpath to use when loading the files.
     * To actually share the same loader, set loaderref as well
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
     * @since Ant 1.5
     */
    public void setLoaderRef(Reference r) {
        this.cpDelegate.setLoaderRef(r);
    }


    public void execute() throws BuildException {
        ClassLoader al = createLoader();

        if (file == null && resource == null) {

            // simple case - one definition
            if (name == null || value == null) {
                String msg = "name or classname attributes of "
                    + getTaskName() + " element "
                    + "are undefined";
                throw new BuildException(msg);
            }
            addDefinition(al, name, value);

        } else {

            InputStream is = null;
            try {
                if (name != null || value != null) {
                    String msg = "You must not specify name or value "
                        + "together with file or resource.";
                    throw new BuildException(msg, getLocation());
                }

                if (file != null && resource != null) {
                    String msg = "You must not specify both, file and "
                        + "resource.";
                    throw new BuildException(msg, getLocation());
                }


                Properties props = new Properties();
                if (file != null) {
                    log("Loading definitions from file " + file,
                        Project.MSG_VERBOSE);
                    is = new FileInputStream(file);
                    if (is == null) {
                        log("Could not load definitions from file " + file
                            + ". It doesn\'t exist.", Project.MSG_WARN);
                    }
                }
                if (resource != null) {
                    log("Loading definitions from resource " + resource,
                        Project.MSG_VERBOSE);
                    is = al.getResourceAsStream(resource);
                    if (is == null) {
                        log("Could not load definitions from resource "
                            + resource + ". It could not be found.",
                            Project.MSG_WARN);
                    }
                }

                if (is != null) {
                    props.load(is);
                    Enumeration keys = props.keys();
                    while (keys.hasMoreElements()) {
                        String n = (String) keys.nextElement();
                        String v = props.getProperty(n);
                        addDefinition(al, n, v);
                    }
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
    }

    /**
     * create the classloader then hand the definition off to the subclass;
     * @throws BuildException when the class wont load for any reason
     */
    private void addDefinition(ClassLoader al, String name, String value)
        throws BuildException {
        try {
            Class c = al.loadClass(value);
            AntClassLoader.initializeClass(c);
            addDefinition(name, c);
        } catch (ClassNotFoundException cnfe) {
            String msg = getTaskName() + " class " + value
                + " cannot be found";
            throw new BuildException(msg, cnfe, getLocation());
        } catch (NoClassDefFoundError ncdfe) {
            String msg = getTaskName() + ": A class needed by class "
                + value + " cannot be found: " + ncdfe.getMessage();
            throw new BuildException(msg, ncdfe, location);
        }
    }

    /**
     * create a classloader for this definition
     */
    private ClassLoader createLoader() {
        ClassLoader al = this.cpDelegate.getClassLoader();
        // need to load Task via system classloader or the new
        // task we want to define will never be a Task but always
        // be wrapped into a TaskAdapter.
        ((AntClassLoader)al).addSystemPackageRoot("org.apache.tools.ant");

        return al;
    }

    /**
     * Name of the property file  to load
     * ant name/classname pairs from.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Name of the property resource to load
     * ant name/classname pairs from.
     */
    public void setResource(String res) {
        this.resource = res;
    }

    /**
     * Name of the property resource to load
     * ant name/classname pairs from.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the classname of the object we are defining.
     * May be <code>null</code>.
     */
    public String getClassname() {
        return value;
    }

    /**
     * The full class name of the object being defined.
     * Required, unless file or resource have
     * been specified.
     */
    public void setClassname(String v) {
        value = v;
    }

    /**
     * This must be implemented by subclasses; it is the callback
     * they will get to add a new definition of their type.
     */
    protected abstract void addDefinition(String name, Class c);
    
    
    /**
     * @see org.apache.tools.ant.Task#init()
     * @since Ant 1.6
     */
    public void init() throws BuildException {
        this.cpDelegate = ClasspathUtils.getDelegate(this);
        super.init();
    }

}
