/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

import java.util.EventObject;

/**
 *  This class encapsulates information about events that occur during
 *  a build.
 *
 *  @see BuildListener
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class BuildEvent extends EventObject {
    public static final int DEBUG = 1;
    public static final int INFO = 2;
    public static final int WARN = 3;
    public static final int ERROR = 4;

    private Workspace workspace;
    private Project project;
    private Target target;
    private Task task;
    private String message;
    private int priority;
    private BuildException exception;

    /**
     * Construct a BuildEvent for a workspace level event
     *
     * @param workspace the workspace that emitted the event.
     */
    public BuildEvent(Workspace workspace) {
        super(workspace);
        this.workspace = workspace;
        this.project = null;
        this.target = null;
        this.task = null;
    }

    /**
     * Construct a BuildEvent for a project level event
     *
     * @param project the project that emitted the event.
     */
    public BuildEvent(Project project) {
        super(project);
        this.workspace = project.getWorkspace();
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
        this.workspace = target.getProject().getWorkspace();
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
        this.workspace = task.getProject().getWorkspace();
        this.project = task.getProject();
        this.target = task.getTarget();
        this.task = task;
    }

    public void setMessage(String message, int priority) {
        this.message = message;
        this.priority = priority;
    }

    public void setException(BuildException exception) {
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
    public BuildException getException() {
        return exception;
    }
}