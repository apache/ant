/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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

import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.service.ExecService;
import org.apache.ant.common.util.AntException;

/**
 * Ant1 Task facade
 *
 * @author Conor MacNeill
 * @created 31 January 2002
 */
public abstract class Task extends ProjectComponent
     implements org.apache.ant.common.antlib.Task {
    /** the name of this task */
    protected String taskName;
    /** The target with which this target is associated */
    protected Target target = null;
    /** The type of this target */
    protected String taskType = null;
    /** The description of this task */
    protected String description = null;
    /** The location within the build file of this project component */
    protected Location location;

    /**
     * Called by the project to let the task do its work. This method may be
     * called more than once, if the task is invoked more than once.
     * For example,
     * if target1 and target2 both depend on target3, then running
     * "ant target1 target2" will run all tasks in target3 twice.
     *
     * @exception BuildException if something goes wrong with the build
     */
    public void execute() throws BuildException {}

    /**
     * Set the name to use in logging messages.
     *
     * @param name the name to use in logging messages.
     */
    public void setTaskName(String name) {
        this.taskName = name;
    }


    /**
     * Sets the target object of this task.
     *
     * @param target Target in whose scope this task belongs.
     */
    public void setOwningTarget(Target target) {
        this.target = target;
    }


    /**
     * Has this task been marked invalid?
     *
     * @since Ant 1.5
     */
    protected final boolean isInvalid() {
        return false;
    }

    public void maybeConfigure() throws BuildException {
        // XXX
        throw new BuildException("Not supported");
    }

    /**
     * Returns the wrapper used for runtime configuration.
     *
     * @return the wrapper used for runtime configuration. This
     *         method will generate a new wrapper (and cache it)
     *         if one isn't set already.
     */
    public RuntimeConfigurable getRuntimeConfigurableWrapper() {
        // XXX
        throw new BuildException("Not supported");
    }

    /**
     * Sets a description of the current action. It will be usefull in
     * commenting what we are doing.
     *
     * @param desc the new description value
     */
    public void setDescription(String desc) {
        description = desc;
    }

    /**
     * Get the name to use in logging messages.
     *
     * @return the name to use in logging messages.
     */
    public String getTaskName() {
        return taskName;
    }


    /**
     * Get the Target to which this task belongs
     *
     * @return the task's target.
     */
    public Target getOwningTarget() {
        return target;
    }

    /**
     * Gets the description of the Task
     *
     * @return the task's description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the location of the ProjectComponent's associated model element
     * in the build file
     *
     * @return the location of the associated model element
     */
    public Location getLocation() {
        return location;
    }


    /**
     * Add a nested task to this Ant1 task.
     *
     * @param task The task to be added
     * @exception AntException if the task cannot be added.
     */
    public void addNestedTask(org.apache.ant.common.antlib.Task task)
         throws AntException {

        if (!(this instanceof TaskContainer)) {
            throw new BuildException("Can't add tasks to this task");
        }
        // wrap the Ant2 task in a TaskAdapter
        TaskContainer container = (TaskContainer) this;
        if (task instanceof Task) {
            container.addTask((Task) task);
        } else {
            TaskAdapter adapter = new TaskAdapter();
            adapter.setProxy(task);
            adapter.setProject(getProject());
            adapter.init(task.getAntContext(), task.getComponentType());
            container.addTask(adapter);
        }
    }

    /**
     * Initialise this component
     *
     * @param context the core context for this component
     * @param componentType the component type of this component
     * @exception AntException if the component cannot be initialized
     */
    public void init(AntContext context, String componentType)
         throws AntException {
        super.init(context, componentType);

        taskType = componentType;
        taskName = componentType;

        org.apache.ant.common.util.Location contextLocation
             = context.getLocation();

        if (contextLocation
             == org.apache.ant.common.util.Location.UNKNOWN_LOCATION) {
            location = Location.UNKNOWN_LOCATION;
        } else {
            location = new Location(contextLocation.getSource(),
                contextLocation.getLineNumber(),
                contextLocation.getColumnNumber());
        }
        init();
    }


    /** Initialise this task */
    public void init() {
    }

    /** Validate this component */
    public void validateComponent() {
        // no default validation for Ant1 tasks
    }

    /** Execute this task sending the appropriate build events */
    public final void perform() {
        try {
            AntContext context = getAntContext();
            ExecService execService
                 = (ExecService) context.getCoreService(ExecService.class);
            execService.executeTask(this);
        } catch (AntException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Handle Output produced by the task. When a task prints to System.out
     * the container may catch this and redirect the content back to the
     * task by invoking this method. This method must NOT call System.out,
     * directly or indirectly.
     *
     * @param line The line of content produce by the task
     */
    public void handleSystemOut(String line) {
        handleOutput(line);
    }

    /**
     * Handle error information produced by the task. When a task prints to
     * System.err the container may catch this and redirect the content back
     * to the task by invoking this method. This method must NOT call
     * System.err, directly or indirectly.
     *
     * @param line The line of error info produce by the task
     */
    public void handleSystemErr(String line) {
        // default behaviout is to log at WARN level
        handleErrorOutput(line);
    }

    /**
     * Handle output captured for this task
     *
     * @param line the captured output
     */
    protected void handleOutput(String line) {
        log(line, Project.MSG_INFO);
    }

    /**
     * Handle error output captured for this task
     *
     * @param line the captured error output
     */
    protected void handleErrorOutput(String line) {
        log(line, Project.MSG_ERR);
    }

    /**
     * Set the name with which the task has been invoked.
     *
     * @param type the name the task has been invoked as.
     */
    void setTaskType(String type) {
        this.taskType = type;
    }

    /**
     * Sets the file location where this task was defined.
     *
     * @param location the new location value
     */
    public void setLocation(Location location) {
        this.location = location;
    }

}

