/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.ant.cli;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.ant.cli.xml.XMLProjectParser;
import org.apache.ant.common.util.AntException;
import org.apache.ant.common.util.Location;
import org.apache.ant.common.util.MessageLevel;
import org.apache.ant.antcore.event.BuildListener;
import org.apache.ant.antcore.execution.ExecutionManager;
import org.apache.ant.antcore.model.Project;
import org.apache.ant.antcore.util.ConfigException;
import org.apache.ant.antcore.xml.XMLParseException;
import org.apache.ant.init.InitUtils;
import org.apache.ant.init.InitConfig;

/**
 * This is the command line front end to end. It drives the core
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 9 January 2002
 */
public class Commandline {
    /** The default build file name */
    public final static String DEFAULT_BUILD_FILENAME = "build.ant";

    /** The initialisation configuration for Ant */
    private InitConfig config;

    /** Stream that we are using for logging */
    private PrintStream out = System.out;

    /** Stream that we are using for logging error messages */
    private PrintStream err = System.err;

    /** Names of classes to add as listeners to project */
    private List listeners = new ArrayList(2);

    /** The list of targets to be evaluated in this invocation */
    private List targets = new ArrayList(4);

    /**
     * This is the build file to run. By default it is a file: type URL but
     * other URL protocols can be used.
     */
    private URL buildFileURL;

    /**
     * The Ant logger class. There may be only one logger. It will have the
     * right to use the 'out' PrintStream. The class must implements the
     * BuildLogger interface
     */
    private String loggerClassname = null;

    /** Our current message output status. Follows MessageLevel values */
    private int messageOutputLevel = MessageLevel.MSG_VERBOSE;

    /**
     * Start the command line front end for mutant.
     *
     * @param args the commandline arguments
     * @param config the initialisation configuration
     */
    public static void start(String[] args, InitConfig config) {
        // create a command line and use it to run ant
        Commandline commandline = new Commandline();
        commandline.process(args, config);
    }

    /**
     * Adds a feature to the BuildListeners attribute of the Commandline
     * object
     *
     * @param execManager The feature to be added to the BuildListeners
     *      attribute
     * @exception ConfigException if the necessary listener instances could
     *      not be created
     */
    protected void addBuildListeners(ExecutionManager execManager)
         throws ConfigException {

        // Add the default listener
        execManager.addBuildListener(createLogger());

        for (Iterator i = listeners.iterator(); i.hasNext(); ) {
            String className = (String)i.next();
            try {
                BuildListener listener =
                    (BuildListener)Class.forName(className).newInstance();
                execManager.addBuildListener(listener);
            } catch (ClassCastException e) {
                System.err.println("The specified listener class "
                     + className +
                    " does not implement the Listener interface");
                throw new ConfigException("Unable to instantiate listener "
                     + className, e);
            } catch (Exception e) {
                System.err.println("Unable to instantiate specified listener "
                     + "class " + className + " : "
                     + e.getClass().getName());
                throw new ConfigException("Unable to instantiate listener "
                     + className, e);
            }
        }
    }


    /**
     * Start the command line front end for mutant.
     *
     * @param args the commandline arguments
     * @param config the initialisation configuration
     */
    private void process(String[] args, InitConfig config) {
        this.config = config;
        try {
            parseArguments(args);
            Project project = parseProject();

            // create the execution manager to execute the build
            ExecutionManager executionManager = new ExecutionManager(config);
            addBuildListeners(executionManager);
            executionManager.runBuild(project, targets);
        } catch (AntException e) {
            Location location = e.getLocation();
            Throwable cause = e.getCause();
            System.out.println(e.getMessage());

            if (cause != null) {
                System.out.print("Root cause: " + cause.getClass().getName());
                if (!cause.getMessage().equals(e.getMessage())) {
                    System.out.print(": " + cause.getMessage());
                }
                System.out.println();
            }

            e.printStackTrace();

            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Use the XML parser to parse the build file into a project model
     *
     * @return a project model representation of the project file
     * @exception XMLParseException if the project cannot be parsed
     */
    private Project parseProject()
         throws XMLParseException {
        XMLProjectParser parser = new XMLProjectParser();
        Project project = parser.parseBuildFile(buildFileURL);
        return project;
    }

    /**
     * Parse the command line arguments.
     *
     * @param args the command line arguments
     * @exception ConfigException thrown when the command line contains some
     *      sort of error.
     */
    private void parseArguments(String[] args)
         throws ConfigException {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("-buildfile") || arg.equals("-file")
                 || arg.equals("-f")) {
                try {
                    String url = args[i + 1];
                    if (url.indexOf(":") == -1) {
                        // We convert any hash characters to their URL escape.
                        buildFileURL = InitUtils.getFileURL(new File(url));
                    } else {
                        buildFileURL = new URL(url);
                    }
                    i++;
                } catch (MalformedURLException e) {
                    System.err.println("Buildfile is not valid: " +
                        e.getMessage());
                    throw new ConfigException("Build file is not valid", e);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.println("You must specify a buildfile when " +
                        "using the -buildfile argument");
                    return;
                }
            } else if (arg.equals("-logfile") || arg.equals("-l")) {
                try {
                    File logFile = new File(args[i + 1]);
                    i++;
                    out = new PrintStream(new FileOutputStream(logFile));
                    err = out;
                } catch (IOException ioe) {
                    System.err.println("Cannot write on the specified log " +
                        "file. Make sure the path exists and " +
                        "you have write permissions.");
                    return;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    System.err.println("You must specify a log file when " +
                        "using the -log argument");
                    return;
                }
            } else if (arg.equals("-quiet") || arg.equals("-q")) {
                messageOutputLevel = MessageLevel.MSG_WARN;
            } else if (arg.equals("-verbose") || arg.equals("-v")) {
                // printVersion();
                messageOutputLevel = MessageLevel.MSG_VERBOSE;
            } else if (arg.equals("-listener")) {
                try {
                    listeners.add(args[i + 1]);
                    i++;
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    System.err.println("You must specify a classname when " +
                        "using the -listener argument");
                    return;
                }
            } else if (arg.equals("-logger")) {
                if (loggerClassname != null) {
                    System.err.println("Only one logger class may be " +
                        "specified.");
                    return;
                }
                try {
                    loggerClassname = args[++i];
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    System.err.println("You must specify a classname when " +
                        "using the -logger argument");
                    return;
                }
            } else if (arg.startsWith("-")) {
                // we don't have any more args to recognize!
                System.out.println("Unknown option: " + arg);
                return;
            } else {
                // if it's no other arg, it must be a target
                targets.add(arg);
            }
        }

        if (buildFileURL == null) {
            File defaultBuildFile = new File(DEFAULT_BUILD_FILENAME);
            try {
                buildFileURL = InitUtils.getFileURL(defaultBuildFile);
            } catch (MalformedURLException e) {
                System.err.println("Buildfile is not valid: " + e.getMessage());
                throw new ConfigException("Build file is not valid", e);
            }
        }
    }

    /**
     * Creates the default build logger for sending build events to the ant
     * log.
     *
     * @return the logger instance to be used for the build
     * @exception ConfigException if the logger cannot be instantiatd
     */
    private BuildLogger createLogger() throws ConfigException {
        BuildLogger logger = null;
        if (loggerClassname != null) {
            try {
                Class loggerClass = Class.forName(loggerClassname);
                logger = (BuildLogger)(loggerClass.newInstance());
            } catch (ClassCastException e) {
                System.err.println("The specified logger class "
                     + loggerClassname +
                    " does not implement the BuildLogger interface");
                throw new ConfigException("Unable to instantiate logger "
                     + loggerClassname, e);
            } catch (Exception e) {
                System.err.println("Unable to instantiate specified logger "
                     + "class " + loggerClassname + " : "
                     + e.getClass().getName());
                throw new ConfigException("Unable to instantiate logger "
                     + loggerClassname, e);
            }
        } else {
            logger = new DefaultLogger();
        }

        logger.setMessageOutputLevel(messageOutputLevel);
        logger.setOutputPrintStream(out);
        logger.setErrorPrintStream(err);

        return logger;
    }
}

