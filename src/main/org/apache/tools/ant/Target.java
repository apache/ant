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

package org.apache.tools.ant;

import java.util.Enumeration;
import java.util.Vector;
import java.util.StringTokenizer;

/**
 * Class to implement a target object with required parameters.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 */
public class Target implements TaskContainer {

    /** Name of this target. */
    private String name;
    /** The "if" condition to test on execution. */
    private String ifCondition = "";
    /** The "unless" condition to test on execution. */
    private String unlessCondition = "";
    /** List of targets this target is dependent on. */
    private Vector dependencies = new Vector(2);
    /** Children of this target (tasks and data types). */
    private Vector children = new Vector(5);
    /** Project this target belongs to. */
    private Project project;
    /** Description of this target, if any. */
    private String description = null;

    /** Sole constructor. */
    public Target() {
    }
    
    /** 
     * Sets the project this target belongs to. 
     * 
     * @param project The project this target belongs to. 
     *                Must not be <code>null</code>.
     */
    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * Returns the project this target belongs to.
     * 
     * @return The project this target belongs to, or <code>null</code> if 
     *         the project has not been set yet.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Sets the list of targets this target is dependent on.
     * The targets themselves are not resolved at this time.
     * 
     * @param depS A comma-separated list of targets this target
     *             depends on. Must not be <code>null</code>.
     */
    public void setDepends(String depS) {
        if (depS.length() > 0) {
            StringTokenizer tok =
                new StringTokenizer(depS, ",", true);
            while (tok.hasMoreTokens()) {
                String token = tok.nextToken().trim();

                // Make sure the dependency is not empty string
                if (token.equals("") || token.equals(",")) {
                    throw new BuildException("Syntax Error: Depend "
                        + "attribute for target \"" + getName() 
                        + "\" has an empty string for dependency.");
                }

                addDependency(token);
                
                // Make sure that depends attribute does not
                // end in a ,
                if (tok.hasMoreTokens()) {
                    token = tok.nextToken();
                    if (!tok.hasMoreTokens() || !token.equals(",")) {
                        throw new BuildException("Syntax Error: Depend " 
                            + "attribute for target \"" + getName() 
                            + "\" ends with a , character");
                    }
                }
            }
        }
    }

    /**
     * Sets the name of this target.
     * 
     * @param name The name of this target. Should not be <code>null</code>.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this target.
     * 
     * @return the name of this target, or <code>null</code> if the
     *         name has not been set yet.
     */
    public String getName() {
        return name;
    }

    /**
     * Adds a task to this target.
     * 
     * @param task The task to be added. Must not be <code>null</code>.
     */
    public void addTask(Task task) {
        children.addElement(task);
    }

    /**
     * Adds the wrapper for a data type element to this target.
     * 
     * @param r The wrapper for the data type element to be added. 
     *          Must not be <code>null</code>.
     */
    public void addDataType(RuntimeConfigurable r) {
        children.addElement(r);
    }

    /** 
     * Returns the current set of tasks to be executed by this target.
     * 
     * @return an array of the tasks currently within this target
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

    /**
     * Adds a dependency to this target.
     * 
     * @param dependency The name of a target this target is dependent on.
     *                   Must not be <code>null</code>.
     */
    public void addDependency(String dependency) {
        dependencies.addElement(dependency);
    }

    /**
     * Returns an enumeration of the dependencies of this target.
     * 
     * @return an enumeration of the dependencies of this target
     */
    public Enumeration getDependencies() {
        return dependencies.elements();
    }

    /**
     * Sets the "if" condition to test on execution. This is the
     * name of a property to test for existence - if the property
     * is not set, the task will not execute. The property goes
     * through property substitution once before testing, so if
     * property <code>foo</code> has value <code>bar</code>, setting
     * the "if" condition to <code>${foo}_x</code> will mean that the
     * task will only execute if property <code>bar_x</code> is set.
     * 
     * @param property The property condition to test on execution.
     *                 May be <code>null</code>, in which case
     *                 no "if" test is performed.
     */
    public void setIf(String property) {
        this.ifCondition = (property == null) ? "" : property;
    }
 
    /**
     * Sets the "unless" condition to test on execution. This is the
     * name of a property to test for existence - if the property
     * is set, the task will not execute. The property goes
     * through property substitution once before testing, so if
     * property <code>foo</code> has value <code>bar</code>, setting
     * the "unless" condition to <code>${foo}_x</code> will mean that the
     * task will only execute if property <code>bar_x</code> isn't set.
     * 
     * @param property The property condition to test on execution.
     *                 May be <code>null</code>, in which case
     *                 no "unless" test is performed.
     */
    public void setUnless(String property) {
        this.unlessCondition = (property == null) ? "" : property;
    }

    /**
     * Sets the description of this target.
     * 
     * @param description The description for this target. 
     *                    May be <code>null</code>, indicating that no 
     *                    description is available.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the description of this target.
     * 
     * @return the description of this target, or <code>null</code> if no
     *         description is available.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the name of this target.
     * 
     * @return the name of this target, or <code>null</code> if the
     *         name has not been set yet.
     */
    public String toString() {
        return name;
    }

    /**
     * Executes the target if the "if" and "unless" conditions are
     * satisfied. Dependency checking should be done before calling this
     * method, as it does no checking of its own. If either the "if"
     * or "unless" test prevents this target from being executed, a verbose
     * message is logged giving the reason. It is recommended that clients
     * of this class call performTasks rather than this method so that 
     * appropriate build events are fired.
     * 
     * @exception BuildException if any of the tasks fail or if a data type
     *                           configuration fails.
     * 
     * @see #performTasks()
     * @see #setIf(String)
     * @see #setUnless(String)
     */
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
            project.log(this, "Skipped because property '" 
                        + project.replaceProperties(this.ifCondition) 
                        + "' not set.", Project.MSG_VERBOSE);
        } else {
            project.log(this, "Skipped because property '" 
                        + project.replaceProperties(this.unlessCondition) 
                        + "' set.", Project.MSG_VERBOSE);
        }
    }

    /**
     * Performs the tasks within this target (if the conditions are met),
     * firing target started/target finished messages around a call to 
     * execute.
     * 
     * @see #execute()
     */
    public final void performTasks() {
        try {
            project.fireTargetStarted(this);
            execute();
            project.fireTargetFinished(this, null);
        } catch (RuntimeException exc) {
            project.fireTargetFinished(this, exc);
            throw exc;
        }
    }
    
    /**
     * Replaces all occurrences of the given task in the list
     * of children with the replacement data type wrapper.
     * 
     * @param el The task to replace.
     *           Must not be <code>null</code>.
     * @param o  The data type wrapper to replace <code>el</code> with.
     */
    void replaceChild(Task el, RuntimeConfigurable o) {
        int index;
        while ((index = children.indexOf(el)) >= 0) {
            children.setElementAt(o, index);
        }
    }

    /**
     * Replaces all occurrences of the given task in the list
     * of children with the replacement task.
     * 
     * @param el The task to replace.
     *           Must not be <code>null</code>.
     * @param o  The task to replace <code>el</code> with.
     */
    void replaceChild(Task el, Task o) {
        int index;
        while ((index = children.indexOf(el)) >= 0) {
            children.setElementAt(o, index);
        }
    }

    /**
     * Tests whether or not the "if" condition is satisfied.
     * 
     * @return whether or not the "if" condition is satisfied. If no
     *         condition (or an empty condition) has been set,
     *         <code>true</code> is returned.
     *
     * @see #setIf(String)
     */
    private boolean testIfCondition() {
        if ("".equals(ifCondition)) {
            return true;
        }
        
        String test = project.replaceProperties(ifCondition);
        return project.getProperty(test) != null;
    }

    /**
     * Tests whether or not the "unless" condition is satisfied.
     * 
     * @return whether or not the "unless" condition is satisfied. If no
     *         condition (or an empty condition) has been set,
     *         <code>true</code> is returned.
     *
     * @see #setUnless(String)
     */
    private boolean testUnlessCondition() {
        if ("".equals(unlessCondition)) {
            return true;
        }
        String test = project.replaceProperties(unlessCondition);
        return project.getProperty(test) == null;
    }
}
