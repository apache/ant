/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;

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

    /** The default build file name. */
    public static final String DEFAULT_BUILD_FILENAME = "build.xml";

    /** Our current message output status. Follows Project.MSG_XXX. */
    private int msgOutputLevel = Project.MSG_INFO;

    /** File that we are using for configuration. */
    private File buildFile; /* null */

    /** Stream to use for logging. */
    private static PrintStream out = System.out;

    /** Stream that we are using for logging error messages. */
    private static PrintStream err = System.err;

    /** The build targets. */
    private Vector targets = new Vector(5);

    /** Set of properties that can be used by tasks. */
    private Properties definedProps = new Properties();

    /** Names of classes to add as listeners to project. */
    private Vector listeners = new Vector(5);

    /** File names of property files to load on startup. */
    private Vector propertyFiles = new Vector(5);

    /**
     * The Ant logger class. There may be only one logger. It will have
     * the right to use the 'out' PrintStream. The class must implements the
     * BuildLogger interface.
     */
    private String loggerClassname = null;

    /**
     * The Ant InputHandler class.  There may be only one input
     * handler.
     */
    private String inputHandlerClassname = null;

    /**
     * Whether or not output to the log is to be unadorned.
     */
    private boolean emacsMode = false;

    /**
     * Whether or not this instance has successfully been
     * constructed and is ready to run.
     */
    private boolean readyToRun = false;

    /**
     * Whether or not we should only parse and display the project help
     * information.
     */
    private boolean projectHelp = false;

    /**
     * Whether or not a logfile is being used. This is used to
     * check if the output streams must be closed.
     */
    private static boolean isLogFileUsed = false;

    /**
     * Prints the message of the Throwable if it (the message) is not
     * <code>null</code>.
     *
     * @param t Throwable to print the message of.
     *          Must not be <code>null</code>.
     */
    private static void printMessage(Throwable t) {
        String message = t.getMessage();
        if (message != null) {
            System.err.println(message);
        }
    }

    /**
     * Creates a new instance of this class using the
     * arguments specified, gives it any extra user properties which have been
     * specified, and then runs the build using the classloader provided.
     *
     * @param args Command line arguments. Must not be <code>null</code>.
     * @param additionalUserProperties Any extra properties to use in this
     *        build. May be <code>null</code>, which is the equivalent to
     *        passing in an empty set of properties.
     * @param coreLoader Classloader used for core classes. May be
     *        <code>null</code> in which case the system classloader is used.
     */
    public static void start(String[] args, Properties additionalUserProperties,
                             ClassLoader coreLoader) {
        Main m = null;

        try {
            Diagnostics.validateVersion();
            m = new Main(args);
        } catch (Throwable exc) {
            printMessage(exc);
            System.exit(1);
        }

        if (additionalUserProperties != null) {
            for (Enumeration e = additionalUserProperties.keys();
                    e.hasMoreElements();) {
                String key = (String) e.nextElement();
                String property = additionalUserProperties.getProperty(key);
                m.definedProps.put(key, property);
            }
        }

        try {
            m.runBuild(coreLoader);
            System.exit(0);
        } catch (BuildException be) {
            if (m.err != System.err) {
                printMessage(be);
            }
            System.exit(1);
        } catch (Throwable exc) {
            exc.printStackTrace();
            printMessage(exc);
            System.exit(1);
        } finally {
            if (isLogFileUsed) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (final Exception e) {
                        //ignore
                    }
                }
                if (err != null) {
                    try {
                        err.close();
                    } catch (final Exception e) {
                        //ignore
                    }
                }
            }
        }
    }

    /**
     * Command line entry point. This method kicks off the building
     * of a project object and executes a build using either a given
     * target or the default target.
     *
     * @param args Command line arguments. Must not be <code>null</code>.
     */
    public static void main(String[] args) {
        start(args, null, null);
    }

    // XXX: (Jon Skeet) Error handling appears to be inconsistent here.
    // Sometimes there's just a return statement, and sometimes a
    // BuildException is thrown. What's the rationale for when to do
    // what?
    /**
     * Sole constructor, which parses and deals with command line
     * arguments.
     *
     * @param args Command line arguments. Must not be <code>null</code>.
     *
     * @exception BuildException if the specified build file doesn't exist
     *                           or is a directory.
     */
    protected Main(String[] args) throws BuildException {
        String searchForThis = null;
        PrintStream logTo = null;

        // cycle through given args

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("-help")) {
                printUsage();
                return;
            } else if (arg.equals("-version")) {
                printVersion();
                return;
            } else if (arg.equals("-diagnostics")){
                Diagnostics.doReport(System.out);
                return;
            } else if (arg.equals("-quiet") || arg.equals("-q")) {
                msgOutputLevel = Project.MSG_WARN;
            } else if (arg.equals("-verbose") || arg.equals("-v")) {
                printVersion();
                msgOutputLevel = Project.MSG_VERBOSE;
            } else if (arg.equals("-debug")) {
                printVersion();
                msgOutputLevel = Project.MSG_DEBUG;
            } else if (arg.equals("-logfile") || arg.equals("-l")) {
                try {
                    File logFile = new File(args[i + 1]);
                    i++;
                    logTo = new PrintStream(new FileOutputStream(logFile));
                    isLogFileUsed = true;
                } catch (IOException ioe) {
                    String msg = "Cannot write on the specified log file. "
                        + "Make sure the path exists and you have write "
                        + "permissions.";
                    System.out.println(msg);
                    return;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a log file when " +
                        "using the -log argument";
                    System.out.println(msg);
                    return;
                }
            } else if (arg.equals("-buildfile") || arg.equals("-file")
                       || arg.equals("-f")) {
                try {
                    buildFile = new File(args[i + 1].replace('/', File.separatorChar));
                    i++;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a buildfile when " +
                        "using the -buildfile argument";
                    System.out.println(msg);
                    return;
                }
            } else if (arg.equals("-listener")) {
                try {
                    listeners.addElement(args[i + 1]);
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
                 * goes ahead and parses this out to args
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
                    value = name.substring(posEq + 1);
                    name = name.substring(0, posEq);
                } else if (i < args.length - 1) {
                    value = args[++i];
                       }

                definedProps.put(name, value);
            } else if (arg.equals("-logger")) {
                if (loggerClassname != null) {
                    System.out.println("Only one logger class may "
                        + " be specified.");
                    return;
                }
                try {
                    loggerClassname = args[++i];
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    System.out.println("You must specify a classname when " +
                                       "using the -logger argument");
                    return;
                }
            } else if (arg.equals("-inputhandler")) {
                if (inputHandlerClassname != null) {
                    System.out.println("Only one input handler class may " +
                                       "be specified.");
                    return;
                }
                try {
                    inputHandlerClassname = args[++i];
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    System.out.println("You must specify a classname when " +
                                       "using the -inputhandler argument");
                    return;
                }
            } else if (arg.equals("-emacs")) {
                emacsMode = true;
            } else if (arg.equals("-projecthelp")) {
                // set the flag to display the targets and quit
                projectHelp = true;
            } else if (arg.equals("-find")) {
                // eat up next arg if present, default to build.xml
                if (i < args.length - 1) {
                    searchForThis = args[++i];
                } else {
                    searchForThis = DEFAULT_BUILD_FILENAME;
                }
            } else if (arg.startsWith("-propertyfile")) {
                try {
                    propertyFiles.addElement(args[i + 1]);
                    i++;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a property filename when " +
                        "using the -propertyfile argument";
                    System.out.println(msg);
                    return;
                }
            } else if (arg.startsWith("-")) {
                // we don't have any more args to recognize!
                String msg = "Unknown argument: " + arg;
                System.out.println(msg);
                printUsage();
                return;
            } else {
                // if it's no other arg, it may be the target
                targets.addElement(arg);
            }
        }

        // if buildFile was not specified on the command line,
        if (buildFile == null) {
            // but -find then search for it
            if (searchForThis != null) {
                buildFile = findBuildFile(System.getProperty("user.dir"),
                                          searchForThis);
            } else {
                buildFile = new File(DEFAULT_BUILD_FILENAME);
            }
        }

        // make sure buildfile exists
        if (!buildFile.exists()) {
            System.out.println("Buildfile: " + buildFile + " does not exist!");
            throw new BuildException("Build failed");
        }

        // make sure it's not a directory (this falls into the ultra
        // paranoid lets check everything catagory

        if (buildFile.isDirectory()) {
            System.out.println("What? Buildfile: " + buildFile + " is a dir!");
            throw new BuildException("Build failed");
        }

        // Load the property files specified by -propertyfile
        for (int propertyFileIndex = 0;
             propertyFileIndex < propertyFiles.size();
             propertyFileIndex++) {
            String filename
                = (String) propertyFiles.elementAt(propertyFileIndex);
            Properties props = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(filename);
                props.load(fis);
            } catch (IOException e) {
                System.out.println("Could not load property file "
                   + filename + ": " + e.getMessage());
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e){
                    }
                }
            }

            // ensure that -D properties take precedence
            Enumeration propertyNames = props.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String name = (String) propertyNames.nextElement();
                if (definedProps.getProperty(name) == null) {
                    definedProps.put(name, props.getProperty(name));
                }
            }
        }

        if (msgOutputLevel >= Project.MSG_INFO) {
            System.out.println("Buildfile: " + buildFile);
        }

        if (logTo != null) {
            out = err = logTo;
            System.setOut(out);
            System.setErr(out);
        }
        readyToRun = true;
    }

    /**
     * Helper to get the parent file for a given file.
     * <p>
     * Added to simulate File.getParentFile() from JDK 1.2.
     *
     * @param file   File to find parent of. Must not be <code>null</code>.
     * @return       Parent file or null if none
     */
    private File getParentFile(File file) {
        String filename = file.getAbsolutePath();
        file = new File(filename);
        filename = file.getParent();

        if (filename != null && msgOutputLevel >= Project.MSG_VERBOSE) {
            System.out.println("Searching in " + filename);
        }

        return (filename == null) ? null : new File(filename);
    }

    /**
     * Search parent directories for the build file.
     * <p>
     * Takes the given target as a suffix to append to each
     * parent directory in seach of a build file.  Once the
     * root of the file-system has been reached an exception
     * is thrown.
     *
     * @param start  Leaf directory of search.
     *               Must not be <code>null</code>.
     * @param suffix  Suffix filename to look for in parents.
     *                Must not be <code>null</code>.
     *
     * @return A handle to the build file if one is found
     *
     * @exception BuildException if no build file is found
     */
    private File findBuildFile(String start, String suffix)
         throws BuildException {
        if (msgOutputLevel >= Project.MSG_INFO) {
            System.out.println("Searching for " + suffix + " ...");
        }

        File parent = new File(new File(start).getAbsolutePath());
        File file = new File(parent, suffix);

        // check if the target file exists in the current directory
        while (!file.exists()) {
            // change to parent directory
            parent = getParentFile(parent);

            // if parent is null, then we are at the root of the fs,
            // complain that we can't find the build file.
            if (parent == null) {
                throw new BuildException("Could not locate a build file!");
            }

            // refresh our file handle
            file = new File(parent, suffix);
        }

        return file;
    }

    /**
     * Executes the build. If the constructor for this instance failed
     * (e.g. returned after issuing a warning), this method returns
     * immediately.
     *
     * @param coreLoader The classloader to use to find core classes.
     *                   May be <code>null</code>, in which case the
     *                   system classloader is used.
     *
     * @exception BuildException if the build fails
     */
    private void runBuild(ClassLoader coreLoader) throws BuildException {

        if (!readyToRun) {
            return;
        }

        final Project project = new Project();
        project.setCoreLoader(coreLoader);

        Throwable error = null;

        try {
            addBuildListeners(project);
            addInputHandler(project);

            PrintStream err = System.err;
            PrintStream out = System.out;

            // use a system manager that prevents from System.exit()
            // only in JDK > 1.1
            SecurityManager oldsm = null;
            if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_0) &&
                !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)){
                oldsm = System.getSecurityManager();

                //SecurityManager can not be installed here for backwards
                //compatability reasons (PD). Needs to be loaded prior to
                //ant class if we are going to implement it.
                //System.setSecurityManager(new NoExitSecurityManager());
            }
            try {
                System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
                System.setErr(new PrintStream(new DemuxOutputStream(project, true)));

                if (!projectHelp) {
                    project.fireBuildStarted();
                }
                project.init();
                project.setUserProperty("ant.version", getAntVersion());

                // set user-define properties
                Enumeration e = definedProps.keys();
                while (e.hasMoreElements()) {
                    String arg = (String) e.nextElement();
                    String value = (String) definedProps.get(arg);
                    project.setUserProperty(arg, value);
                }

                project.setUserProperty("ant.file",
                                        buildFile.getAbsolutePath());

                ProjectHelper.configureProject(project, buildFile);

                if (projectHelp) {
                    printDescription(project);
                    printTargets(project, msgOutputLevel > Project.MSG_INFO);
                    return;
                }

                // make sure that we have a target to execute
                if (targets.size() == 0) {
                    targets.addElement(project.getDefaultTarget());
                }

                project.executeTargets(targets);
            } finally {
                // put back the original security manager
                //The following will never eval to true. (PD)
                if (oldsm != null){
                    System.setSecurityManager(oldsm);
                }

                System.setOut(out);
                System.setErr(err);
            }
        } catch (RuntimeException exc) {
            error = exc;
            throw exc;
        } catch (Error err) {
            error = err;
            throw err;
        } finally {
            if (!projectHelp) {
                project.fireBuildFinished(error);
            }
        }
    }

    /**
     * Adds the listeners specified in the command line arguments,
     * along with the default listener, to the specified project.
     *
     * @param project The project to add listeners to.
     *                Must not be <code>null</code>.
     */
    protected void addBuildListeners(Project project) {

        // Add the default listener
        project.addBuildListener(createLogger());

        for (int i = 0; i < listeners.size(); i++) {
            String className = (String) listeners.elementAt(i);
            try {
                BuildListener listener =
                    (BuildListener) Class.forName(className).newInstance();
                project.addBuildListener(listener);
            } catch (Throwable exc) {
                throw new BuildException("Unable to instantiate listener "
                    + className, exc);
            }
        }
    }

    /**
     * Creates the InputHandler and adds it to the project.
     *
     * @exception BuildException if a specified InputHandler
     *                           implementation could not be loaded.
     */
    private void addInputHandler(Project project) {
        InputHandler handler = null;
        if (inputHandlerClassname == null) {
            handler = new DefaultInputHandler();
        } else {
            try {
                handler = (InputHandler)
                    (Class.forName(inputHandlerClassname).newInstance());
            } catch (ClassCastException e) {
                String msg = "The specified input handler class "
                    + inputHandlerClassname
                    + " does not implement the InputHandler interface";
                throw new BuildException(msg);
            }
            catch (Exception e) {
                String msg = "Unable to instantiate specified input handler "
                    + "class " + inputHandlerClassname + " : "
                    + e.getClass().getName();
                throw new BuildException(msg);
            }
        }
        project.setInputHandler(handler);
    }

    // XXX: (Jon Skeet) Any reason for writing a message and then using a bare
    // RuntimeException rather than just using a BuildException here? Is it
    // in case the message could end up being written to no loggers (as the
    // loggers could have failed to be created due to this failure)?
    /**
     * Creates the default build logger for sending build events to the ant
     * log.
     *
     * @return the logger instance for this build.
     */
    private BuildLogger createLogger() {
        BuildLogger logger = null;
        if (loggerClassname != null) {
            try {
                logger = (BuildLogger) (Class.forName(loggerClassname).newInstance());
            } catch (ClassCastException e) {
                System.err.println("The specified logger class "
                    + loggerClassname
                    + " does not implement the BuildLogger interface");
                throw new RuntimeException();
            } catch (Exception e) {
                System.err.println("Unable to instantiate specified logger "
                    + "class " + loggerClassname + " : "
                    + e.getClass().getName());
                throw new RuntimeException();
            }
        } else {
            logger = new DefaultLogger();
        }

        logger.setMessageOutputLevel(msgOutputLevel);
        logger.setOutputPrintStream(out);
        logger.setErrorPrintStream(err);
        logger.setEmacsMode(emacsMode);

        return logger;
    }

    /**
     * Prints the usage information for this class to <code>System.out</code>.
     */
    private static void printUsage() {
        String lSep = System.getProperty("line.separator");
        StringBuffer msg = new StringBuffer();
        msg.append("ant [options] [target [target2 [target3] ...]]" + lSep);
        msg.append("Options: " + lSep);
        msg.append("  -help                  print this message" + lSep);
        msg.append("  -projecthelp           print project help information" + lSep);
        msg.append("  -version               print the version information and exit" + lSep);
        msg.append("  -diagnostics           print information that might be helpful to" + lSep);
        msg.append("                         diagnose or report problems." + lSep);
        msg.append("  -quiet, -q             be extra quiet" + lSep);
        msg.append("  -verbose, -v           be extra verbose" + lSep);
        msg.append("  -debug                 print debugging information" + lSep);
        msg.append("  -emacs                 produce logging information without adornments" + lSep);
        msg.append("  -logfile <file>        use given file for log" + lSep);
        msg.append("    -l     <file>                ''" + lSep);
        msg.append("  -logger <classname>    the class which is to perform logging" + lSep);
        msg.append("  -listener <classname>  add an instance of class as a project listener" + lSep);
        msg.append("  -buildfile <file>      use given buildfile" + lSep);
        msg.append("    -file    <file>              ''" + lSep);
        msg.append("    -f       <file>              ''" + lSep);
        msg.append("  -D<property>=<value>   use value for given property" + lSep);
        msg.append("  -propertyfile <name>   load all properties from file with -D" + lSep);
        msg.append("                         properties taking precedence" + lSep);
        msg.append("  -inputhandler <class>  the class which will handle input requests" + lSep);
        msg.append("  -find <file>           search for buildfile towards the root of the" + lSep);
        msg.append("                         filesystem and use it" + lSep);
        System.out.println(msg.toString());
    }

    /**
     * Prints the Ant version information to <code>System.out</code>.
     *
     * @exception BuildException if the version information is unavailable
     */
    private static void printVersion() throws BuildException {
        System.out.println(getAntVersion());
    }

    /**
     * Cache of the Ant version information when it has been loaded.
     */
    private static String antVersion = null;

    /**
     * Returns the Ant version information, if available. Once the information
     * has been loaded once, it's cached and returned from the cache on future
     * calls.
     *
     * @return the Ant version information as a String
     *         (always non-<code>null</code>)
     *
     * @exception BuildException if the version information is unavailable
     */
    public static synchronized String getAntVersion() throws BuildException {
        if (antVersion == null) {
            try {
                Properties props = new Properties();
                InputStream in =
                    Main.class.getResourceAsStream("/org/apache/tools/ant/version.txt");
                props.load(in);
                in.close();

                StringBuffer msg = new StringBuffer();
                msg.append("Apache Ant version ");
                msg.append(props.getProperty("VERSION"));
                msg.append(" compiled on ");
                msg.append(props.getProperty("DATE"));
                antVersion = msg.toString();
            } catch (IOException ioe) {
                throw new BuildException("Could not load the version information:"
                                         + ioe.getMessage());
            } catch (NullPointerException npe) {
                throw new BuildException("Could not load the version information.");
            }
        }
        return antVersion;
    }

     /**
      * Prints the description of a project (if there is one) to
      * <code>System.out</code>.
      *
      * @param project The project to display a description of.
      *                Must not be <code>null</code>.
      */
    private static void printDescription(Project project) {
       if (project.getDescription() != null) {
          project.log(project.getDescription());
       }
    }

    /**
     * Prints a list of all targets in the specified project to
     * <code>System.out</code>, optionally including subtargets.
     *
     * @param project The project to display a description of.
     *                Must not be <code>null</code>.
     * @param printSubTargets Whether or not subtarget names should also be
     *                        printed.
     */
    private static void printTargets(Project project, boolean printSubTargets) {
        // find the target with the longest name
        int maxLength = 0;
        Enumeration ptargets = project.getTargets().elements();
        String targetName;
        String targetDescription;
        Target currentTarget;
        // split the targets in top-level and sub-targets depending
        // on the presence of a description
        Vector topNames = new Vector();
        Vector topDescriptions = new Vector();
        Vector subNames = new Vector();

        while (ptargets.hasMoreElements()) {
            currentTarget = (Target) ptargets.nextElement();
            targetName = currentTarget.getName();
            targetDescription = currentTarget.getDescription();
            // maintain a sorted list of targets
            if (targetDescription == null) {
                int pos = findTargetPosition(subNames, targetName);
                subNames.insertElementAt(targetName, pos);
            } else {
                int pos = findTargetPosition(topNames, targetName);
                topNames.insertElementAt(targetName, pos);
                topDescriptions.insertElementAt(targetDescription, pos);
                if (targetName.length() > maxLength) {
                    maxLength = targetName.length();
                }
            }
        }

        printTargets(project, topNames, topDescriptions, "Main targets:",
                     maxLength);
        //if there were no main targets, we list all subtargets
        //as it means nothing has a description
        if(topNames.size()==0) {
            printSubTargets=true;
        }
        if (printSubTargets) {
            printTargets(project, subNames, null, "Subtargets:", 0);
        }

        String defaultTarget = project.getDefaultTarget();
        if (defaultTarget != null && !"".equals(defaultTarget)) {
            // shouldn't need to check but...
            project.log("Default target: " + defaultTarget);
        }
    }

    /**
     * Searches for the correct place to insert a name into a list so as
     * to keep the list sorted alphabetically.
     *
     * @param names The current list of names. Must not be <code>null</code>.
     * @param name  The name to find a place for.
     *              Must not be <code>null</code>.
     *
     * @return the correct place in the list for the given name
     */
    private static int findTargetPosition(Vector names, String name) {
        int res = names.size();
        for (int i = 0; i < names.size() && res == names.size(); i++) {
            if (name.compareTo((String) names.elementAt(i)) < 0) {
                res = i;
            }
        }
        return res;
    }

    /**
     * Writes a formatted list of target names to <code>System.out</code>
     * with an optional description.
     *
     * @param names The names to be printed.
     *              Must not be <code>null</code>.
     * @param descriptions The associated target descriptions.
     *                     May be <code>null</code>, in which case
     *                     no descriptions are displayed.
     *                     If non-<code>null</code>, this should have
     *                     as many elements as <code>names</code>.
     * @param heading The heading to display.
     *                Should not be <code>null</code>.
     * @param maxlen The maximum length of the names of the targets.
     *               If descriptions are given, they are padded to this
     *               position so they line up (so long as the names really
     *               <i>are</i> shorter than this).
     */
    private static void printTargets(Project project,Vector names,
                                     Vector descriptions,String heading,
                                     int maxlen) {
        // now, start printing the targets and their descriptions
        String lSep = System.getProperty("line.separator");
        // got a bit annoyed that I couldn't find a pad function
        String spaces = "    ";
        while (spaces.length() <= maxlen) {
            spaces += spaces;
        }
        StringBuffer msg = new StringBuffer();
        msg.append(heading + lSep + lSep);
        for (int i = 0; i < names.size(); i++) {
            msg.append(" ");
            msg.append(names.elementAt(i));
            if (descriptions != null) {
                msg.append(spaces.substring(0, maxlen - ((String) names.elementAt(i)).length() + 2));
                msg.append(descriptions.elementAt(i));
            }
            msg.append(lSep);
        }
        project.log(msg.toString());
    }
}
