/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant.cmdline;

import java.io.*;
import org.apache.tools.ant.*;

/**
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class DefaultLogger implements BuildListener {
    private PrintStream out;

    public DefaultLogger(PrintStream out) {
        this.out = out;
    }

    public void buildStarted(BuildEvent event) {
    }

    public void buildFinished(BuildEvent event) {
        BuildException exc = event.getException();
        out.println();
        if (exc == null) {
            out.println("BUILD SUCCESSFUL");
        }
        else {
            out.println("BUILD FAILED");
            out.println();
            out.println(exc);
        }
    }

    public void importStarted(BuildEvent event) {
        out.println("Importing: " + event.getProject().getName());
    }

    public void importFinished(BuildEvent event) {
    }

    public void targetStarted(BuildEvent event) {
        out.println("\n[" + event.getProject().getName() + ":" + event.getTarget().getName() + "]");
    }

    public void targetFinished(BuildEvent event) {
    }

    public void taskStarted(BuildEvent event) {
    }

    public void taskFinished(BuildEvent event) {
    }

    public void messageLogged(BuildEvent event) {
        out.println("    " + event.getMessage());
    }
}