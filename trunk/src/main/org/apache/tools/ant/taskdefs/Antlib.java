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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.apache.tools.ant.UnknownElement;


/**
 * Antlib task. It does not
 * occur in an ant build file. It is the root element
 * an antlib xml file.
 *
 * @since Ant 1.6
 */
public class Antlib extends Task implements TaskContainer {
    //
    // Static
    //

    /** The name of this task */
    public static final String TAG = "antlib";

    /**
     * Static method to read an ant lib definition from
     * a url.
     *
     * @param project   the current project
     * @param antlibUrl the url to read the definitions from
     * @param uri       the uri that the antlib is to be placed in
     * @return   the ant lib task
     */
    public static Antlib createAntlib(Project project, URL antlibUrl,
                                      String uri) {
        // Check if we can contact the URL
        try {
            antlibUrl.openConnection().connect();
        } catch (IOException ex) {
            throw new BuildException(
                "Unable to find " + antlibUrl, ex);
        }
        ComponentHelper helper =
            ComponentHelper.getComponentHelper(project);
        helper.enterAntLib(uri);
        try {
            // Should be safe to parse
            ProjectHelper2 parser = new ProjectHelper2();
            UnknownElement ue =
                parser.parseUnknownElement(project, antlibUrl);
            // Check name is "antlib"
            if (!(ue.getTag().equals(TAG))) {
                throw new BuildException(
                    "Unexpected tag " + ue.getTag() + " expecting "
                    + TAG, ue.getLocation());
            }
            Antlib antlib = new Antlib();
            antlib.setProject(project);
            antlib.setLocation(ue.getLocation());
            antlib.setTaskName("antlib");
            antlib.init();
            ue.configure(antlib);
            return antlib;
        } finally {
            helper.exitAntLib();
        }
    }


    //
    // Instance
    //
    private ClassLoader classLoader;
    private String      uri = "";
    private List  tasks = new ArrayList();

    /**
     * Set the class loader for this antlib.
     * This class loader is used for any tasks that
     * derive from Definer.
     *
     * @param classLoader the class loader
     */
    protected void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Set the URI for this antlib.
     * @param uri the namespace uri
     */
    protected void  setURI(String uri) {
        this.uri = uri;
    }

    private ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = Antlib.class.getClassLoader();
        }
        return classLoader;
    }

    /**
     * add a task to the list of tasks
     *
     * @param nestedTask Nested task to execute in antlib
     */
    public void addTask(Task nestedTask) {
        tasks.add(nestedTask);
    }

    /**
     * Execute the nested tasks, setting the classloader for
     * any tasks that derive from Definer.
     */
    public void execute() {
        for (Iterator i = tasks.iterator(); i.hasNext();) {
            UnknownElement ue = (UnknownElement) i.next();
            setLocation(ue.getLocation());
            ue.maybeConfigure();
            Object configuredObject = ue.getRealThing();
            if (configuredObject == null) {
                continue;
            }
            if (!(configuredObject instanceof AntlibDefinition)) {
                throw new BuildException(
                    "Invalid task in antlib " + ue.getTag()
                    + " " + configuredObject.getClass() + " does not "
                    + "extend org.apache.tools.ant.taskdefs.AntlibDefinition");
            }
            AntlibDefinition def = (AntlibDefinition) configuredObject;
            def.setURI(uri);
            def.setAntlibClassLoader(getClassLoader());
            def.init();
            def.execute();
        }
    }

}
