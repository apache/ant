/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant;

import java.io.*;
import java.util.*;

/**
 * Command line entry point into Ant. This class is entered via the
 * cannonical `public static void main` entry point and reads the
 * command line arguments. It then assembles and executes an Ant
 * project.
 * <p>
 * If you integrating Ant into some other tool, this is not the class
 * to use as an entry point. Please see the source code of this
 * class to see how it manipulates the Ant project classes.
 *
 * @author duncan@x180.com
 */

public class Main {

    /** Our current message output status. Follows Project.MSG_XXX */
    private int msgOutputLevel = Project.MSG_INFO;

    /** File that we are using for configuration */
    private File buildFile = new File("build.xml");

    /** Stream that we are using for logging */
    private PrintStream out = System.out;

    /** The build targets */
    private Vector targets = new Vector(5);

    /** Set of properties that can be used by tasks */
    private Properties definedProps = new Properties();

    /** Names of classes to add as listeners to project */
    private Vector listeners = new Vector(5);

    /**
     * Indicates if this ant should be run.
     */
    private boolean readyToRun = false;

    /**
     * Command line entry point. This method kicks off the building
     * of a project object and executes a build using either a given
     * target or the default target.
     *
     * @param args Command line args.
     */
    public static void main(String[] args) {
        try {
            new Main(args).runBuild();
            System.exit(0);
        }
        catch(Throwable exc) {
            System.exit(1);
        }
    }

    protected Main(String[] args) throws BuildException {

        // cycle through given args

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("-help") || arg.equals("help")) {
                printUsage();
                return;
            } else if (arg.equals("-version")) {
                printVersion();
                return;
            } else if (arg.equals("-quiet") || arg.equals("-q") || arg.equals("q")) {
                msgOutputLevel = Project.MSG_WARN;
            } else if (arg.equals("-verbose") || arg.equals("-v") || arg.equals("v")) {
                msgOutputLevel = Project.MSG_VERBOSE;
            } else if (arg.equals("-logfile") || arg.equals("-l") || arg.equals("l")) {
                try {
                    File logFile = new File(args[i+1]);
                    i++;
                    out = new PrintStream(new FileOutputStream(logFile));
                    System.setOut(out);
                    System.setErr(out);
                } catch (IOException ioe) {
                    String msg = "Cannot write on the specified log file. " +
                        "Make sure the path exists and you have write permissions.";
                    System.out.println(msg);
                    return;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a log file when " +
                        "using the -log argument";
                    System.out.println(msg);
                    return;
                }
            } else if (arg.equals("-buildfile") || arg.equals("-file") || arg.equals("-f") || arg.equals("f")) {
                try {
                    buildFile = new File(args[i+1]);
                    i++;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a buildfile when " +
                        "using the -buildfile argument";
                    System.out.println(msg);
                    return;
                }
            } else if (arg.equals("-listener")) {
                try {
                    listeners.addElement(args[i+1]);
                    i++;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a classname when " +
                        "using the -listener argument";
                    System.out.println(msg);
                    return;
                }
            } else if (arg.startsWith("-D")) {

                /* Interestingly enough, we get to here when a user
                 * uses -Dname=value. However, in some cases, the JDK
                 * goes ahead * and parses this out to args
                 *   {"-Dname", "value"}
                 * so instead of parsing on "=", we just make the "-D"
                 * characters go away and skip one argument forward.
                 *
                 * I don't know how to predict when the JDK is going
                 * to help or not, so we simply look for the equals sign.
                 */

                String name = arg.substring(2, arg.length());
                String value = null;
                int posEq = name.indexOf("=");
                if (posEq > 0) {
                    value = name.substring(posEq+1);
                    name = name.substring(0, posEq);
                } else if (i < args.length)
                    value = args[++i];

                definedProps.put(name, value);
            } else if (arg.startsWith("-")) {
                // we don't have any more args to recognize!
                String msg = "Unknown arg: " + arg;
                System.out.println(msg);
                printUsage();
                return;
            } else {
                // if it's no other arg, it may be the target
                targets.addElement(arg);
            }

        }

        // make sure buildfile exists

        if (!buildFile.exists()) {
            System.out.println("Buildfile: " + buildFile + " does not exist!");
            return;
        }

        // make sure it's not a directory (this falls into the ultra
        // paranoid lets check everything catagory

        if (buildFile.isDirectory()) {
            System.out.println("What? Buildfile: " + buildFile + " is a dir!");
            return;
        }

        readyToRun = true;
    }

    /**
     * Executes the build.
     */

    private void runBuild() throws BuildException {

        if (!readyToRun) {
            return;
        }

        // track when we started

        if (msgOutputLevel >= Project.MSG_INFO) {
            System.out.println("Buildfile: " + buildFile);
        }

        Project project = new Project();

        Throwable error = null;

        try {
            addBuildListeners(project);
            project.fireBuildStarted();
            
            project.init();

            // set user-define properties
            Enumeration e = definedProps.keys();
            while (e.hasMoreElements()) {
                String arg = (String)e.nextElement();
                String value = (String)definedProps.get(arg);
                project.setUserProperty(arg, value);
            }

            project.setUserProperty( "ant.file" , buildFile.getAbsolutePath() );

            // first use the ProjectHelper to create the project object
            // from the given build file.
            try {
                Class.forName("javax.xml.parsers.SAXParserFactory");
                ProjectHelper.configureProject(project, buildFile);
            } catch (NoClassDefFoundError ncdfe) {
                throw new BuildException("No JAXP compliant XML parser found. See http://java.sun.com/xml for the\nreference implementation.", ncdfe);
            } catch (ClassNotFoundException cnfe) {
                throw new BuildException("No JAXP compliant XML parser found. See http://java.sun.com/xml for the\nreference implementation.", cnfe);
            } catch (NullPointerException npe) {
                throw new BuildException("No JAXP compliant XML parser found. See http://java.sun.com/xml for the\nreference implementation.", npe);
            }

            // make sure that we have a target to execute
            if (targets.size() == 0) {
                targets.addElement(project.getDefaultTarget());
            }

            // actually do some work
            project.executeTargets(targets);
        }
        catch(RuntimeException exc) {
            error = exc;
            throw exc;
        }
        catch(Error err) {
            error = err;
            throw err;
        }
        finally {
            project.fireBuildFinished(error);
        }
    }

    protected void addBuildListeners(Project project) {

        // Add the default listener
        project.addBuildListener(createDefaultBuildListener());

        for (int i = 0; i < listeners.size(); i++) {
            String className = (String) listeners.elementAt(i);
            try {
                BuildListener listener =
                    (BuildListener) Class.forName(className).newInstance();
                project.addBuildListener(listener);
            }
            catch(Exception exc) {
                throw new BuildException("Unable to instantiate listener " + className, exc);
            }
        }
    }

    /**
     *  Creates the default build listener for displaying output to the screen.
     */
    private BuildListener createDefaultBuildListener() {
        return new DefaultLogger(out, msgOutputLevel);
    }

    /**
     * Prints the usage of how to use this class to System.out
     */
    private static void printUsage() {
        String lSep = System.getProperty("line.separator");
        StringBuffer msg = new StringBuffer();
        msg.append("ant [options] [target]" + lSep);
        msg.append("Options: " + lSep);
        msg.append("  -help                  print this message" + lSep);
        msg.append("  -version               print the version information and exit" + lSep);
        msg.append("  -quiet                 be extra quiet" + lSep);
        msg.append("  -verbose               be extra verbose" + lSep);
        msg.append("  -logfile <file>        use given file for log" + lSep);
        msg.append("  -listener <classname>  add an instance of class as a project listener" + lSep);
        msg.append("  -buildfile <file>      use given buildfile" + lSep);
        msg.append("  -D<property>=<value>   use value for given property" + lSep);
        System.out.println(msg.toString());
    }

    private static void printVersion() {
        try {
            Properties props = new Properties();
            InputStream in =
                Main.class.getResourceAsStream("/org/apache/tools/ant/version.txt");
            props.load(in);
            in.close();

            String lSep = System.getProperty("line.separator");
            StringBuffer msg = new StringBuffer();
            msg.append("Ant version ");
            msg.append(props.getProperty("VERSION"));
            msg.append(" compiled on ");
            msg.append(props.getProperty("DATE"));
            msg.append(lSep);
            System.out.println(msg.toString());
        } catch (IOException ioe) {
            System.err.println("Could not load the version information.");
            System.err.println(ioe.getMessage());
        } catch (NullPointerException npe) {
            System.err.println("Could not load the version information.");
        }
    }
}
