/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003-2004 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.AntClassLoader;

import java.net.URL;

/**
 * Find a class or resource on the supplied classpath, or the
 * system classpath if none is supplied. The named property is set if
 * the item can be found. For example
 * <pre>
 * &lt;whichresource resource="/log4j.properties"
 *   property="log4j.url" &gt;
 * </pre>
 * @author steve loughran while stuck in Enumclaw, WA, with a broken down car
 * @since Ant 1.6
 * @ant.attribute.group name="oneof" description="Exactly one of these two"
 */
public class WhichResource extends Task {
    /**
     * our classpath
     */
    private Path classpath;

    /**
     * class to look for
     */
    private String classname;

    /**
     * resource to look for
     */
    private String resource;

    /**
     * property to set
     */
    private String property;


    /**
     * Set the classpath to be used for this compilation.
     * @param cp the classpath to be used.
     */
    public void setClasspath(Path cp) {
        if (classpath == null) {
            classpath = cp;
        } else {
            classpath.append(cp);
        }
    }

    /**
     * Adds a path to the classpath.
     * @return a classpath to be configured.
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }


    /**
     * validate
     */
    private void validate() {
        int setcount = 0;
        if (classname != null) {
            setcount++;
        }
        if (resource != null) {
            setcount++;
        }


        if (setcount == 0) {
            throw new BuildException(
                    "One of classname or resource must be specified");
        }
        if (setcount > 1) {
            throw new BuildException(
                    "Only one of classname or resource can be specified");
        }
        if (property == null) {
            throw new BuildException("No property defined");
        }
    }

    /**
     * execute it
     * @throws BuildException
     */
    public void execute() throws BuildException {
        validate();
        if (classpath != null) {
            getProject().log("using user supplied classpath: " + classpath,
                    Project.MSG_DEBUG);
            classpath = classpath.concatSystemClasspath("ignore");
        } else {
            classpath = new Path(getProject());
            classpath = classpath.concatSystemClasspath("only");
            getProject().log("using system classpath: " + classpath, Project.MSG_DEBUG);
        }
        AntClassLoader loader;
        loader = new AntClassLoader(getProject().getCoreLoader(),
                    getProject(),
                    classpath, false);
        String location = null;
        if (classname != null) {
            //convert a class name into a resource
            resource = classname.replace('.', '/') + ".class";
        }

        if (resource == null) {
            throw new BuildException("One of class or resource is required");
        }

        if (resource.startsWith("/")) {
            resource = resource.substring(1);
        }

        log("Searching for " + resource, Project.MSG_VERBOSE);
        URL url;
        url = loader.getResource(resource);
        if (url != null) {
            //set the property
            location = url.toExternalForm();
            getProject().setNewProperty(property, location);
        }
    }

    /**
     * name the resource to look for
     * @param resource the name of the resource to look for.
     * @ant.attribute group="oneof"
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * name the class to look for
     * @param classname the name of the class to look for.
     * @ant.attribute group="oneof"
     */
    public void setClass(String classname) {
        this.classname = classname;
    }

    /**
     * the property to fill with the URL of the resource or class
     * @param property the property to be set.
     * @ant.attribute group="required"
     */
    public void setProperty(String property) {
        this.property = property;
    }

}
