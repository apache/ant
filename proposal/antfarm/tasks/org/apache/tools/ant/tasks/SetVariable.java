/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant.tasks;

import org.apache.tools.ant.*;

public class SetVariable extends Task {
    private String name;
    private String value;

    public void execute() throws BuildException {
        getProject().setVariable(name, value);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}