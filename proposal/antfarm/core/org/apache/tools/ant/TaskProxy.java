/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

/**
 *  This class stores the information needed to
 *  instantiate a task object. It basically consists of
 *  the task name and a TaskData object, which stores the
 *  values of the fields.
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class TaskProxy {
    private Target target;
    private String name;
    private TaskData data;
    private String location;

    public TaskProxy(Target target, String name) {
        this.target = target;
        this.name = name;
        this.data = new TaskData(this);
    }

    public Target getTarget() {
        return target;
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

    public TaskData getData() {
        return data;
    }

    /**
     *  Finds the class for this task name, and creates an
     *  instance of it using TaskData.createBean().
     */
    public Task createTask() throws BuildException {
        Class type = target.getProject().getWorkspace().getTaskClass(name);
        return (Task) data.createBean(type);
    }
}