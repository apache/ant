/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.*;

/**
 * Will set the given property if the requested resource is available at runtime.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 */

public class Available extends Task implements Condition {

    private String property;
    private String classname;
    private File file;
    private Path filepath;
    private String resource;
    private String type;
    private Path classpath;
    private AntClassLoader loader;
    private String value = "true";

    public void setClasspath(Path classpath) {
        createClasspath().append(classpath);
    }

    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(project);
        }
        return this.classpath.createPath();
    }

    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    public void setFilepath(Path filepath) {
        createFilepath().append(filepath);
    }
    
    public Path createFilepath() {
        if (this.filepath == null) {
            this.filepath = new Path(project);
        }
        return this.filepath.createPath();
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setClassname(String classname) {
        if (!"".equals(classname)) {
            this.classname = classname;
        }
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void execute() throws BuildException {
        if (property == null) {
            throw new BuildException("property attribute is required", location);
        }

        if (eval()) {
            this.project.setProperty(property, value);
        }
    }
        
    public boolean  eval() throws BuildException {
        if (classname == null && file == null && resource == null) {
            throw new BuildException("At least one of (classname|file|resource) is required", location);
        }

        if (type != null){
            if (!type.equalsIgnoreCase("file") && !type.equalsIgnoreCase("dir")){
                throw new BuildException("Type must be one of either dir or file");
            }
        }

        if (classpath != null) {
            classpath.setProject(project);
            this.loader = new AntClassLoader(project, classpath);
        }

        if ((classname != null) && !checkClass(classname)) {
            log("Unable to load class " + classname + " to set property " + property, Project.MSG_VERBOSE);
            return false;
        }
        
        if ((file != null) && !checkFile()) {
            log("Unable to find " + file + " to set property " + property, Project.MSG_VERBOSE);
            return false;
        }
        
        if ((resource != null) && !checkResource(resource)) {
            log("Unable to load resource " + resource + " to set property " + property, Project.MSG_VERBOSE);
            return false;
        }

        if (loader != null) {
            loader.cleanup();
        }

        return true;
    }

    private boolean checkFile() {
        if (filepath == null) {
            return checkFile(file);
        } else {
            String[] paths = filepath.list();
            for(int i = 0; i < paths.length; ++i) {
                log("Searching " + paths[i], Project.MSG_VERBOSE);
                if(new File(paths[i], file.getName()).isFile()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkFile(File file) {
        if (type != null) {
            if (type.equalsIgnoreCase("dir")) {
                return file.isDirectory();
            } else if (type.equalsIgnoreCase("file")) {
                return file.isFile();
            }
        }
        return file.exists();
    }

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

    private boolean checkClass(String classname) {
        try {
            if (loader != null) {
                loader.loadClass(classname);
            } else {
                ClassLoader l = this.getClass().getClassLoader();
                // Can return null to represent the bootstrap class loader.
                // see API docs of Class.getClassLoader.
                if (l != null) {
                    l.loadClass(classname);
                } else {
                    Class.forName(classname);
                }
            }
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }
}
