/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.HashMap;

import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.launch.AntMain;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.ProxySetup;


/**
 * Command line entry point into Ant. This class is entered via the
 * canonical `public static void main` entry point and reads the
 * command line arguments. It then assembles and executes an Ant
 * project.
 * <p>
 * If you integrating Ant into some other tool, this is not the class
 * to use as an entry point. Please see the source code of this
 * class to see how it manipulates the Ant project classes.
 *
 */
public class Main implements AntMain {

    /** The default build file name. {@value} */
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
    private Vector targets = new Vector();

    /** Set of properties that can be used by tasks. */
    private Properties definedProps = new Properties();

    /** Names of classes to add as listeners to project. */
    private Vector listeners = new Vector(1);

    /** File names of property files to load on startup. */
    private Vector propertyFiles = new Vector(1);

    /** Indicates whether this build is to support interactive input */
    private boolean allowInput = true;

    /** keep going mode */
    private boolean keepGoingMode = false;

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
     * optional thread priority
     */
    private Integer threadPriority = null;

    /**
     * proxy flag: default is false
     */
    private boolean proxy = false;

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
        Main m = new Main();
        m.startAnt(args, additionalUserProperties, coreLoader);
    }

    /**
     * Start Ant
     * @param args command line args
     * @param additionalUserProperties properties to set beyond those that
     *        may be specified on the args list
     * @param coreLoader - not used
     *
     * @since Ant 1.6
     */
    public void startAnt(String[] args, Properties additionalUserProperties,
                         ClassLoader coreLoader) {

        try {
            Diagnostics.validateVersion();
            processArgs(args);
        } catch (Throwable exc) {
            handleLogfile();
            printMessage(exc);
            exit(1);
            return;
        }

        if (additionalUserProperties != null) {
            for (Enumeration e = additionalUserProperties.keys();
                    e.hasMoreElements();) {
                String key = (String) e.nextElement();
                String property = additionalUserProperties.getProperty(key);
                definedProps.put(key, property);
            }
        }

        // expect the worst
        int exitCode = 1;
        try {
            try {
                runBuild(coreLoader);
                exitCode = 0;
            } catch (ExitStatusException ese) {
                exitCode = ese.getStatus();
                if (exitCode != 0) {
                    throw ese;
                }
            }
        } catch (BuildException be) {
            if (err != System.err) {
                printMessage(be);
            }
        } catch (Throwable exc) {
            exc.printStackTrace();
            printMessage(exc);
        } finally {
            handleLogfile();
        }
        exit(exitCode);
    }

    /**
     * This operation is expected to call {@link System#exit(int)}, which
     * is what the base version does.
     * However, it is possible to do something else.
     * @param exitCode code to exit with
     */
    protected void exit(int exitCode) {
        System.exit(exitCode);
    }

    /**
     * Close logfiles, if we have been writing to them.
     *
     * @since Ant 1.6
     */
    private static void handleLogfile() {
        if (isLogFileUsed) {
            FileUtils.close(out);
            FileUtils.close(err);
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

    /**
     * Constructor used when creating Main for later arg processing
     * and startup
     */
    public Main() {
    }

    /**
     * Sole constructor, which parses and deals with command line
     * arguments.
     *
     * @param args Command line arguments. Must not be <code>null</code>.
     *
     * @exception BuildException if the specified build file doesn't exist
     *                           or is a directory.
     *
     * @deprecated since 1.6.x
     */
    protected Main(String[] args) throws BuildException {
        processArgs(args);
    }

    /**
     * Process command line arguments.
     * When ant is started from Launcher, launcher-only arguments doe not get
     * passed through to this routine.
     *
     * @param args the command line arguments.
     *
     * @since Ant 1.6
     */
    private void processArgs(String[] args) {
        String searchForThis = null;
        PrintStream logTo = null;

        //this is the list of lu
        HashMap launchCommands = new HashMap();
        launchCommands.put("-lib", "");
        launchCommands.put("-cp", "");
        launchCommands.put("-noclasspath", "");
        launchCommands.put("--noclasspath", "");
        launchCommands.put("-nouserlib", "");
        launchCommands.put("--nouserlib", "");
        launchCommands.put("-main", "");
        // cycle through given args

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("-help") || arg.equals("-h")) {
                printUsage();
                return;
            } else if (arg.equals("-version")) {
                printVersion();
                return;
            } else if (arg.equals("-diagnostics")) {
                Diagnostics.doReport(System.out);
                return;
            } else if (arg.equals("-quiet") || arg.equals("-q")) {
                msgOutputLevel = Project.MSG_WARN;
            } else if (arg.equals("-verbose") || arg.equals("-v")) {
                printVersion();
                msgOutputLevel = Project.MSG_VERBOSE;
            } else if (arg.equals("-debug") || arg.equals("-d")) {
                printVersion();
                msgOutputLevel = Project.MSG_DEBUG;
            } else if (arg.equals("-noinput")) {
                allowInput = false;
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
                    throw new BuildException(msg);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a log file when "
                        + "using the -log argument";
                    throw new BuildException(msg);
                }
            } else if (arg.equals("-buildfile") || arg.equals("-file")
                       || arg.equals("-f")) {
                try {
                    buildFile = new File(args[i + 1].replace('/', File.separatorChar));
                    i++;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a buildfile when "
                        + "using the -buildfile argument";
                    throw new BuildException(msg);
                }
            } else if (arg.equals("-listener")) {
                try {
                    listeners.addElement(args[i + 1]);
                    i++;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a classname when "
                        + "using the -listener argument";
                    throw new BuildException(msg);
                }
            } else if (arg.startsWith("-D")) {

                /* Interestingly enough, we get to here when a user
                 * uses -Dname=value. However, in some cases, the OS
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
                } else {
                    throw new BuildException("Missing value for property "
                                             + name);
                }

                definedProps.put(name, value);
            } else if (arg.equals("-logger")) {
                if (loggerClassname != null) {
                    throw new BuildException("Only one logger class may "
                        + " be specified.");
                }
                try {
                    loggerClassname = args[++i];
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    throw new BuildException("You must specify a classname when"
                                             + " using the -logger argument");
                }
            } else if (arg.equals("-inputhandler")) {
                if (inputHandlerClassname != null) {
                    throw new BuildException("Only one input handler class may "
                                             + "be specified.");
                }
                try {
                    inputHandlerClassname = args[++i];
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    throw new BuildException("You must specify a classname when"
                                             + " using the -inputhandler"
                                             + " argument");
                }
            } else if (arg.equals("-emacs") || arg.equals("-e")) {
                emacsMode = true;
            } else if (arg.equals("-projecthelp") || arg.equals("-p")) {
                // set the flag to display the targets and quit
                projectHelp = true;
            } else if (arg.equals("-find") || arg.equals("-s")) {
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
                    String msg = "You must specify a property filename when "
                        + "using the -propertyfile argument";
                    throw new BuildException(msg);
                }
            } else if (arg.equals("-k") || arg.equals("-keep-going")) {
                keepGoingMode = true;
            } else if (arg.equals("-nice")) {
                try {
                    threadPriority = Integer.decode(args[i + 1]);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    throw new BuildException(
                            "You must supply a niceness value (1-10)"
                            + " after the -nice option");
                } catch (NumberFormatException e) {
                    throw new BuildException("Unrecognized niceness value: "
                                             + args[i + 1]);
                }
                i++;
                if (threadPriority.intValue() < Thread.MIN_PRIORITY
                    || threadPriority.intValue() > Thread.MAX_PRIORITY) {
                    throw new BuildException(
                            "Niceness value is out of the range 1-10");
                }
            } else if (launchCommands.get(arg) != null) {
                //catch script/ant mismatch with a meaningful message
                //we could ignore it, but there are likely to be other
                //version problems, so we stamp down on the configuration now
                String msg = "Ant's Main method is being handed "
                        + "an option " + arg + " that is only for the launcher class."
                        + "\nThis can be caused by a version mismatch between "
                        + "the ant script/.bat file and Ant itself.";
                throw new BuildException(msg);
            } else if (arg.equals("-autoproxy")) {
                proxy = false;
            } else if (arg.startsWith("-")) {
                // we don't have any more args to recognize!
                String msg = "Unknown argument: " + arg;
                System.err.println(msg);
                printUsage();
                throw new BuildException("");
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
        // paranoid lets check everything category

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
                FileUtils.close(fis);
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
            out = logTo;
            err = logTo;
            System.setOut(out);
            System.setErr(err);
        }
        readyToRun = true;
    }

    /**
     * Helper to get the parent file for a given file.
     * <p>
     * Added to simulate File.getParentFile() from JDK 1.2.
     * @deprecated since 1.6.x
     *
     * @param file   File to find parent of. Must not be <code>null</code>.
     * @return       Parent file or null if none
     */
    private File getParentFile(File file) {
        File parent = file.getParentFile();

        if (parent != null && msgOutputLevel >= Project.MSG_VERBOSE) {
            System.out.println("Searching in " + parent.getAbsolutePath());
        }

        return parent;
    }

    /**
     * Search parent directories for the build file.
     * <p>
     * Takes the given target as a suffix to append to each
     * parent directory in search of a build file.  Once the
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

            PrintStream savedErr = System.err;
            PrintStream savedOut = System.out;
            InputStream savedIn = System.in;

            // use a system manager that prevents from System.exit()
            SecurityManager oldsm = null;
            oldsm = System.getSecurityManager();

                //SecurityManager can not be installed here for backwards
                //compatibility reasons (PD). Needs to be loaded prior to
                //ant class if we are going to implement it.
                //System.setSecurityManager(new NoExitSecurityManager());
            try {
                if (allowInput) {
                    project.setDefaultInputStream(System.in);
                }
                System.setIn(new DemuxInputStream(project));
                System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
                System.setErr(new PrintStream(new DemuxOutputStream(project, true)));


                if (!projectHelp) {
                    project.fireBuildStarted();
                }

                // set the thread priorities
                if (threadPriority != null) {
                    try {
                        project.log("Setting Ant's thread priority to "
                                + threadPriority, Project.MSG_VERBOSE);
                        Thread.currentThread().setPriority(threadPriority.intValue());
                    } catch (SecurityException swallowed) {
                        //we cannot set the priority here.
                        project.log("A security manager refused to set the -nice value");
                    }
                }



                project.init();

                // set user-define properties
                Enumeration e = definedProps.keys();
                while (e.hasMoreElements()) {
                    String arg = (String) e.nextElement();
                    String value = (String) definedProps.get(arg);
                    project.setUserProperty(arg, value);
                }

                project.setUserProperty(MagicNames.ANT_FILE,
                                        buildFile.getAbsolutePath());

                project.setKeepGoingMode(keepGoingMode);
                if (proxy) {
                    //proxy setup if enabled
                    ProxySetup proxySetup = new ProxySetup(project);
                    proxySetup.enableProxies();
                }

                ProjectHelper.configureProject(project, buildFile);

                if (projectHelp) {
                    printDescription(project);
                    printTargets(project, msgOutputLevel > Project.MSG_INFO);
                    return;
                }

                // make sure that we have a target to execute
                if (targets.size() == 0) {
                    if (project.getDefaultTarget() != null) {
                        targets.addElement(project.getDefaultTarget());
                    }
                }

                project.executeTargets(targets);
            } finally {
                // put back the original security manager
                //The following will never eval to true. (PD)
                if (oldsm != null) {
                    System.setSecurityManager(oldsm);
                }

                System.setOut(savedOut);
                System.setErr(savedErr);
                System.setIn(savedIn);
            }
        } catch (RuntimeException exc) {
            error = exc;
            throw exc;
        } catch (Error e) {
            error = e;
            throw e;
        } finally {
            if (!projectHelp) {
                project.fireBuildFinished(error);
            } else if (error != null) {
                project.log(error.toString(), Project.MSG_ERR);
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
            BuildListener listener =
                    (BuildListener) ClasspathUtils.newInstance(className,
                            Main.class.getClassLoader(), BuildListener.class);
            if (project != null) {
                project.setProjectReference(listener);
            }
            project.addBuildListener(listener);
        }
    }

    /**
     * Creates the InputHandler and adds it to the project.
     *
     * @param project the project instance.
     *
     * @exception BuildException if a specified InputHandler
     *                           implementation could not be loaded.
     */
    private void addInputHandler(Project project) throws BuildException {
        InputHandler handler = null;
        if (inputHandlerClassname == null) {
            handler = new DefaultInputHandler();
        } else {
            handler = (InputHandler) ClasspathUtils.newInstance(
                    inputHandlerClassname, Main.class.getClassLoader(),
                    InputHandler.class);
            if (project != null) {
                project.setProjectReference(handler);
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
                logger = (BuildLogger) ClasspathUtils.newInstance(
                        loggerClassname, Main.class.getClassLoader(),
                        BuildLogger.class);
            } catch (BuildException e) {
                System.err.println("The specified logger class "
                    + loggerClassname
                    + " could not be used because " + e.getMessage());
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
        msg.append("  -help, -h              print this message" + lSep);
        msg.append("  -projecthelp, -p       print project help information" + lSep);
        msg.append("  -version               print the version information and exit" + lSep);
        msg.append("  -diagnostics           print information that might be helpful to" + lSep);
        msg.append("                         diagnose or report problems." + lSep);
        msg.append("  -quiet, -q             be extra quiet" + lSep);
        msg.append("  -verbose, -v           be extra verbose" + lSep);
        msg.append("  -debug, -d             print debugging information" + lSep);
        msg.append("  -emacs, -e             produce logging information without adornments"
                   + lSep);
        msg.append("  -lib <path>            specifies a path to search for jars and classes"
                   + lSep);
        msg.append("  -logfile <file>        use given file for log" + lSep);
        msg.append("    -l     <file>                ''" + lSep);
        msg.append("  -logger <classname>    the class which is to perform logging" + lSep);
        msg.append("  -listener <classname>  add an instance of class as a project listener"
                   + lSep);
        msg.append("  -noinput               do not allow interactive input" + lSep);
        msg.append("  -buildfile <file>      use given buildfile" + lSep);
        msg.append("    -file    <file>              ''" + lSep);
        msg.append("    -f       <file>              ''" + lSep);
        msg.append("  -D<property>=<value>   use value for given property" + lSep);
        msg.append("  -keep-going, -k        execute all targets that do not depend" + lSep);
        msg.append("                         on failed target(s)" + lSep);
        msg.append("  -propertyfile <name>   load all properties from file with -D" + lSep);
        msg.append("                         properties taking precedence" + lSep);
        msg.append("  -inputhandler <class>  the class which will handle input requests" + lSep);
        msg.append("  -find <file>           (s)earch for buildfile towards the root of" + lSep);
        msg.append("    -s  <file>           the filesystem and use it" + lSep);
        msg.append("  -nice  number          A niceness value for the main thread:" + lSep
                   + "                         1 (lowest) to 10 (highest); 5 is the default"
                   + lSep);
        msg.append("  -nouserlib             Run ant without using the jar files from" + lSep
                   + "                         ${user.home}/.ant/lib" + lSep);
        msg.append("  -noclasspath           Run ant without using CLASSPATH" + lSep);
        msg.append("  -autoproxy             Java1.5+: use the OS proxy settings"
                + lSep);
        msg.append("  -main <class>          override Ant's normal entry point");
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
            if (targetName.equals("")) {
                continue;
            }
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
        if (topNames.size() == 0) {
            printSubTargets = true;
        }
        if (printSubTargets) {
            printTargets(project, subNames, null, "Other targets:", 0);
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
     *
     * @param project the project instance.
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
    private static void printTargets(Project project, Vector names,
                                     Vector descriptions, String heading,
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
                msg.append(
                    spaces.substring(0, maxlen - ((String) names.elementAt(i)).length() + 2));
                msg.append(descriptions.elementAt(i));
            }
            msg.append(lSep);
        }
        project.log(msg.toString(), Project.MSG_WARN);
    }
}
