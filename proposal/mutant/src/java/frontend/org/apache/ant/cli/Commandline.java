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
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.ant.antcore.config.AntConfig;
import org.apache.ant.antcore.execution.Frame;
import org.apache.ant.antcore.frontend.FrontendException;
import org.apache.ant.antcore.frontend.FrontendUtils;
import org.apache.ant.antcore.modelparser.XMLProjectParser;
import org.apache.ant.antcore.xml.XMLParseException;
import org.apache.ant.common.event.BuildEvent;
import org.apache.ant.common.event.BuildListener;
import org.apache.ant.common.event.MessageLevel;
import org.apache.ant.common.logger.BuildLogger;
import org.apache.ant.common.logger.DefaultLogger;
import org.apache.ant.common.model.Project;
import org.apache.ant.common.util.DataValue;
import org.apache.ant.common.util.DemuxOutputStream;
import org.apache.ant.init.AntEnvironment;
import org.apache.ant.init.Frontend;
import org.apache.ant.init.InitUtils;

/**
 * This is the command line front end. It drives the core.
 *
 * @author Conor MacNeill
 * @created 9 January 2002
 */
public class Commandline implements Frontend {
    /** The initialisation configuration for Ant */
    private AntEnvironment antEnv;

    /** Stream that we are using for logging */
    private PrintStream out = System.out;

    /** Stream that we are using for logging error messages */
    private PrintStream err = System.err;

    /** Names of classes to add as listeners to project */
    private List listeners = new ArrayList(2);

    /** The list of targets to be evaluated in this invocation */
    private List targets = new ArrayList(4);

    /** The command line properties */
    private Map definedValues = new HashMap();

    /** The Config files to use in this run */
    private List configFiles = new ArrayList();

    /** This is the build to run.  */
    private String buildSource;

    /**
     * The Ant logger class. There may be only one logger. It will have the
     * right to use the 'out' PrintStream. The class must implements the
     * BuildLogger interface
     */
    private String loggerClassname = null;

    /** Our current message output status. Follows MessageLevel values */
    private int messageOutputLevel = MessageLevel.INFO;

    /** The logger that will be used for the build */
    private BuildLogger logger = null;

    /**
     * Adds a feature to the BuildListeners attribute of the Commandline
     * object
     *
     * @param eventSource the build event source to which listeners will be
     *      added.
     * @exception FrontendException if the necessary listener instances could
     *      not be created
     */
    protected void addBuildListeners(Frame eventSource)
         throws FrontendException {

        // Add the default listener
        eventSource.addBuildListener(logger);

        for (Iterator i = listeners.iterator(); i.hasNext();) {
            String className = (String) i.next();
            try {
                BuildListener listener =
                    (BuildListener) Class.forName(className).newInstance();
                eventSource.addBuildListener(listener);
            } catch (ClassCastException e) {
                System.err.println("The specified listener class "
                     + className +
                    " does not implement the Listener interface");
                throw new FrontendException("Unable to instantiate listener "
                     + className, e);
            } catch (Exception e) {
                System.err.println("Unable to instantiate specified listener "
                     + "class " + className + " : "
                     + e.getClass().getName());
                throw new FrontendException("Unable to instantiate listener "
                     + className, e);
            }
        }
    }

    /**
     * Get an option value
     *
     * @param args the full list of command line arguments
     * @param position the position in the args array where the value shoudl
     *      be
     * @param argType the option type
     * @return the value of the option
     * @exception FrontendException if the option cannot be read
     */
    private String getOption(String[] args, int position, String argType)
         throws FrontendException {
        String value = null;
        try {
            value = args[position];
        } catch (IndexOutOfBoundsException e) {
            throw new FrontendException("You must specify a value for the "
                 + argType + " argument");
        }
        return value;
    }


    /**
     * Start the command line front end for mutant.
     *
     * @param args the commandline arguments
     * @param antEnv Ant's initialization configuration
     */
    public void start(final String[] args, final AntEnvironment antEnv) {
        this.antEnv = antEnv;

        Frame mainFrame = null;
        Project project = null;
        try {
            parseArguments(args);
            createLogger();
            URL buildSourceURL = determineBuildFile();

            AntConfig config = new AntConfig();
            AntConfig userConfig =
                FrontendUtils.getAntConfig(antEnv.getUserConfigArea());
            AntConfig systemConfig
                 = FrontendUtils.getAntConfig(antEnv.getSystemConfigArea());

            if (systemConfig != null) {
                config.merge(systemConfig);
            }
            if (userConfig != null) {
                config.merge(userConfig);
            }

            for (Iterator i = configFiles.iterator(); i.hasNext();) {
                File configFile = (File) i.next();
                AntConfig runConfig
                     = FrontendUtils.getAntConfigFile(configFile);
                config.merge(runConfig);
            }

            if (buildSourceURL.getProtocol().equals("file")) {
                System.out.println("Buildfile: " + buildSource);
            } else if (!config.isRemoteProjectAllowed()) {
                throw new FrontendException("Remote Projects are not allowed: "
                     + buildSourceURL);
            } else {
                System.out.println("Build: " + buildSourceURL);
            }

            project = parseProject(buildSourceURL);

            // create the execution manager to execute the build
            mainFrame = new Frame(antEnv, config);
            OutputStream demuxOut
                 = new DemuxOutputStream(mainFrame, false);
            OutputStream demuxErr
                 = new DemuxOutputStream(mainFrame, true);
            System.setOut(new PrintStream(demuxOut));
            System.setErr(new PrintStream(demuxErr));
            addBuildListeners(mainFrame);
            mainFrame.setProject(project);
            mainFrame.initialize(definedValues);
        } catch (Throwable e) {
            if (logger != null) {
                BuildEvent finishedEvent
                     = new BuildEvent(this, BuildEvent.BUILD_FINISHED, e);
                logger.buildFinished(finishedEvent);
            } else {
                e.printStackTrace();
            }
            System.exit(1);
        }

        try {
            mainFrame.startBuild(targets);
            System.exit(0);
        } catch (Throwable t) {
            System.exit(1);
        }
    }

    /**
     * Use the XML parser to parse the build into a project model
     *
     * @param buildSourceURL the location of the build XML source.
     * @return a project model representation of the project file
     * @exception XMLParseException if the project cannot be parsed
     */
    private Project parseProject(URL buildSourceURL)
         throws XMLParseException {
        XMLProjectParser parser = new XMLProjectParser();
        Project project = parser.parseBuildFile(buildSourceURL);
        return project;
    }

    /**
     * Handle build argument
     *
     * @param buildSource the build to process
     */
    private void argBuild(String buildSource) {
        this.buildSource = buildSource;
    }

    /**
     * Handle the log file option
     *
     * @param arg the value of the log file option
     * @exception FrontendException if the log file is not writeable
     */
    private void argLogFile(String arg) throws FrontendException {
        try {
            File logFile = new File(arg);
            out = new PrintStream(new FileOutputStream(logFile));
            err = out;
        } catch (IOException ioe) {
            throw new FrontendException("Cannot write on the specified log " +
                "file. Make sure the path exists and " +
                "you have write permissions.", ioe);
        }
    }

    /**
     * Handle the logger attribute
     *
     * @param arg the logger classname
     * @exception FrontendException if a logger has already been defined
     */
    private void argLogger(String arg) throws FrontendException {
        if (loggerClassname != null) {
            throw new FrontendException("Only one logger class may be " +
                "specified.");
        }
        loggerClassname = arg;
    }


    /**
     * Determine the build to use
     *
     * @return the URL of the build source.
     * @exception FrontendException if the build cannot be found
     */
    private URL determineBuildFile() throws FrontendException {

        URL buildSourceURL = null;
        try {
            if (buildSource == null) {
                buildSource = FrontendUtils.DEFAULT_BUILD_FILENAME;
                File defaultBuildFile = new File(buildSource);
                if (!defaultBuildFile.exists()) {
                    String ant1File = FrontendUtils.DEFAULT_ANT1_FILENAME;
                    File ant1BuildFile = new File(ant1File);
                    if (ant1BuildFile.exists()) {
                        buildSource = ant1File;
                        defaultBuildFile = ant1BuildFile;
                    } else {
                        throw new FrontendException("No build file "
                             + FrontendUtils.DEFAULT_BUILD_FILENAME + " or "
                             + FrontendUtils.DEFAULT_ANT1_FILENAME + " found.");
                    }
                }
                buildSourceURL = InitUtils.getFileURL(defaultBuildFile);
            } else {
                // we have been given a file as a string - try to figure out if
                // it is a URL or just a file
                try {
                    buildSourceURL = new URL(buildSource);
                } catch (MalformedURLException e) {
                    // must be a file
                    File buildFile = new File(buildSource);
                    if (!buildFile.exists()) {
                        throw new FrontendException("Cannot find build: "
                             + buildSource);
                    }
                    buildSourceURL = InitUtils.getFileURL(buildFile);
                }
            }
            return buildSourceURL;
        } catch (MalformedURLException e) {
            throw new FrontendException("Build file " + buildSource
                 + " is not valid", e);
        }
    }

    /**
     * Parse the command line arguments.
     *
     * @param args the command line arguments
     * @exception FrontendException thrown when the command line contains some
     *      sort of error.
     */
    private void parseArguments(String[] args)
         throws FrontendException {

        int i = 0;
        while (i < args.length) {
            String arg = args[i++];

            if (arg.equals("-buildfile") || arg.equals("-file")
                 || arg.equals("-f")) {
                argBuild(getOption(args, i++, arg));
            } else if (arg.equals("-logfile") || arg.equals("-l")) {
                argLogFile(getOption(args, i++, arg));
            } else if (arg.equals("-quiet") || arg.equals("-q")) {
                messageOutputLevel = MessageLevel.WARNING;
            } else if (arg.equals("-verbose") || arg.equals("-v")) {
                // printVersion();
                messageOutputLevel = MessageLevel.VERBOSE;
            } else if (arg.equals("-debug")) {
                // printVersion();
                messageOutputLevel = MessageLevel.DEBUG;
            } else if (arg.equals("-config") || arg.equals("-c")) {
                configFiles.add(new File(getOption(args, i++, arg)));
            } else if (arg.equals("-listener")) {
                listeners.add(getOption(args, i++, arg));
            } else if (arg.equals("-logger")) {
                argLogger(getOption(args, i++, arg));
            } else if (arg.startsWith("-D")) {
                String name = arg.substring(2, arg.length());
                String value = null;
                int posEq = name.indexOf("=");
                if (posEq > 0) {
                    value = name.substring(posEq + 1);
                    name = name.substring(0, posEq);
                } else {
                    value = getOption(args, i++, arg);
                }
                definedValues.put(name,
                    new DataValue(value, DataValue.PRIORITY_USER));
            } else if (arg.startsWith("-")) {
                // we don't have any more args to recognize!
                System.out.println("Unknown option: " + arg);
                return;
            } else {
                // if it's no other arg, it must be a target
                targets.add(arg);
            }
        }

    }

    /**
     * Creates the default build logger for sending build events to the ant
     * log.
     *
     * @exception FrontendException if the logger cannot be instantiatd
     */
    private void createLogger() throws FrontendException {
        if (loggerClassname != null) {
            try {
                Class loggerClass = Class.forName(loggerClassname);
                logger = (BuildLogger) loggerClass.newInstance();
            } catch (ClassCastException e) {
                System.err.println("The specified logger class "
                     + loggerClassname +
                    " does not implement the BuildLogger interface");
                throw new FrontendException("Unable to instantiate logger "
                     + loggerClassname, e);
            } catch (Exception e) {
                System.err.println("Unable to instantiate specified logger "
                     + "class " + loggerClassname + " : "
                     + e.getClass().getName());
                throw new FrontendException("Unable to instantiate logger "
                     + loggerClassname, e);
            }
        } else {
            logger = new DefaultLogger();
        }

        logger.setMessageOutputLevel(messageOutputLevel);
        logger.setOutputPrintStream(out);
        logger.setErrorPrintStream(err);
    }
}

