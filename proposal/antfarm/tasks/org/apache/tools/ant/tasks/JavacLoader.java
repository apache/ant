/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant.tasks;

import java.io.*;
import java.net.*;
import org.apache.tools.ant.*;

public class JavacLoader extends Task {
    public void execute() throws BuildException {
        try {
            URL toolsJar = findToolsJar();
            ClassLoader loader = new URLClassLoader(
                new URL[] { getProject().getBase(), toolsJar },
                getWorkspace().getClass().getClassLoader());

            getWorkspace().registerTask("javac", loader.loadClass("org.apache.tools.ant.tasks.Javac"));
        }
        catch(MalformedURLException exc) {
            throw new AntException("Bad URL", exc);
        }
        catch(ClassNotFoundException exc) {
            throw new BuildException("Class not found");
        }
    }

    private URL findToolsJar() throws MalformedURLException {
        // I assume this won't work everywhere...
        return new File(new File(System.getProperty("java.home")), "../lib/tools.jar").toURL();
    }
}