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

import java.io.File;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;

/**
 * Will set the given property if the requested resource is available at 
 * runtime. This task may also be used as a condition by the condition task.
 *
 * @author Stefano Mazzocchi 
 *         <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author Magesh Umasankar
 *
 * @since Ant 1.1
 *
 * @ant.task category="control"
 */
public class Available extends Task implements Condition {

    private String property;
    private String classname;
    private String file;
    private Path filepath;
    private String resource;
    private FileDir type;
    private Path classpath;
    private AntClassLoader loader;
    private String value = "true";
    private boolean isTask = false;
    private boolean ignoreSystemclasses = false;

    /**
     * Set the classpath to be used when searching for classes and resources.
     * 
     * @param classpath an Ant Path object containing the search path.
     */
    public void setClasspath(Path classpath) {
        createClasspath().append(classpath);
    }

    /**
     * Classpath to be used when searching for classes and resources.
     *
     * @return an empty Path instance to be configured by Ant.
     */
    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        return this.classpath.createPath();
    }

    /**
     * Set the classpath by reference.
     *
     * @param r a Reference to a Path instance to be used as the classpath 
     *          value.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Set the path to use when looking for a file.
     *
     * @param filepath a Path instance containing the search path for files.
     */
    public void setFilepath(Path filepath) {
        createFilepath().append(filepath);
    }

    /**
     * Path to search for file resources.
     *
     * @return a new Path instance which Ant will configure with a file search
     *         path.
     */
    public Path createFilepath() {
        if (this.filepath == null) {
            this.filepath = new Path(getProject());
        }
        return this.filepath.createPath();
    }

    /**
     * Set the name of the property which will be set if the particular resource
     * is available.
     *
     * @param property the name of the property to set.
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Set the value to be given to the property if the desired resource is
     * available.
     *
     * @param value the value to be given.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Set a classname of a class which must be available to set the given 
     * property.
     *
     * @param classname the name of the class required.
     */
    public void setClassname(String classname) {
        if (!"".equals(classname)) {
            this.classname = classname;
        }
    }

    /**
     * Set the file which must be present in the file system to set the given
     * property.
     *
     * @param file the name of the file which is required.
     */
    public void setFile(File file) {
        this.file = FileUtils.newFileUtils()
            .removeLeadingPath(getProject().getBaseDir(), file);
    }

    /**
     * Set the name of a Java resource which is required to set the property.
     *
     * @param resource the name of a resource which is required to be available.
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * @deprecated setType(String) is deprecated and is replaced with
     *             setType(Available.FileDir) to make Ant's Introspection
     *             mechanism do the work and also to encapsulate operations on
     *             the type in its own class.
     */
    public void setType(String type) {
        log("DEPRECATED - The setType(String) method has been deprecated."
            + " Use setType(Available.FileDir) instead.");
        this.type = new FileDir();
        this.type.setValue(type);
    }

    /**
     * Set what type of file is required - either directory or file.
     *
     * @param type an instance of the FileDir enumeratedAttribute indicating
     *             whether the file required is to be a directory or a plain 
     *             file.
     */
    public void setType(FileDir type) {
        this.type = type;
    }

    /**
     * Set whether the search for classes should ignore the runtime classes and
     * just use the given classpath.
     *
     * @param ignore true if system classes are to be ignored.
     */
    public void setIgnoresystemclasses(boolean ignore) {
        this.ignoreSystemclasses = ignore;
    }

    /**
     * Entry point when operating as a task.
     *
     * @exception BuildException if the task is not configured correctly.
     */
    public void execute() throws BuildException {
        if (property == null) {
            throw new BuildException("property attribute is required", 
                                     getLocation());
        }

        isTask = true;
        try {
            if (eval()) {
                String oldvalue = getProject().getProperty(property);
                if (null != oldvalue && !oldvalue.equals(value)) {
                    log("DEPRECATED - <available> used to override an existing"
                        + " property."
                        + StringUtils.LINE_SEP
                        + "  Build file should not reuse the same property"
                        + " name for different values.");
                }
                getProject().setProperty(property, value);
            }
        } finally {
            isTask = false;
        }
    }

    /**
     * Evaluate the availability of a resource.
     *
     * @return boolean is the resource is available.
     * @exception BuildException if the condition is not configured correctly
     */
    public boolean eval() throws BuildException {
        if (classname == null && file == null && resource == null) {
            throw new BuildException("At least one of (classname|file|"
                                     + "resource) is required", getLocation());
        }

        if (type != null) {
            if (file == null) {
                throw new BuildException("The type attribute is only valid "
                                         + "when specifying the file "
                                         + "attribute.", getLocation());
            }
        }

        if (classpath != null) {
            classpath.setProject(getProject());
            this.loader = new AntClassLoader(getProject(), classpath);
        }

        String appendix = "";
        if (isTask) {
            appendix = " to set property " + property;
        } else {
            setTaskName("available");
        }

        if ((classname != null) && !checkClass(classname)) {
            log("Unable to load class " + classname + appendix, 
                Project.MSG_VERBOSE);
            return false;
        }

        if ((file != null) && !checkFile()) {
            if (type != null) {
                log("Unable to find " + type + " " + file + appendix, 
                    Project.MSG_VERBOSE);
            } else {
                log("Unable to find " + file + appendix, Project.MSG_VERBOSE);
            }
            return false;
        }

        if ((resource != null) && !checkResource(resource)) {
            log("Unable to load resource " + resource + appendix, 
                Project.MSG_VERBOSE);
            return false;
        }

        if (loader != null) {
            loader.cleanup();
            loader = null;
        }

        if (!isTask) {
            setTaskName(null);
        }

        return true;
    }

    /**
     * Search for file/directory either either relative to project's
     * basedir or in the path given as filepath.
     *
     * <p>filepath can be a list of directory and/or file names (gen'd
     * via <fileset>)</p>
     *
     * <p>look for:</p><ul>
     *   <li>full-pathname specified == path in list</li>
     *   <li>full-pathname specified == parent dir of path in list</li>
     *   <li>simple name specified   == path in list</li>
     *   <li>simple name specified   == path in list + name</li>
     *   <li>simple name specified   == parent dir + name</li>
     *   <li>simple name specified   == parent of parent dir + name</li>
     * </ul>
     */
    private boolean checkFile() {
        if (filepath == null) {
            return checkFile(getProject().resolveFile(file), file);
        } else {
            String[] paths = filepath.list();
            for (int i = 0; i < paths.length; ++i) {
                log("Searching " + paths[i], Project.MSG_DEBUG);
                File path = new File(paths[i]);

                // **   full-pathname specified == path in list
                // **   simple name specified   == path in list
                if (path.exists() && file.equals(paths[i])) {
                    if (type == null) {
                        log("Found: " + path, Project.MSG_VERBOSE);
                        return true;
                    } else if (type.isDir()
                               && path.isDirectory()) {
                        log("Found directory: " + path, Project.MSG_VERBOSE);
                        return true;
                    } else if (type.isFile()
                               && path.isFile()) {
                        log("Found file: " + path, Project.MSG_VERBOSE);
                        return true;
                    }
                    // not the requested type
                    return false;
                }

                FileUtils fileUtils = FileUtils.newFileUtils();
                File parent = fileUtils.getParentFile(path);
                // **   full-pathname specified == parent dir of path in list
                if (parent != null && parent.exists()
                    && file.equals(parent.getAbsolutePath())) {
                    if (type == null) {
                        log("Found: " + parent, Project.MSG_VERBOSE);
                        return true;
                    } else if (type.isDir()) {
                        log("Found directory: " + parent, Project.MSG_VERBOSE);
                        return true;
                    }
                    // not the requested type
                    return false;
                }

                // **   simple name specified   == path in list + name
                if (path.exists() && path.isDirectory()) {
                    if (checkFile(new File(path, file),
                                  file + " in " + path)) {
                        return true;
                    }
                }

                // **   simple name specified   == parent dir + name
                if (parent != null && parent.exists()) {
                    if (checkFile(new File(parent, file),
                                  file + " in " + parent)) {
                        return true;
                    }
                }

                // **   simple name specified   == parent of parent dir + name
                if (parent != null) {
                    File grandParent = fileUtils.getParentFile(parent);
                    if (grandParent != null && grandParent.exists()) {
                        if (checkFile(new File(grandParent, file),
                                      file + " in " + grandParent)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if a given file exists and matches the required type.
     */
    private boolean checkFile(File f, String text) {
        if (type != null) {
            if (type.isDir()) {
                if (f.isDirectory()) {
                    log("Found directory: " + text, Project.MSG_VERBOSE);
                }
                return f.isDirectory();
            } else if (type.isFile()) {
                if (f.isFile()) {
                    log("Found file: " + text, Project.MSG_VERBOSE);
                }
                return f.isFile();
            }
        }
        if (f.exists()) {
            log("Found: " + text, Project.MSG_VERBOSE);
        }
        return f.exists();
    }

    /**
     * Check if a given resource can be loaded.
     */
    private boolean checkResource(String resource) {
        if (loader != null) {
            return (loader.getResourceAsStream(resource) != null);
        } else {
            ClassLoader cL = this.getClass().getClassLoader();
            if (cL != null) {
                return (cL.getResourceAsStream(resource) != null);
            } else {
                return
                    (ClassLoader.getSystemResourceAsStream(resource) != null);
            }
        }
    }

    /**
     * Check if a given class can be loaded.
     */
    private boolean checkClass(String classname) {
        try {
            Class requiredClass = null;
            if (ignoreSystemclasses) {
                loader = new AntClassLoader(null, getProject(), classpath, 
                                            false);
                if (loader != null) {
                    try {
                        requiredClass = loader.findClass(classname);
                    } catch (SecurityException se) {
                        // class found but restricted name; this is
                        // actually the case we're looking for in JDK 1.3+,
                        // so catch the exception and return
                        return true;
                    }
                } else {
                    return false;
                }
            } else if (loader != null) {
                requiredClass = loader.loadClass(classname);
            } else {
                ClassLoader l = this.getClass().getClassLoader();
                // Can return null to represent the bootstrap class loader.
                // see API docs of Class.getClassLoader.
                if (l != null) {
                    requiredClass = l.loadClass(classname);
                } else {
                    requiredClass = Class.forName(classname);
                }
            }
            AntClassLoader.initializeClass(requiredClass);
            return true;
        } catch (ClassNotFoundException e) {
            log("class \"" + classname + "\" was not found",
                Project.MSG_DEBUG);
            return false;
        } catch (NoClassDefFoundError e) {
            log("Could not load dependent class \"" + e.getMessage()
                + "\" for class \"" + classname + "\"",
                Project.MSG_DEBUG);
            return false;
        }
    }

    /**
     * EnumeratedAttribute covering the file types to be checked for, either
     * file or dir.
     */
    public static class FileDir extends EnumeratedAttribute {

        private static final String[] values = {"file", "dir"};

        /**
         * @see EnumeratedAttribute#getValues
         */
        public String[] getValues() {
            return values;
        }

        /**
         * Indicate if the value specifies a directory.
         *
         * @return true if the value specifies a directory.
         */
        public boolean isDir() {
            return "dir".equalsIgnoreCase(getValue());
        }

        /**
         * Indicate if the value specifies a file.
         *
         * @return true if the value specifies a file.
         */
        public boolean isFile() {
            return "file".equalsIgnoreCase(getValue());
        }

    }
}
