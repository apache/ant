/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.util.*;

/**
 * This class implements a target object with required parameters.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 */

public class Target implements TaskContainer {

    private String name;
    private String ifCondition = "";
    private String unlessCondition = "";
    private Vector dependencies = new Vector(2);
    private Vector children = new Vector(5);
    private Project project;
    private String description = null;

    public void setProject(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public void setDepends(String depS) {
        if (depS.length() > 0) {
            StringTokenizer tok =
                new StringTokenizer(depS, ",", true);
            while (tok.hasMoreTokens()) {
                String token = tok.nextToken().trim();

                //Make sure the dependency is not empty string
                if (token.equals("") || token.equals(",")) {
                    throw new BuildException( "Syntax Error: Depend attribute " +
                                              "for target \"" + getName() + 
                                              "\" has an empty string for dependency." );
                }

                addDependency(token);
                
                //Make sure that depends attribute does not
                //end in a ,
                if (tok.hasMoreTokens()) {
                    token = tok.nextToken();
                    if (!tok.hasMoreTokens() || !token.equals(",")) {
                        throw new BuildException( "Syntax Error: Depend attribute " +
                                                  "for target \"" + getName() + 
                                                  "\" ends with a , character" );
                    }
                }
            }
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addTask(Task task) {
        children.addElement(task);
    }

    public void addDataType(RuntimeConfigurable r) {
        children.addElement(r);
    }

    /** 
     * Get the current set of tasks to be executed by this target.
     * 
     * @return The current set of tasks.
     */
    public Task[] getTasks() {
        Vector tasks = new Vector(children.size());
        Enumeration enum = children.elements();
        while (enum.hasMoreElements()) {
            Object o = enum.nextElement();
            if (o instanceof Task) {
                tasks.addElement(o);
            }
        }
        
        Task[] retval = new Task[tasks.size()];
        tasks.copyInto(retval);
        return retval;
    }

    public void addDependency(String dependency) {
        dependencies.addElement(dependency);
    }

    public Enumeration getDependencies() {
        return dependencies.elements();
    }

    public void setIf(String property) {
        this.ifCondition = (property == null) ? "" : property;
    }
 
    public void setUnless(String property) {
        this.unlessCondition = (property == null) ? "" : property;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String toString() {
        return name;
    }

    public void execute() throws BuildException {
        if (testIfCondition() && testUnlessCondition()) {
            Enumeration enum = children.elements();
            while (enum.hasMoreElements()) {
                Object o = enum.nextElement();
                if (o instanceof Task) {
                    Task task = (Task) o;
                    task.perform();
                } else {
                    RuntimeConfigurable r = (RuntimeConfigurable) o;
                    r.maybeConfigure(project);
                }
            }
        } else if (!testIfCondition()) {
            project.log(this, "Skipped because property '" + this.ifCondition + "' not set.", 
                        Project.MSG_VERBOSE);
        } else {
            project.log(this, "Skipped because property '" + this.unlessCondition + "' set.",
                        Project.MSG_VERBOSE);
        }
    }

    public final void performTasks() {
        try {
            project.fireTargetStarted(this);
            execute();
            project.fireTargetFinished(this, null);
        }
        catch(RuntimeException exc) {
            project.fireTargetFinished(this, exc);
            throw exc;
        }
    }
    
    void replaceTask(UnknownElement el, Task t) {
        int index = -1;
        while ((index = children.indexOf(el)) >= 0) {
            children.setElementAt(t, index);
        }
    }

    private boolean testIfCondition() {
        if ("".equals(ifCondition)) {
            return true;
        }
        
        String test = ProjectHelper.replaceProperties(getProject(), ifCondition, getProject().getProperties());
        return project.getProperty(test) != null;
    }

    private boolean testUnlessCondition() {
        if ("".equals(unlessCondition)) {
            return true;
        }
        String test = ProjectHelper.replaceProperties(getProject(), unlessCondition, getProject().getProperties());
        return project.getProperty(test) == null;
    }
}
