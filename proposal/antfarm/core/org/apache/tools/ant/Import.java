/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

/**
 *  Represents an import statement from a project.
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class Import {
    private Project project;
    private String name;
    private String location;

    public Import(Project project, String name) {
        this.project = project;
        this.name = name;
        this.location = location;
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
}