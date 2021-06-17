/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.launch.AntMain;
import org.apache.tools.ant.listener.SilentLogger;
import org.apache.tools.ant.property.GetProperty;
import org.apache.tools.ant.property.ResolvePropertyMap;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.ProxySetup;
import org.apache.tools.ant.util.StreamUtils;

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

    /**
     * A Set of args that are handled by the launcher and should
     * not be seen by Main.
     */
    private static final Set<String> LAUNCH_COMMANDS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("-lib", "-cp", "-noclasspath",
                    "--noclasspath", "-nouserlib", "-main")));

    /** The default build file name. {@value} */
    public static final String DEFAULT_BUILD_FILENAME = "build.xml";

    /** Our current message output status. Follows Project.MSG_XXX. */
    private int msgOutputLevel = Project.MSG_INFO;

    /** File that we are using for configuration. */
    private File buildFile; /* null */

    /** Stream to use for logging. */
    private PrintStream out = System.out;

    /** Stream that we are using for logging error messages. */
    private PrintStream err = System.err;

    /** The build targets. */
    private final Vector<String> targets = new Vector<>();

    /** Set of properties that can be used by tasks. */
    private final Properties definedProps = new Properties();

    /** Names of classes to add as listeners to project. */
    private final Vector<String> listeners = new Vector<>(1);

    /** File names of property files to load on startup. */
    private final Vector<String> propertyFiles = new Vector<>(1);

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
     * Whether or not log output should be reduced to the minimum
     */
    private boolean silent = false;

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
    private boolean isLogFileUsed = false;

    /**
     * optional thread priority
     */
    private Integer threadPriority = null;

    /**
     * proxy flag: default is false
     */
    private boolean proxy = false;

    private final Map<Class<?>, List<String>> extraArguments = new HashMap<>();

    private static final GetProperty NOPROPERTIES = aName -> null;

    /**
     * Prints the message of the Throwable if it (the message) is not
     * <code>null</code>.
     *
     * @param t Throwable to print the message of.
     *          Must not be <code>null</code>.
     */
    private static void printMessage(final Throwable t) {
        final String message = t.getMessage();
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
    public static void start(final String[] args, final Properties additionalUserProperties,
                             final ClassLoader coreLoader) {
        final Main m = new Main();
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
    public void startAnt(final String[] args, final Properties additionalUserProperties,
                         final ClassLoader coreLoader) {

        try {
            processArgs(args);
        } catch (final Throwable exc) {
            handleLogfile();
            printMessage(exc);
            exit(1);
            return;
        }

        if (additionalUserProperties != null) {
            additionalUserProperties.stringPropertyNames()
                    .forEach(key -> definedProps.put(key, additionalUserProperties.getProperty(key)));
        }

        // expect the worst
        int exitCode = 1;
        try {
            try {
                runBuild(coreLoader);
                exitCode = 0;
            } catch (final ExitStatusException ese) {
                exitCode = ese.getStatus();
                if (exitCode != 0) {
                    throw ese;
                }
            }
        } catch (final BuildException be) {
            if (err != System.err) {
                printMessage(be);
            }
        } catch (final Throwable exc) {
            exc.printStackTrace(); //NOSONAR
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
    protected void exit(final int exitCode) {
        System.exit(exitCode);
    }

    /**
     * Close logfiles, if we have been writing to them.
     *
     * @since Ant 1.6
     */
    private void handleLogfile() {
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
    public static void main(final String[] args) {
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
    @Deprecated
    protected Main(final String[] args) throws BuildException {
        processArgs(args);
    }

    /**
     * Process command line arguments.
     * When ant is started from Launcher, launcher-only arguments do not get
     * passed through to this routine.
     *
     * @param args the command line arguments.
     *
     * @since Ant 1.6
     */
    private void processArgs(final String[] args) {
        String searchForThis = null;
        boolean searchForFile = false;
        PrintStream logTo = null;

        // cycle through given args

        boolean justPrintUsage = false;
        boolean justPrintVersion = false;
        boolean justPrintDiagnostics = false;

        final ArgumentProcessorRegistry processorRegistry = ArgumentProcessorRegistry.getInstance();

        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];

            if (arg.equals("-help") || arg.equals("-h")) {
                justPrintUsage = true;
            } else if (arg.equals("-version")) {
                justPrintVersion = true;
            } else if (arg.equals("-diagnostics")) {
                justPrintDiagnostics = true;
            } else if (arg.equals("-quiet") || arg.equals("-q")) {
                msgOutputLevel = Project.MSG_WARN;
            } else if (arg.equals("-verbose") || arg.equals("-v")) {
                msgOutputLevel = Project.MSG_VERBOSE;
            } else if (arg.equals("-debug") || arg.equals("-d")) {
                msgOutputLevel = Project.MSG_DEBUG;
            } else if (arg.equals("-silent") || arg.equals("-S")) {
                silent = true;
            } else if (arg.equals("-noinput")) {
                allowInput = false;
            } else if (arg.equals("-logfile") || arg.equals("-l")) {
                try {
                    final File logFile = new File(args[i + 1]);
                    i++;
                    // life-cycle of OutputStream is controlled by
                    // logTo which becomes "out" and is closed in
                    // handleLogfile
                    logTo = new PrintStream(Files.newOutputStream(logFile.toPath())); //NOSONAR
                    isLogFileUsed = true;
                } catch (final IOException ioe) {
                    final String msg = "Cannot write on the specified log file. "
                        + "Make sure the path exists and you have write "
                        + "permissions.";
                    throw new BuildException(msg);
                } catch (final ArrayIndexOutOfBoundsException aioobe) {
                    final String msg = "You must specify a log file when "
                        + "using the -log argument";
                    throw new BuildException(msg);
                }
            } else if (arg.equals("-buildfile") || arg.equals("-file")
                       || arg.equals("-f")) {
                i = handleArgBuildFile(args, i);
            } else if (arg.equals("-listener")) {
                i = handleArgListener(args, i);
            } else if (arg.startsWith("-D")) {
                i = handleArgDefine(args, i);
            } else if (arg.equals("-logger")) {
                i = handleArgLogger(args, i);
            } else if (arg.equals("-inputhandler")) {
                i = handleArgInputHandler(args, i);
            } else if (arg.equals("-emacs") || arg.equals("-e")) {
                emacsMode = true;
            } else if (arg.equals("-projecthelp") || arg.equals("-p")) {
                // set the flag to display the targets and quit
                projectHelp = true;
            } else if (arg.equals("-find") || arg.equals("-s")) {
                searchForFile = true;
                // eat up next arg if present, default to build.xml
                if (i < args.length - 1) {
                    searchForThis = args[++i];
                }
            } else if (arg.startsWith("-propertyfile")) {
                i = handleArgPropertyFile(args, i);
            } else if (arg.equals("-k") || arg.equals("-keep-going")) {
                keepGoingMode = true;
            } else if (arg.equals("-nice")) {
                i = handleArgNice(args, i);
            } else if (LAUNCH_COMMANDS.contains(arg)) {
                //catch script/ant mismatch with a meaningful message
                //we could ignore it, but there are likely to be other
                //version problems, so we stamp down on the configuration now
                final String msg = "Ant's Main method is being handed "
                        + "an option " + arg + " that is only for the launcher class."
                        + "\nThis can be caused by a version mismatch between "
                        + "the ant script/.bat file and Ant itself.";
                throw new BuildException(msg);
            } else if (arg.equals("-autoproxy")) {
                proxy = true;
            } else if (arg.startsWith("-")) {
                boolean processed = false;
                for (final ArgumentProcessor processor : processorRegistry.getProcessors()) {
                    final int newI = processor.readArguments(args, i);
                    if (newI != -1) {
                        List<String> extraArgs = extraArguments.computeIfAbsent(processor.getClass(), k -> new ArrayList<>());
                        extraArgs.addAll(Arrays.asList(args).subList(newI, args.length));
                        processed = true;
                        break;
                    }
                }
                if (!processed) {
                    // we don't have any more args to recognize!
                    final String msg = "Unknown argument: " + arg;
                    System.err.println(msg);
                    printUsage();
                    throw new BuildException("");
                }
            } else {
                // if it's no other arg, it may be the target
                targets.addElement(arg);
            }
        }

        if (msgOutputLevel >= Project.MSG_VERBOSE || justPrintVersion) {
            printVersion(msgOutputLevel);
        }

        if (justPrintUsage || justPrintVersion || justPrintDiagnostics) {
            if (justPrintUsage) {
                printUsage();
            }
            if (justPrintDiagnostics) {
                Diagnostics.doReport(System.out, msgOutputLevel);
            }
            return;
        }

        // if buildFile was not specified on the command line,
        if (buildFile == null) {
            // but -find then search for it
            if (searchForFile) {
                if (searchForThis != null) {
                    buildFile = findBuildFile(System.getProperty("user.dir"), searchForThis);
                } else {
                    // no search file specified: so search an existing default file
                    final Iterator<ProjectHelper> it = ProjectHelperRepository.getInstance().getHelpers();
                    do {
                        final ProjectHelper helper = it.next();
                        searchForThis = helper.getDefaultBuildFile();
                        if (msgOutputLevel >= Project.MSG_VERBOSE) {
                            System.out.println("Searching the default build file: " + searchForThis);
                        }
                        buildFile = findBuildFile(System.getProperty("user.dir"), searchForThis);
                    } while (buildFile == null && it.hasNext());
                }
                if (buildFile == null) {
                    throw new BuildException("Could not locate a build file!");
                }
            } else {
                // no build file specified: so search an existing default file
                final Iterator<ProjectHelper> it = ProjectHelperRepository.getInstance().getHelpers();
                do {
                    final ProjectHelper helper = it.next();
                    buildFile = new File(helper.getDefaultBuildFile());
                    if (msgOutputLevel >= Project.MSG_VERBOSE) {
                        System.out.println("Trying the default build file: " + buildFile);
                    }
                } while (!buildFile.exists() && it.hasNext());
            }
        }

        // make sure buildfile exists
        if (!buildFile.exists()) {
            System.out.println("Buildfile: " + buildFile + " does not exist!");
            throw new BuildException("Build failed");
        }

        if (buildFile.isDirectory()) {
            final File whatYouMeant = new File(buildFile, "build.xml");
            if (whatYouMeant.isFile()) {
                buildFile = whatYouMeant;
            } else {
                System.out.println("What? Buildfile: " + buildFile + " is a dir!");
                throw new BuildException("Build failed");
            }
        }

        // Normalize buildFile for re-import detection
        buildFile =
            FileUtils.getFileUtils().normalize(buildFile.getAbsolutePath());

        // Load the property files specified by -propertyfile
        loadPropertyFiles();

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

    // --------------------------------------------------------
    //    Methods for handling the command line arguments
    // --------------------------------------------------------

    /** Handle the -buildfile, -file, -f argument */
    private int handleArgBuildFile(final String[] args, int pos) {
        try {
            buildFile = new File(
                args[++pos].replace('/', File.separatorChar));
        } catch (final ArrayIndexOutOfBoundsException aioobe) {
            throw new BuildException(
                "You must specify a buildfile when using the -buildfile argument");
        }
        return pos;
    }

    /** Handle -listener argument */
    private int handleArgListener(final String[] args, int pos) {
        try {
            listeners.addElement(args[pos + 1]);
            pos++;
        } catch (final ArrayIndexOutOfBoundsException aioobe) {
            final String msg = "You must specify a classname when "
                + "using the -listener argument";
            throw new BuildException(msg);
        }
        return pos;
    }

    /** Handler -D argument */
    private int handleArgDefine(final String[] args, int argPos) {
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
        final String arg = args[argPos];
        String name = arg.substring(2);
        String value;
        final int posEq = name.indexOf('=');
        if (posEq > 0) {
            value = name.substring(posEq + 1);
            name = name.substring(0, posEq);
        } else if (argPos < args.length - 1) {
            value = args[++argPos];
        } else {
            throw new BuildException("Missing value for property "
                                     + name);
        }
        definedProps.put(name, value);
        return argPos;
    }

    /** Handle the -logger argument. */
    private int handleArgLogger(final String[] args, int pos) {
        if (loggerClassname != null) {
            throw new BuildException(
                "Only one logger class may be specified.");
        }
        try {
            loggerClassname = args[++pos];
        } catch (final ArrayIndexOutOfBoundsException aioobe) {
            throw new BuildException(
                "You must specify a classname when using the -logger argument");
        }
        return pos;
    }

    /** Handle the -inputhandler argument. */
    private int handleArgInputHandler(final String[] args, int pos) {
        if (inputHandlerClassname != null) {
            throw new BuildException("Only one input handler class may "
                                     + "be specified.");
        }
        try {
            inputHandlerClassname = args[++pos];
        } catch (final ArrayIndexOutOfBoundsException aioobe) {
            throw new BuildException("You must specify a classname when"
                                     + " using the -inputhandler"
                                     + " argument");
        }
        return pos;
    }

    /** Handle the -propertyfile argument. */
    private int handleArgPropertyFile(final String[] args, int pos) {
        try {
            propertyFiles.addElement(args[++pos]);
        } catch (final ArrayIndexOutOfBoundsException aioobe) {
            final String msg = "You must specify a property filename when "
                + "using the -propertyfile argument";
            throw new BuildException(msg);
        }
        return pos;
    }

    /** Handle the -nice argument. */
    private int handleArgNice(final String[] args, int pos) {
        try {
            threadPriority = Integer.decode(args[++pos]);
        } catch (final ArrayIndexOutOfBoundsException aioobe) {
            throw new BuildException(
                "You must supply a niceness value (1-10)"
                + " after the -nice option");
        } catch (final NumberFormatException e) {
            throw new BuildException("Unrecognized niceness value: "
                                     + args[pos]);
        }

        if (threadPriority < Thread.MIN_PRIORITY
            || threadPriority > Thread.MAX_PRIORITY) {
            throw new BuildException(
                "Niceness value is out of the range 1-10");
        }
        return pos;
    }

    // --------------------------------------------------------
    //    other methods
    // --------------------------------------------------------

    /** Load the property files specified by -propertyfile */
    private void loadPropertyFiles() {
        for (final String filename : propertyFiles) {
            final Properties props = new Properties();
            try (InputStream fis = Files.newInputStream(Paths.get(filename))) {
                props.load(fis);
            } catch (final IOException e) {
                System.out.println("Could not load property file "
                                   + filename + ": " + e.getMessage());
            }

            // ensure that -D properties take precedence
            props.stringPropertyNames().stream()
                    .filter(name -> definedProps.getProperty(name) == null)
                    .forEach(name -> definedProps.put(name, props.getProperty(name)));
        }
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
    @Deprecated
    private File getParentFile(final File file) {
        final File parent = file.getParentFile();

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
     * root of the file-system has been reached <code>null</code>
     * is returned.
     *
     * @param start  Leaf directory of search.
     *               Must not be <code>null</code>.
     * @param suffix  Suffix filename to look for in parents.
     *                Must not be <code>null</code>.
     *
     * @return A handle to the build file if one is found, <code>null</code> if not
     */
    private File findBuildFile(final String start, final String suffix) {
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
                return null;
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
    private void runBuild(final ClassLoader coreLoader) throws BuildException {

        if (!readyToRun) {
            return;
        }

        final ArgumentProcessorRegistry processorRegistry = ArgumentProcessorRegistry.getInstance();

        for (final ArgumentProcessor processor : processorRegistry.getProcessors()) {
            final List<String> extraArgs = extraArguments.get(processor.getClass());
            if (extraArgs != null) {
                if (processor.handleArg(extraArgs)) {
                    return;
                }
            }
        }

        final Project project = new Project();
        project.setCoreLoader(coreLoader);

        Throwable error = null;

        try {
            addBuildListeners(project);
            addInputHandler(project);

            final PrintStream savedErr = System.err;
            final PrintStream savedOut = System.out;
            final InputStream savedIn = System.in;
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
                        Thread.currentThread().setPriority(threadPriority);
                    } catch (final SecurityException swallowed) {
                        //we cannot set the priority here.
                        project.log("A security manager refused to set the -nice value");
                    }
                }

                setProperties(project);

                project.setKeepGoingMode(keepGoingMode);
                if (proxy) {
                    //proxy setup if enabled
                    final ProxySetup proxySetup = new ProxySetup(project);
                    proxySetup.enableProxies();
                }

                for (final ArgumentProcessor processor : processorRegistry.getProcessors()) {
                    final List<String> extraArgs = extraArguments.get(processor.getClass());
                    if (extraArgs != null) {
                        processor.prepareConfigure(project, extraArgs);
                    }
                }

                ProjectHelper.configureProject(project, buildFile);

                for (final ArgumentProcessor processor : processorRegistry.getProcessors()) {
                    final List<String> extraArgs = extraArguments.get(processor.getClass());
                    if (extraArgs != null) {
                        if (processor.handleArg(project, extraArgs)) {
                            return;
                        }
                    }
                }

                if (projectHelp) {
                    printDescription(project);
                    printTargets(project, msgOutputLevel > Project.MSG_INFO,
                            msgOutputLevel > Project.MSG_VERBOSE);
                    return;
                }

                // make sure that we have a target to execute
                if (targets.isEmpty()) {
                    if (project.getDefaultTarget() != null) {
                        targets.addElement(project.getDefaultTarget());
                    }
                }

                project.executeTargets(targets);
            } finally {
                System.setOut(savedOut);
                System.setErr(savedErr);
                System.setIn(savedIn);
            }
        } catch (final RuntimeException | Error exc) {
            error = exc;
            throw exc;
        } finally {
            if (!projectHelp) {
                try {
                    project.fireBuildFinished(error);
                } catch (final Throwable t) {
                    // yes, I know it is bad style to catch Throwable,
                    // but if we don't, we lose valuable information
                    System.err.println("Caught an exception while logging the"
                                       + " end of the build.  Exception was:");
                    t.printStackTrace(); //NOSONAR
                    if (error != null) {
                        System.err.println("There has been an error prior to"
                                           + " that:");
                        error.printStackTrace(); //NOSONAR
                    }
                    throw new BuildException(t); //NOSONAR
                }
            } else if (error != null) {
                project.log(error.toString(), Project.MSG_ERR);
            }
        }
    }

    private void setProperties(final Project project) {

        project.init();

        // resolve properties
        final PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(project);
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final Map raw = new HashMap(definedProps);
        @SuppressWarnings("unchecked")
        final Map<String, Object> props = raw;

        final ResolvePropertyMap resolver = new ResolvePropertyMap(project,
                NOPROPERTIES, propertyHelper.getExpanders());
        resolver.resolveAllProperties(props, null, false);

        // set user-define properties
        props.forEach((arg, value) -> project.setUserProperty(arg, String.valueOf(value)));

        project.setUserProperty(MagicNames.ANT_FILE,
                                buildFile.getAbsolutePath());
        project.setUserProperty(MagicNames.ANT_FILE_TYPE,
                                MagicNames.ANT_FILE_TYPE_FILE);

        // this list doesn't contain the build files default target,
        // which may be added later unless targets have been specified
        // on the command line. Therefore the property gets set again
        // in Project#executeTargets when we can be sure the list is
        // complete.
        // Setting it here allows top-level tasks to access the
        // property.
        project.setUserProperty(MagicNames.PROJECT_INVOKED_TARGETS,
                String.join(",", targets));
    }

    /**
     * Adds the listeners specified in the command line arguments,
     * along with the default listener, to the specified project.
     *
     * @param project The project to add listeners to.
     *                Must not be <code>null</code>.
     */
    protected void addBuildListeners(final Project project) {

        // Add the default listener
        project.addBuildListener(createLogger());

        final int count = listeners.size();
        for (int i = 0; i < count; i++) {
            final String className = listeners.elementAt(i);
            final BuildListener listener =
                    ClasspathUtils.newInstance(className,
                            Main.class.getClassLoader(), BuildListener.class);
            project.setProjectReference(listener);

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
    private void addInputHandler(final Project project) throws BuildException {
        InputHandler handler = null;
        if (inputHandlerClassname == null) {
            handler = new DefaultInputHandler();
        } else {
            handler = ClasspathUtils.newInstance(
                    inputHandlerClassname, Main.class.getClassLoader(),
                    InputHandler.class);
            project.setProjectReference(handler);
        }
        project.setInputHandler(handler);
    }

    /**
     * Creates the default build logger for sending build events to the ant
     * log.
     *
     * @return the logger instance for this build.
     */
    private BuildLogger createLogger() {
        BuildLogger logger = null;
        if (silent) {
            logger = new SilentLogger();
            msgOutputLevel = Project.MSG_WARN;
            emacsMode = true;
        } else if (loggerClassname != null) {
            try {
                logger = ClasspathUtils.newInstance(
                        loggerClassname, Main.class.getClassLoader(),
                        BuildLogger.class);
            } catch (final BuildException e) {
                System.err.println("The specified logger class "
                    + loggerClassname
                    + " could not be used because " + e.getMessage());
                throw e;
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
        System.out.println("ant [options] [target [target2 [target3] ...]]");
        System.out.println("Options: ");
        System.out.println("  -help, -h              print this message and exit");
        System.out.println("  -projecthelp, -p       print project help information and exit");
        System.out.println("  -version               print the version information and exit");
        System.out.println("  -diagnostics           print information that might be helpful to");
        System.out.println("                         diagnose or report problems and exit");
        System.out.println("  -quiet, -q             be extra quiet");
        System.out.println("  -silent, -S            print nothing but task outputs and build failures");
        System.out.println("  -verbose, -v           be extra verbose");
        System.out.println("  -debug, -d             print debugging information");
        System.out.println("  -emacs, -e             produce logging information without adornments");
        System.out.println("  -lib <path>            specifies a path to search for jars and classes");
        System.out.println("  -logfile <file>        use given file for log");
        System.out.println("    -l     <file>                ''");
        System.out.println("  -logger <classname>    the class which is to perform logging");
        System.out.println("  -listener <classname>  add an instance of class as a project listener");
        System.out.println("  -noinput               do not allow interactive input");
        System.out.println("  -buildfile <file>      use given buildfile");
        System.out.println("    -file    <file>              ''");
        System.out.println("    -f       <file>              ''");
        System.out.println("  -D<property>=<value>   use value for given property");
        System.out.println("  -keep-going, -k        execute all targets that do not depend");
        System.out.println("                         on failed target(s)");
        System.out.println("  -propertyfile <name>   load all properties from file with -D");
        System.out.println("                         properties taking precedence");
        System.out.println("  -inputhandler <class>  the class which will handle input requests");
        System.out.println("  -find <file>           (s)earch for buildfile towards the root of");
        System.out.println("    -s  <file>           the filesystem and use it");
        System.out.println("  -nice  number          A niceness value for the main thread:");
        System.out.println("                         1 (lowest) to 10 (highest); 5 is the default");
        System.out.println("  -nouserlib             Run ant without using the jar files from");
        System.out.println("                         ${user.home}/.ant/lib");
        System.out.println("  -noclasspath           Run ant without using CLASSPATH");
        System.out.println("  -autoproxy             Java1.5+: use the OS proxy settings");
        System.out.println("  -main <class>          override Ant's normal entry point");
        for (final ArgumentProcessor processor : ArgumentProcessorRegistry.getInstance().getProcessors()) {
            processor.printUsage(System.out);
        }
    }

    /**
     * Prints the Ant version information to <code>System.out</code>.
     *
     * @exception BuildException if the version information is unavailable
     */
    private static void printVersion(final int logLevel) throws BuildException {
        System.out.println(getAntVersion());
    }

    /**
     * Cache of the Ant version information when it has been loaded.
     */
    private static String antVersion = null;

    /**
     * Cache of the short Ant version information when it has been loaded.
     */
    private static String shortAntVersion = null;

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
                final Properties props = new Properties();
                final InputStream in =
                    Main.class.getResourceAsStream("/org/apache/tools/ant/version.txt");
                props.load(in);
                in.close();
                shortAntVersion = props.getProperty("VERSION");
                antVersion = "Apache Ant(TM) version " +
                        shortAntVersion +
                        " compiled on " +
                        props.getProperty("DATE");
            } catch (final IOException ioe) {
                throw new BuildException("Could not load the version information:"
                                         + ioe.getMessage());
            } catch (final NullPointerException npe) {
                throw new BuildException("Could not load the version information.");
            }
        }
        return antVersion;
    }

    /**
     * Returns the short Ant version information, if available. Once the information
     * has been loaded once, it's cached and returned from the cache on future
     * calls.
     *
     * @return the short Ant version information as a String
     *         (always non-<code>null</code>)
     *
     * @throws BuildException BuildException if the version information is unavailable
     * @since Ant 1.9.3
     */
    public static String getShortAntVersion() throws BuildException {
        if (shortAntVersion == null) {
            getAntVersion();
        }
        return shortAntVersion;
    }

     /**
      * Prints the description of a project (if there is one) to
      * <code>System.out</code>.
      *
      * @param project The project to display a description of.
      *                Must not be <code>null</code>.
      */
    private static void printDescription(final Project project) {
       if (project.getDescription() != null) {
          project.log(project.getDescription());
       }
    }

    /**
     * Targets in imported files with a project name
     * and not overloaded by the main build file will
     * be in the target map twice. This method
     * removes the duplicate target.
     * @param targets the targets to filter.
     * @return the filtered targets.
     */
    private static Map<String, Target> removeDuplicateTargets(final Map<String, Target> targets) {
        final Map<Location, Target> locationMap = new HashMap<>();
        targets.forEach((name, target) -> {
            final Target otherTarget = locationMap.get(target.getLocation());
            // Place this entry in the location map if
            //  a) location is not in the map
            //  b) location is in map, but its name is longer
            //     (an imported target will have a name. prefix)
            if (otherTarget == null || otherTarget.getName().length() > name.length()) {
                locationMap.put(target.getLocation(), target); // Smallest name wins
            }
        });
        return locationMap.values().stream()
                .collect(Collectors.toMap(Target::getName, target -> target, (a, b) -> b));
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
    private static void printTargets(final Project project, boolean printSubTargets,
            final boolean printDependencies) {
        // find the target with the longest name
        int maxLength = 0;
        final Map<String, Target> ptargets = removeDuplicateTargets(project.getTargets());
        // split the targets in top-level and sub-targets depending
        // on the presence of a description
        final Vector<String> topNames = new Vector<>();
        final Vector<String> topDescriptions = new Vector<>();
        final Vector<Enumeration<String>> topDependencies = new Vector<>();
        final Vector<String> subNames = new Vector<>();
        final Vector<Enumeration<String>> subDependencies = new Vector<>();

        for (final Target currentTarget : ptargets.values()) {
            final String targetName = currentTarget.getName();
            if (targetName.isEmpty()) {
                continue;
            }
            final String targetDescription = currentTarget.getDescription();
            // maintain a sorted list of targets
            if (targetDescription == null) {
                final int pos = findTargetPosition(subNames, targetName);
                subNames.insertElementAt(targetName, pos);
                if (printDependencies) {
                    subDependencies.insertElementAt(currentTarget.getDependencies(), pos);
                }
            } else {
                final int pos = findTargetPosition(topNames, targetName);
                topNames.insertElementAt(targetName, pos);
                topDescriptions.insertElementAt(targetDescription, pos);
                if (targetName.length() > maxLength) {
                    maxLength = targetName.length();
                }
                if (printDependencies) {
                    topDependencies.insertElementAt(currentTarget.getDependencies(), pos);
                }
            }
        }

        printTargets(project, topNames, topDescriptions, topDependencies,
                "Main targets:", maxLength);
        //if there were no main targets, we list all subtargets
        //as it means nothing has a description
        if (topNames.isEmpty()) {
            printSubTargets = true;
        }
        if (printSubTargets) {
            printTargets(project, subNames, null, subDependencies, "Other targets:", 0);
        }

        final String defaultTarget = project.getDefaultTarget();
        if (defaultTarget != null && !defaultTarget.isEmpty()) {
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
    private static int findTargetPosition(final Vector<String> names, final String name) {
        final int size = names.size();
        int res = size;
        for (int i = 0; i < size && res == size; i++) {
            if (name.compareTo(names.elementAt(i)) < 0) {
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
    private static void printTargets(final Project project, final Vector<String> names,
                                     final Vector<String> descriptions, final Vector<Enumeration<String>> dependencies,
                                     final String heading,
                                     final int maxlen) {
        // now, start printing the targets and their descriptions
        final String eol = System.lineSeparator();
        // got a bit annoyed that I couldn't find a pad function
        StringBuilder spaces = new StringBuilder("    ");
        while (spaces.length() <= maxlen) {
            spaces.append(spaces);
        }
        final StringBuilder msg = new StringBuilder();
        msg.append(heading).append(eol).append(eol);
        final int size = names.size();
        for (int i = 0; i < size; i++) {
            msg.append(" ");
            msg.append(names.elementAt(i));
            if (descriptions != null) {
                msg.append(
                    spaces.substring(0, maxlen - names.elementAt(i).length() + 2));
                msg.append(descriptions.elementAt(i));
            }
            msg.append(eol);
            if (!dependencies.isEmpty() && dependencies.elementAt(i).hasMoreElements()) {
                msg.append(StreamUtils.enumerationAsStream(dependencies.elementAt(i))
                        .collect(Collectors.joining(", ", "   depends on: ", eol)));
            }
        }
        project.log(msg.toString(), Project.MSG_WARN);
    }
}
