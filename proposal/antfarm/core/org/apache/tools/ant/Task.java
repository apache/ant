/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

/**
 *  Abstract superclass for all task objects. Any class that
 *  extends this class can be plugged into a workspace by using the "load"
 *  task.
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public abstract class Task {
    private Workspace workspace;
    private Project project;
    private Target target;

    public abstract void execute() throws BuildException;

    public Workspace getWorkspace() {
        return workspace;
    }

    void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public Project getProject() {
        return project;
    }

    void setProject(Project project) {
        this.project = project;
    }

    public Target getTarget() {
        return target;
    }

    void setTarget(Target target) {
        this.target = target;
    }
}