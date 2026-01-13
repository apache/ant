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
package org.apache.tools.ant.listener;

import static org.apache.tools.ant.Project.MSG_DEBUG;
import static org.apache.tools.ant.Project.MSG_ERR;
import static org.apache.tools.ant.Project.MSG_INFO;
import static org.apache.tools.ant.Project.MSG_VERBOSE;
import static org.apache.tools.ant.Project.MSG_WARN;

import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.SubBuildListener;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.util.StringUtils;

/**
 * <p>
 * Listener which sends events to Log4j2 logging system.
 * <p>
 * <p>
 * The following names are used for the loggers:
 * </p>
 * <ul>
 * <li><tt>PROJECT_NAME</tt> - for project events,</li>
 * <li><tt>PROJECT_NAME.TARGET_NAME</tt> - for target events,</li>
 * <li><tt>PROJECT_NAME.TARGET_NAME.TASK_NAME</tt> - for task events.</li>
 * </ul>
 *
 * <p>
 * In all names we replace "." with "_" to allow an easy extraction of the event
 * source name. Empty names are replaced with 'global'.
 * </p>
 * <p>
 * The location information contains the Java class name of the event source,
 * while the file name and line number refer to the Ant build script.
 * </p>
 */
public class Log4j2Listener implements SubBuildListener, BuildLogger {

    private static final Level VERBOSE = Level.forName("VERBOSE", 450);
    private static final Marker PROJECT = MarkerManager.getMarker("PROJECT");
    private static final Marker TARGET = MarkerManager.getMarker("TARGET").addParents(PROJECT);
    private static final Marker TASK = MarkerManager.getMarker("TASK").addParents(TARGET);
    private static final String LOGGER_SEP = ".";
    private static final String UNDERSCORE = "_";

    final LoggerContext context;
    private final Map<Object, Logger> loggers = new ConcurrentHashMap<>();

    public Log4j2Listener() {
        // Initializes the logger context before Ant performs System.out and System.err
        // redirections, but after the `-logfile` redirection.
        this.context = LogManager.getContext(false);
    }

    // Used in tests
    Log4j2Listener(final LoggerContext context) {
        this.context = context;
    }

    @Override
    public void setMessageOutputLevel(int priority) {
        final Level level = toLog4jLevel(priority);
        try {
            final Object config = context.getClass().getMethod("getConfiguration").invoke(context);
            final Object loggerConfig = config.getClass().getMethod("getRootLogger").invoke(config);
            final Level oldLevel = (Level) loggerConfig.getClass().getMethod("getLevel").invoke(loggerConfig);
            if (!oldLevel.equals(level)) {
                loggerConfig.getClass().getMethod("setLevel", Level.class).invoke(loggerConfig, level);
                context.getClass().getMethod("updateLoggers").invoke(context);
            }
        } catch (ReflectiveOperationException e) {
            context.getLogger("")
                   .warn("Log level selection requires the Log4j2 Core backend.");
        }
    }

    @Override
    public void setOutputPrintStream(PrintStream output) {
        // Not possible, but also not necessary, since System.out has the correct value,
        // when this class is instantiated.
    }

    @Override
    public void setEmacsMode(boolean emacsMode) {
        // Not supported
    }

    @Override
    public void setErrorPrintStream(PrintStream err) {
        // Not possible, but also not necessary, since System.err has the correct value,
        // when this class is instantiated.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildStarted(final BuildEvent event) {
        this.getLogBuilder(event, MSG_INFO)
            .log("Build started.");
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void buildFinished(final BuildEvent event) {
        final boolean success = event.getException() == null;
        this.getLogBuilder(event, success ? MSG_INFO : MSG_ERR)
            .log("Build finished{}.", success ? "" : "with error");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subBuildStarted(BuildEvent event) {
        this.getLogBuilder(event, MSG_INFO)
            .log("Project \"{}\" started.", defaultString(extractName(event.getProject())));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subBuildFinished(BuildEvent event) {
        final boolean success = event.getException() == null;
        final String projectName = defaultString(extractName(event.getProject()));
        this.getLogBuilder(event, success ? MSG_INFO : MSG_ERR)
            .log("Project \"{}\" finished{}.", projectName, success ? "" : "with error");
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void targetStarted(final BuildEvent event) {
        this.getLogBuilder(event, MSG_INFO)
            .log("Target \"{}\" started.", defaultString(extractName(event.getTarget())));
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void targetFinished(final BuildEvent event) {
        final boolean success = event.getException() == null;
        final String targetName = defaultString(extractName(event.getTarget()));
        this.getLogBuilder(event, success ? MSG_INFO : MSG_ERR)
            .log("Target \"{}\" finished{}.", targetName, success ? "" : "with error");
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void taskStarted(final BuildEvent event) {
        this.getLogBuilder(event, MSG_INFO)
            .log("Task \"{}\" started.", extractName(event.getTask()));
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void taskFinished(final BuildEvent event) {
        final boolean success = event.getException() == null;
        this.getLogBuilder(event, success ? MSG_INFO : MSG_ERR)
            .log("Task \"{}\" finished{}.", extractName(event.getTask()), success ? "" : "with error");
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void messageLogged(final BuildEvent event) {
        this.getLogBuilder(event, event.getPriority())
            .log(event.getMessage());
    }

    private static String extractName(final Project project) {
        return project != null ? StringUtils.trimToNull(project.getName()) : null;
    }

    private static String extractName(final Target target) {
        return target != null ? StringUtils.trimToNull(target.getName()) : null;
    }

    private static String extractName(final Task task) {
        return task != null ? task.getTaskName() : null;
    }

    private static String defaultString(final String str) {
        return str != null ? str : "";
    }

    static String getLoggerName(final BuildEvent event) {
        return Stream.of(extractName(event.getProject()), extractName(event.getTarget()), extractName(event.getTask()))
                .filter(Objects::nonNull)
                .map(name -> name.replace(LOGGER_SEP, UNDERSCORE))
                .collect(Collectors.joining(LOGGER_SEP));
    }

    private Logger getLogger(final BuildEvent event) {
        Logger logger = loggers.get(event.getSource());
        if (logger == null) {
            final String loggerName = getLoggerName(event);
            logger = context.getLogger(loggerName != null ? loggerName : "");
            loggers.put(event.getSource(), logger);
        }
        return logger;
    }

    static Marker getMarker(final BuildEvent event) {
        final Object source = event.getSource();
        return source instanceof Task ? TASK : source instanceof Target ? TARGET : PROJECT;
    }

    private static Level toLog4jLevel(int priority) {
        switch (priority) {
        case MSG_DEBUG:
            return Level.DEBUG;
        case MSG_VERBOSE:
            return VERBOSE;
        case MSG_INFO:
            return Level.INFO;
        case MSG_WARN:
            return Level.WARN;
        case MSG_ERR:
        default:
            return Level.ERROR;
        }
    }

    static StackTraceElement extractLocation(final BuildEvent event) {
        Object source = event.getSource();
        if (source instanceof UnknownElement) {
            final Task task = ((UnknownElement) source).getTask();
            if (task != null) {
                source = task;
            }
        }
        // We use the source's class name for the StackTraceElement
        final String className = source.getClass().getName();
        if (source instanceof Project) {
            final Project project = event.getProject();
            return new StackTraceElement(className, "", project.getUserProperty(MagicNames.ANT_FILE), -1);
        }
        final Location location = source instanceof Target ? event.getTarget().getLocation() : event.getTask().getLocation();
        return new StackTraceElement(className, "", location.getFileName(), location.getLineNumber());
    }

    private LogBuilder getLogBuilder(final BuildEvent event, int priority) {
        return this.getLogger(event)
                .atLevel(toLog4jLevel(priority))
                .withMarker(getMarker(event))
                .withLocation(extractLocation(event))
                .withThrowable(event.getException());
    }
}
