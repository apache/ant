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

import java.net.URL;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Find a class or resource on the supplied classpath, or the
 * system classpath if none is supplied. The named property is set if
 * the item can be found. For example
 * <pre>
 * &lt;whichresource resource="/log4j.properties"
 *   property="log4j.url" &gt;
 * </pre>
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
     * Set the classpath to use by reference.
     *
     * @param r a reference to an existing classpath.
     * @since Ant 1.7.1
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
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
            throw new BuildException("One of classname or resource must be specified");
        }
        if (setcount > 1) {
            throw new BuildException("Only one of classname or resource can be specified");
        }
        if (property == null) {
            throw new BuildException("No property defined");
        }
    }

    /**
     * execute it
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        validate();
        if (classpath != null) {
            classpath = classpath.concatSystemClasspath("ignore");
            getProject().log("using user supplied classpath: " + classpath,
                             Project.MSG_DEBUG);
        } else {
            classpath = new Path(getProject());
            classpath = classpath.concatSystemClasspath("only");
            getProject().log("using system classpath: " + classpath,
                             Project.MSG_DEBUG);
        }
        try (AntClassLoader loader =
             AntClassLoader.newAntClassLoader(getProject().getCoreLoader(),
                                              getProject(),
                                              classpath, false)) {
            String loc = null;
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
            URL url = loader.getResource(resource);
            if (url != null) {
                //set the property
                loc = url.toExternalForm();
                getProject().setNewProperty(property, loc);
            }
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
