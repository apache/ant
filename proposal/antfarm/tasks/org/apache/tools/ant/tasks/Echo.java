/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant.tasks;

import org.apache.tools.ant.*;

public class Echo extends Task {
    private String message;

    public void execute() throws BuildException {
        System.out.println(message);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}