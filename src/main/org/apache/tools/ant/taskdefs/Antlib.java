/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
 * Antlib task.
 *
 * @author Peter Reilly
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
            Task t = ue.getTask();
            if (t == null) {
                continue;
            }
            if (t instanceof AntlibInterface) {
                AntlibInterface d = (AntlibInterface) t;
                d.setURI(uri);
                d.setAntlibClassLoader(getClassLoader());
            }
            t.init();
            t.execute();
        }
    }

}
