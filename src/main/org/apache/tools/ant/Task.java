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

package org.apache.tools.ant;

/**
 * Base class for all tasks.
 *
 * <p>Use {@link Project#createTask Project.createTask} to create a new Task.
 */

public abstract class Task {

    protected Project project = null;
    protected Target target = null;
    protected String description=null;
    protected Location location = Location.UNKNOWN_LOCATION;
    protected String taskName = null;
    protected String taskType = null;
    protected RuntimeConfigurable wrapper;

    /**
     * Sets the project object of this task. This method is used by
     * project when a task is added to it so that the task has
     * access to the functions of the project. It should not be used
     * for any other purpose.
     *
     * @param project Project in whose scope this task belongs.
     */
    void setProject(Project project) {
        this.project = project;
    }

    /**
     * Get the Project to which this task belongs
     *
     * @return the task's project.
     */
    public Project getProject() {
        return project;
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
     * Get the Target to which this task belongs
     *
     * @return the task's target.
     */
    public Target getOwningTarget() {
        return target;
    }
    
    /**
     * Set the name to use in logging messages.
     *
     * @param name the name to use in logging messages.
     */
    public void setTaskName(String name) {
        this.taskName = name;
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
     * Set the name with which the task has been invoked.
     *
     * @param type the name the task has been invoked as.
     */
    void setTaskType(String type) {
        this.taskType = type;
    }

    /**
     * Log a message with the default (INFO) priority.
     *
     * @param the message to be logged.
     */
    public void log(String msg) {
        log(msg, Project.MSG_INFO);
    }

    /**
     * Log a mesage with the give priority.
     *
     * @param the message to be logged.
     * @param msgLevel the message priority at which this message is to be logged.
     */
    public void log(String msg, int msgLevel) {
        project.log(this, msg, msgLevel);
    }


    /** Sets a description of the current action. It will be usefull in commenting
     *  what we are doing.
     */
    public void setDescription( String desc ) {
        description=desc;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Called by the project to let the task initialize properly. 
     *
     * @throws BuildException if someting goes wrong with the build
     */
    public void init() throws BuildException {}

    /**
     * Called by the project to let the task do it's work. This method may be 
     * called more than once, if the task is invoked more than once. For example, 
     * if target1 and target2 both depend on target3, then running 
     * "ant target1 target2" will run all tasks in target3 twice.
     *
     * @throws BuildException if someting goes wrong with the build
     */
    public void execute() throws BuildException {}

    /**
     * Returns the file location where this task was defined.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the file location where this task was defined.
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Returns the wrapper class for runtime configuration.
     */
    public RuntimeConfigurable getRuntimeConfigurableWrapper() {
        if (wrapper == null) {
            wrapper = new RuntimeConfigurable(this);
        }
        return wrapper;
    }

    protected void setRuntimeConfigurableWrapper(RuntimeConfigurable wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * Configure this task - if it hasn't been done already.
     */
    public void maybeConfigure() throws BuildException {
        if (wrapper != null) {
            wrapper.maybeConfigure(project);
        }
    }
}

