/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant.tasks;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.apache.tools.ant.*;

public class Jar extends Task {
    private String jarfile;
    private String basedir;
    private String manifest;

    public String getJarfile() {
        return jarfile;
    }

    public void setJarfile(String jarfile) {
        this.jarfile = jarfile;
    }

    public String getBasedir() {
        return basedir;
    }

    public void setBasedir(String basedir) {
        this.basedir = basedir;
    }

    public String getManifest() {
        return manifest;
    }

    public void setManifest(String manifest) {
        this.manifest = manifest;
    }

    public void execute() throws BuildException {
        File dir = new File(jarfile).getParentFile();
        if (dir != null) {
            dir.mkdirs();
        }
        List argList = new ArrayList();
        if (manifest == null) {
            argList.add("-cf");
        }
        else {
            argList.add("-cmf");
            argList.add(manifest);
        }
        argList.add(jarfile);
        argList.add("-C");
        argList.add(basedir);
        argList.add(".");

        String[] args = (String[]) argList.toArray(new String[argList.size()]);

        try {
            Class type = getClass().getClassLoader().loadClass("sun.tools.jar.Main");
            Method method = type.getMethod("main", new Class[] { args.getClass() });

            getWorkspace().info("Running jar...");

            method.invoke(null, new Object[] { args });
        }
        catch(InvocationTargetException exc) {
            Throwable cause = exc.getTargetException();
            if (cause instanceof ExitException) {
                if (((ExitException)cause).getStatus() != 0) {
                    throw new BuildException("Build failed");
                }
            }
            else {
                throw new AntException("Error running jar", exc);
            }
        }
        catch(ClassNotFoundException exc) {
            throw new AntException("Jar class not found. Makes sure tools.jar is in your classpath");
        }
        catch(Exception exc) {
            throw new AntException("Error running jar", exc);
        }
    }
}