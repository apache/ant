/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

import java.net.*;

/**
 *  The only task that gets loaded by default. It can be used
 *  to dynamically load any other required tasks.
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class Load extends Task {
    private String name;
    private String classname;

    public void execute() throws BuildException {
        try {
            getWorkspace().debug("Loading " + name);
            ClassLoader loader = new URLClassLoader(
                new URL[] { getProject().getBase() },
                getWorkspace().getClass().getClassLoader());

            getWorkspace().registerTask(name, loader.loadClass(classname));
        }
        catch(ClassNotFoundException exc) {
            throw new BuildException("Class \"" + classname + "\" not found");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }
}