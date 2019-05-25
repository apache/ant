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

package org.apache.tools.ant;

import java.io.IOException;
import java.util.Collections;

import org.apache.tools.ant.dispatch.DispatchUtils;

/**
 * Base class for all tasks.
 *
 * Use Project.createTask to create a new task instance rather than
 * using this class directly for construction.
 *
 * @see Project#createTask
 */
public abstract class Task extends ProjectComponent {
    // CheckStyle:VisibilityModifier OFF - bc
    /**
     * Target this task belongs to, if any.
     * @deprecated since 1.6.x.
     *             You should not be accessing this variable directly.
     *             Please use the {@link #getOwningTarget()} method.
     */
    @Deprecated
    protected Target target;

    /**
     * Name of this task to be used for logging purposes.
     * This defaults to the same as the type, but may be
     * overridden by the user. For instance, the name "java"
     * isn't terribly descriptive for a task used within
     * another task - the outer task code can probably
     * provide a better one.
     * @deprecated since 1.6.x.
     *             You should not be accessing this variable directly.
     *             Please use the {@link #getTaskName()} method.
     */
    @Deprecated
    protected String taskName;

    /**
     * Type of this task.
     *
     * @deprecated since 1.6.x.
     *             You should not be accessing this variable directly.
     *             Please use the {@link #getTaskType()} method.
     */
    @Deprecated
    protected String taskType;

    /**
     * Wrapper for this object, used to configure it at runtime.
     *
     * @deprecated since 1.6.x.
     *             You should not be accessing this variable directly.
     *             Please use the {@link #getWrapper()} method.
     */
    @Deprecated
    protected RuntimeConfigurable wrapper;

    // CheckStyle:VisibilityModifier ON

    /**
     * Whether or not this task is invalid. A task becomes invalid
     * if a conflicting class is specified as the implementation for
     * its type.
     */
    private boolean invalid;

    /**
     * Sets the target container of this task.
     *
     * @param target Target in whose scope this task belongs.
     *               May be <code>null</code>, indicating a top-level task.
     */
    public void setOwningTarget(Target target) {
        this.target = target;
    }

    /**
     * Returns the container target of this task.
     *
     * @return The target containing this task, or <code>null</code> if
     *         this task is a top-level task.
     */
    public Target getOwningTarget() {
        return target;
    }

    /**
     * Sets the name to use in logging messages.
     *
     * @param name The name to use in logging messages.
     *             Should not be <code>null</code>.
     */
    public void setTaskName(String name) {
        this.taskName = name;
    }

    /**
     * Returns the name to use in logging messages.
     *
     * @return the name to use in logging messages.
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * Sets the name with which the task has been invoked.
     *
     * @param type The name the task has been invoked as.
     *             Should not be <code>null</code>.
     */
    public void setTaskType(String type) {
        this.taskType = type;
    }

    /**
     * Called by the project to let the task initialize properly.
     * The default implementation is a no-op.
     *
     * @exception BuildException if something goes wrong with the build
     */
    public void init() throws BuildException {
    }

    /**
     * Called by the project to let the task do its work. This method may be
     * called more than once, if the task is invoked more than once.
     * For example,
     * if target1 and target2 both depend on target3, then running
     * "ant target1 target2" will run all tasks in target3 twice.
     *
     * @exception BuildException if something goes wrong with the build.
     */
    public void execute() throws BuildException {
    }

    /**
     * Returns the wrapper used for runtime configuration.
     *
     * @return the wrapper used for runtime configuration. This
     *         method will generate a new wrapper (and cache it)
     *         if one isn't set already.
     */
    public RuntimeConfigurable getRuntimeConfigurableWrapper() {
        if (wrapper == null) {
            wrapper = new RuntimeConfigurable(this, getTaskName());
        }
        return wrapper;
    }

    /**
     * Sets the wrapper to be used for runtime configuration.
     *
     * This method should be used only by the ProjectHelper and Ant internals.
     * It is public to allow helper plugins to operate on tasks, normal tasks
     * should never use it.
     *
     * @param wrapper The wrapper to be used for runtime configuration.
     *                May be <code>null</code>, in which case the next call
     *                to getRuntimeConfigurableWrapper will generate a new
     *                wrapper.
     */
    public void setRuntimeConfigurableWrapper(RuntimeConfigurable wrapper) {
        this.wrapper = wrapper;
    }

    // TODO: (Jon Skeet) The comment "if it hasn't been done already" may
    // not be strictly true. wrapper.maybeConfigure() won't configure the same
    // attributes/text more than once, but it may well add the children again,
    // unless I've missed something.
    /**
     * Configures this task - if it hasn't been done already.
     * If the task has been invalidated, it is replaced with an
     * UnknownElement task which uses the new definition in the project.
     *
     * @exception BuildException if the task cannot be configured.
     */
    public void maybeConfigure() throws BuildException {
        if (invalid) {
            getReplacement();
        } else if (wrapper != null) {
            wrapper.maybeConfigure(getProject());
        }
    }

    /**
     * Force the task to be reconfigured from its RuntimeConfigurable.
     */
    public void reconfigure() {
        if (wrapper != null) {
            wrapper.reconfigure(getProject());
        }
    }

    /**
     * Handles output by logging it with the INFO priority.
     *
     * @param output The output to log. Should not be <code>null</code>.
     */
    protected void handleOutput(String output) {
        log(output, Project.MSG_INFO);
    }

    /**
     * Handles output by logging it with the INFO priority.
     *
     * @param output The output to log. Should not be <code>null</code>.
     *
     * @since Ant 1.5.2
     */
    protected void handleFlush(String output) {
        handleOutput(output);
    }

    /**
     * Handle an input request by this task.
     *
     * @param buffer the buffer into which data is to be read.
     * @param offset the offset into the buffer at which data is stored.
     * @param length the amount of data to read.
     *
     * @return the number of bytes read.
     *
     * @exception IOException if the data cannot be read.
     * @since Ant 1.6
     */
    protected int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        return getProject().defaultInput(buffer, offset, length);
    }

    /**
     * Handles an error output by logging it with the WARN priority.
     *
     * @param output The error output to log. Should not be <code>null</code>.
     */
    protected void handleErrorOutput(String output) {
        log(output, Project.MSG_WARN);
    }

    /**
     * Handles an error line by logging it with the WARN priority.
     *
     * @param output The error output to log. Should not be <code>null</code>.
     *
     * @since Ant 1.5.2
     */
    protected void handleErrorFlush(String output) {
        handleErrorOutput(output);
    }

    /**
     * Logs a message with the default (INFO) priority.
     *
     * @param msg The message to be logged. Should not be <code>null</code>.
     */
    public void log(String msg) {
        log(msg, Project.MSG_INFO);
    }

    /**
     * Logs a message with the given priority. This delegates
     * the actual logging to the project.
     *
     * @param msg The message to be logged. Should not be <code>null</code>.
     * @param msgLevel The message priority at which this message is to
     *                 be logged.
     */
    public void log(String msg, int msgLevel) {
        if (getProject() == null) {
            super.log(msg, msgLevel);
        } else {
            getProject().log(this, msg, msgLevel);
        }
    }

    /**
     * Logs a message with the given priority. This delegates
     * the actual logging to the project.
     *
     * @param t The exception to be logged. Should not be <code>null</code>.
     * @param msgLevel The message priority at which this message is to
     *                 be logged.
     * @since 1.7
     */
    public void log(Throwable t, int msgLevel) {
        if (t != null) {
            log(t.getMessage(), t, msgLevel);
        }
    }

    /**
     * Logs a message with the given priority. This delegates
     * the actual logging to the project.
     *
     * @param msg The message to be logged. Should not be <code>null</code>.
     * @param t The exception to be logged. May be <code>null</code>.
     * @param msgLevel The message priority at which this message is to
     *                 be logged.
     * @since 1.7
     */
    public void log(String msg, Throwable t, int msgLevel) {
        if (getProject() == null) {
            super.log(msg, msgLevel);
        } else {
            getProject().log(this, msg, t, msgLevel);
        }
    }

    /**
     * Performs this task if it's still valid, or gets a replacement
     * version and performs that otherwise.
     *
     * Performing a task consists of firing a task started event,
     * configuring the task, executing it, and then firing task finished
     * event. If a runtime exception is thrown, the task finished event
     * is still fired, but with the exception as the cause.
     */
    public final void perform() {
        if (invalid) {
            UnknownElement ue = getReplacement();
            Task task = ue.getTask();
            task.perform();
        } else {
            getProject().fireTaskStarted(this);
            Throwable reason = null;
            try {
                maybeConfigure();
                DispatchUtils.execute(this);
            } catch (BuildException ex) {
                if (ex.getLocation() == Location.UNKNOWN_LOCATION) {
                    ex.setLocation(getLocation());
                }
                reason = ex;
                throw ex;
            } catch (Exception ex) {
                reason = ex;
                BuildException be = new BuildException(ex);
                be.setLocation(getLocation());
                throw be;
            } catch (Error ex) {
                reason = ex;
                throw ex;
            } finally {
                getProject().fireTaskFinished(this, reason);
            }
        }
    }

    /**
     * Marks this task as invalid. Any further use of this task
     * will go through a replacement with the updated definition.
     */
    final void markInvalid() {
        invalid = true;
    }

    /**
     * Has this task been marked invalid?
     *
     * @return true if this task is no longer valid. A new task should be
     * configured in this case.
     *
     * @since Ant 1.5
     */
    protected final boolean isInvalid() {
        return invalid;
    }

    /**
     * Replacement element used if this task is invalidated.
     */
    private UnknownElement replacement;

    /**
     * Creates an UnknownElement that can be used to replace this task.
     * Once this has been created once, it is cached and returned by
     * future calls.
     *
     * @return the UnknownElement instance for the new definition of this task.
     */
    private UnknownElement getReplacement() {
        if (replacement == null) {
            replacement = new UnknownElement(taskType);
            replacement.setProject(getProject());
            replacement.setTaskType(taskType);
            replacement.setTaskName(taskName);
            replacement.setLocation(getLocation());
            replacement.setOwningTarget(target);
            replacement.setRuntimeConfigurableWrapper(wrapper);
            wrapper.setProxy(replacement);
            replaceChildren(wrapper, replacement);
            target.replaceChild(this, replacement);
            replacement.maybeConfigure();
        }
        return replacement;
    }

    /**
     * Recursively adds an UnknownElement instance for each child
     * element of replacement.
     *
     * @since Ant 1.5.1
     */
    private void replaceChildren(RuntimeConfigurable wrapper,
                                 UnknownElement parentElement) {
        for (RuntimeConfigurable childWrapper : Collections.list(wrapper.getChildren())) {
            UnknownElement childElement = new UnknownElement(childWrapper.getElementTag());
            parentElement.addChild(childElement);
            childElement.setProject(getProject());
            childElement.setRuntimeConfigurableWrapper(childWrapper);
            childWrapper.setProxy(childElement);
            replaceChildren(childWrapper, childElement);
        }
    }

    /**
     * Return the type of task.
     *
     * @return the type of task.
     */
    public String getTaskType() {
        return taskType;
    }

    /**
     * Return the runtime configurable structure for this task.
     *
     * @return the runtime structure for this task.
     */
    protected RuntimeConfigurable getWrapper() {
        return wrapper;
    }

    /**
     * Bind a task to another; use this when configuring a newly created
     * task to do work on behalf of another.
     * Project, OwningTarget, TaskName, Location and Description are all copied
     *
     * Important: this method does not call {@link Task#init()}.
     * If you are creating a task to delegate work to, call {@link Task#init()}
     * to initialize it.
     *
     * @param owner owning target
     * @since Ant1.7
     */
    public final void bindToOwner(Task owner) {
        setProject(owner.getProject());
        setOwningTarget(owner.getOwningTarget());
        setTaskName(owner.getTaskName());
        setDescription(owner.getDescription());
        setLocation(owner.getLocation());
        setTaskType(owner.getTaskType());
    }
}
