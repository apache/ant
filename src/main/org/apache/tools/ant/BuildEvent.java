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

import java.util.EventObject;

public class BuildEvent extends EventObject {
    private Project project;
    private Target target;
    private Task task;
    private String message;
    private int priority = Project.MSG_VERBOSE;
    private Throwable exception;

    /**
     * Construct a BuildEvent for a project level event
     *
     * @param project the project that emitted the event.
     */
    public BuildEvent(Project project) {
        super(project);
        this.project = project;
        this.target = null;
        this.task = null;
    }
    
    /**
     * Construct a BuildEvent for a target level event
     *
     * @param target the target that emitted the event.
     */
    public BuildEvent(Target target) {
        super(target);
        this.project = target.getProject();
        this.target = target;
        this.task = null;
    }
    
    /**
     * Construct a BuildEvent for a task level event
     *
     * @param task the task that emitted the event.
     */
    public BuildEvent(Task task) {
        super(task);
        this.project = task.getProject();
        this.target = task.getOwningTarget();
        this.task = task;
    }

    public void setMessage(String message, int priority) {
        this.message = message;
        this.priority = priority;
    }
    
    public void setException(Throwable exception) {
        this.exception = exception;
    }

    /**
     *  Returns the project that fired this event.
     */
    public Project getProject() {
        return project;
    }

    /**
     *  Returns the target that fired this event.
     */
    public Target getTarget() {
        
        return target;
    }

    /**
     *  Returns the task that fired this event.
     */
    public Task getTask() {
        return task;
    }

    /**
     *  Returns the logging message. This field will only be set
     *  for "messageLogged" events.
     *
     *  @see BuildListener#messageLogged(BuildEvent)
     */
    public String getMessage() {
        return message;
    }

    /**
     *  Returns the priority of the logging message. This field will only
     *  be set for "messageLogged" events.
     *
     *  @see BuildListener#messageLogged(BuildEvent)
     */
    public int getPriority(){
        return priority;
    }

    /**
     *  Returns the exception that was thrown, if any. This field will only
     *  be set for "taskFinished", "targetFinished", and "buildFinished" events.
     *
     *  @see BuildListener#taskFinished(BuildEvent)
     *  @see BuildListener#targetFinished(BuildEvent)
     *  @see BuildListener#buildFinished(BuildEvent)
     */
    public Throwable getException() {
        return exception;
    }
}
