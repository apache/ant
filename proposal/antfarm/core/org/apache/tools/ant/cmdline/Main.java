/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant.cmdline;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.tools.ant.*;

/**
 *  Runs the command line version of ant. Takes a list of
 *  fully qualified targets and builds them.
 *  Any jars in the "tasks" directory will be automatically added
 *  to the project path.
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class Main {

    /**
     *  Builds the specified targets.
     */
    public static void main(String[] args) {
        File installDir = findInstallDir();
        setupProjectPath(installDir);
        Importer importer = loadImporter(installDir);

        Workspace workspace = new Workspace(importer);
        workspace.addBuildListener(new DefaultLogger(System.out));

        List targets = Arrays.asList(args);

        try {
            if (workspace.build(targets)) {
                System.exit(0);
            }
            else {
                System.exit(1);
            }
        }
        catch(Exception exc) {
            exc.printStackTrace();
            System.exit(2);
        }
    }

    /**
     *  Finds the ant.jar file in the classpath.
     */
    private static File findInstallDir() {
        StringTokenizer itr = new StringTokenizer(
            System.getProperty("java.class.path"),
            System.getProperty("path.separator"));

        while (itr.hasMoreTokens()) {
            File file = new File(itr.nextToken());
            if (file.getName().equals("ant.jar")) {
                // Found it
                File dir = file.getParentFile();
                if (dir == null) {
                    dir = new File(".");
                }
                return dir;
            }
        }

        System.err.println("Unable to locate ant.jar");
        System.exit(1);
        return null;
    }

    /**
     *  Locates the "tasks" directory relative to the ant.jar file.
     */
    private static void setupProjectPath(File installDir) {
        StringBuffer path = new StringBuffer(System.getProperty("ant.project.path", "."));

        File taskDir = new File(installDir, "tasks");
        if (taskDir.exists()) {
            File[] taskjars = taskDir.listFiles();
            for (int i = 0; i < taskjars.length; i++) {
                path.append(System.getProperty("path.separator"));
                path.append(taskjars[i].getPath());
            }
        }

        System.setProperty("ant.project.path", path.toString());
        System.out.println(path.toString());
    }

    /**
     *  Creates a class loader using the jars from the "xml" directory, and
     *  loads the XmlImporter class.
     */
    private static Importer loadImporter(File installDir) {
        File xmlDir = new File(installDir, "xml");
        if (xmlDir.exists()) {
            File[] xmlJars = xmlDir.listFiles();
            URL[] urls = new URL[xmlJars.length];
            for (int i = 0; i < xmlJars.length; i++) {
                try {
                    urls[i] = xmlJars[i].toURL();
                }
                catch(MalformedURLException exc) {
                    exc.printStackTrace();
                }
            }

            try {
                URLClassLoader loader = new URLClassLoader(urls);
                return (Importer) loader.loadClass("org.apache.tools.ant.xml.XmlImporter").newInstance();
            }
            catch(Exception exc) {
                exc.printStackTrace();
                System.exit(1);
            }
        }
        else {
            System.err.println("Unable to find xml directory");
            System.exit(1);
        }

        return null;
    }
}