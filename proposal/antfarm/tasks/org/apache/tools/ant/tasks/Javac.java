/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant.tasks;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;

public class Javac extends Task {
    private Fileset[] fileset;
    private String dest;
    private String classpath;
    private String compilerclass = null;

    public void execute() throws BuildException {
        if (compilerclass == null) {
            compilerclass = "com.sun.tools.javac.Main";
        }

        List argList = new ArrayList();
        argList.add("-d");
        argList.add(dest);

        if (classpath != null) {
            argList.add("-classpath");

            // Replace the project's path separator with the system's path separator
            argList.add(classpath.replace(getProject().getPathSeparator(), File.pathSeparatorChar));
        }

        for (int i = 0; i < fileset.length; i++) {
            fileset[i].getFiles(argList);
        }

        String[] args = (String[]) argList.toArray(new String[argList.size()]);

        try {
            new File(dest).mkdirs();

            Class type = getClass().getClassLoader().loadClass(compilerclass);
            Method method = type.getMethod("main", new Class[] { args.getClass() });

            getWorkspace().info("Running javac...");

            method.invoke(null, new Object[] { args });
        }
        catch(InvocationTargetException exc) {
            Throwable cause = exc.getTargetException();
            if (cause instanceof ExitException) {
                if (((ExitException)cause).getStatus() != 0) {
                    throw new BuildException("Compile failed");
                }
            }
            else {
                throw new AntException("Error running compiler", exc);
            }
        }
        catch(ClassNotFoundException exc) {
            throw new BuildException("Compiler class not found. Makes sure tools.jar is in your classpath");
        }
        catch(IllegalAccessException exc) {
            throw new AntException("Unable to access compiler class", exc);
        }
        catch(NoSuchMethodException exc) {
            throw new AntException("Unable to find main method on compiler class", exc);
        }
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public Fileset[] getFileset() {
        return fileset;
    }

    public void setFileset(Fileset[] fileset) {
        this.fileset = fileset;
    }
}