/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

import java.util.*;

/**
 *  Represents a set of actions to be executed, which may or may
 *  not depend on other sets of actions.
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class Target {
    private Project project;
    private String name;
    private String location;
    private List tasks;
    private List depends;

    /**
     *  Called by the Project class to create new targets.
     */
    Target(Project project, String name) {
        this.project = project;
        this.name = name;
        this.location = null;
        this.tasks = new ArrayList();
        this.depends = new ArrayList();
    }

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List getTasks() {
        return tasks;
    }

    public void addDepend(String depend) {
        // If no project was specified, use this target's project
        if (depend.indexOf(Workspace.SCOPE_SEPARATOR) == -1) {
            depend = getProject().getName() + Workspace.SCOPE_SEPARATOR + depend;
        }
        depends.add(depend);
    }

    public List getDepends() {
        return depends;
    }

    /**
     *  Creates a task proxy for this target. The proxy will
     *  be converted into an actual task object at build time.
     */
    public TaskProxy createTaskProxy(String name) {
        TaskProxy proxy = new TaskProxy(this, name);
        tasks.add(proxy);
        return proxy;
    }
}