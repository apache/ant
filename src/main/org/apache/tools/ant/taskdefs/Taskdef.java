/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

/**
 * Define a new task - name and class
 *
 * @author costin@dnt.ro
 */
public class Taskdef extends Task {
    private String name;
    private String value;
    private Path classpath;

    public void setClasspath(Path classpath) {
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
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

    public void execute() throws BuildException {
        if (name==null || value==null ) {
            String msg = "name or classname attributes of taskdef element "
                + "are undefined";
            throw new BuildException(msg);
        }
        try {
            ClassLoader loader = null;
            if (classpath != null) {
                AntClassLoader al = new AntClassLoader(project, classpath,
                                                       false);
                // need to load Task via system classloader or the new
                // task we want to define will never be a Task but always
                // be wrapped into a TaskAdapter.
                al.addSystemPackageRoot("org.apache.tools.ant");
                loader = al;
            } else {
                loader = this.getClass().getClassLoader();
            }

            Class taskClass = null;
            if (loader != null) {
                taskClass = loader.loadClass(value);
            } else {
                taskClass = Class.forName(value);
            }
            project.addTaskDefinition(name, taskClass);
        } catch (ClassNotFoundException cnfe) {
            String msg = "taskdef class " + value +
                " cannot be found";
            throw new BuildException(msg, cnfe, location);
        } catch (NoClassDefFoundError ncdfe) {
            String msg = "taskdef class " + value +
                " cannot be found";
            throw new BuildException(msg, ncdfe, location);
        }
    }
    
    public void setName( String name) {
        this.name = name;
    }

    public String getClassname() {
        return value;
    }

    public void setClassname(String v) {
        value = v;
    }
}
