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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.util.DateUtils;
import org.apache.tools.ant.util.StringUtils;

/**
 * Writes build events to a PrintStream. Currently, it
 * only writes which targets are being executed, and
 * any messages that get logged.
 *
 */
public class DefaultLogger implements BuildLogger {
    /**
     * Size of left-hand column for right-justified task name.
     * @see #messageLogged(BuildEvent)
     */
    public static final int LEFT_COLUMN_SIZE = 12;

    // CheckStyle:VisibilityModifier OFF - bc
    /** PrintStream to write non-error messages to */
    protected PrintStream out;

    /** PrintStream to write error messages to */
    protected PrintStream err;

    /** Lowest level of message to write out */
    protected int msgOutputLevel = Project.MSG_ERR;

    /** Time of the start of the build */
    private long startTime = System.currentTimeMillis();

    // CheckStyle:ConstantNameCheck OFF - bc
    /** Line separator */
    @Deprecated
    protected static final String lSep = StringUtils.LINE_SEP;
    // CheckStyle:ConstantNameCheck ON

    /** Whether or not to use emacs-style output */
    protected boolean emacsMode = false;
    // CheckStyle:VisibilityModifier ON


    /**
     * Sole constructor.
     */
    public DefaultLogger() {
    }

    /**
     * Sets the highest level of message this logger should respond to.
     *
     * Only messages with a message level lower than or equal to the
     * given level should be written to the log.
     * <p>
     * Constants for the message levels are in the
     * {@link Project Project} class. The order of the levels, from least
     * to most verbose, is <code>MSG_ERR</code>, <code>MSG_WARN</code>,
     * <code>MSG_INFO</code>, <code>MSG_VERBOSE</code>,
     * <code>MSG_DEBUG</code>.
     * <p>
     * The default message level for DefaultLogger is Project.MSG_ERR.
     *
     * @param level the logging level for the logger.
     */
    public void setMessageOutputLevel(int level) {
        this.msgOutputLevel = level;
    }

    /**
     * Sets the output stream to which this logger is to send its output.
     *
     * @param output The output stream for the logger.
     *               Must not be <code>null</code>.
     */
    public void setOutputPrintStream(PrintStream output) {
        this.out = new PrintStream(output, true);
    }

    /**
     * Sets the output stream to which this logger is to send error messages.
     *
     * @param err The error stream for the logger.
     *            Must not be <code>null</code>.
     */
    public void setErrorPrintStream(PrintStream err) {
        this.err = new PrintStream(err, true);
    }

    /**
     * Sets this logger to produce emacs (and other editor) friendly output.
     *
     * @param emacsMode <code>true</code> if output is to be unadorned so that
     *                  emacs and other editors can parse files names, etc.
     */
    public void setEmacsMode(boolean emacsMode) {
        this.emacsMode = emacsMode;
    }

    /**
     * Responds to a build being started by just remembering the current time.
     *
     * @param event Ignored.
     */
    public void buildStarted(BuildEvent event) {
        startTime = System.currentTimeMillis();
    }

    static void throwableMessage(StringBuffer m, Throwable error, boolean verbose) {
        while (error instanceof BuildException) { // #43398
            Throwable cause = error.getCause();
            if (cause == null) {
                break;
            }
            String msg1 = error.toString();
            String msg2 = cause.toString();
            if (msg1.endsWith(msg2)) {
                m.append(msg1, 0, msg1.length() - msg2.length());
                error = cause;
            } else {
                break;
            }
        }
        if (verbose || !(error instanceof BuildException)) {
            m.append(StringUtils.getStackTrace(error));
        } else {
            m.append(String.format("%s%n", error));
        }
    }

    /**
     * Prints whether the build succeeded or failed,
     * any errors the occurred during the build, and
     * how long the build took.
     *
     * @param event An event with any relevant extra information.
     *              Must not be <code>null</code>.
     */
    public void buildFinished(BuildEvent event) {
        Throwable error = event.getException();
        StringBuffer message = new StringBuffer();
        if (error == null) {
            message.append(String.format("%n%s", getBuildSuccessfulMessage()));
        } else {
            message.append(String.format("%n%s%n", getBuildFailedMessage()));
            throwableMessage(message, error, Project.MSG_VERBOSE <= msgOutputLevel);
        }
        message.append(String.format("%nTotal time: %s",
                formatTime(System.currentTimeMillis() - startTime)));

        String msg = message.toString();
        if (error == null) {
            printMessage(msg, out, Project.MSG_VERBOSE);
        } else {
            printMessage(msg, err, Project.MSG_ERR);
        }
        log(msg);
    }

    /**
     * This is an override point: the message that indicates whether a build failed.
     * Subclasses can change/enhance the message.
     * @return The classic "BUILD FAILED"
     */
    protected String getBuildFailedMessage() {
        return "BUILD FAILED";
    }

    /**
     * This is an override point: the message that indicates that a build succeeded.
     * Subclasses can change/enhance the message.
     * @return The classic "BUILD SUCCESSFUL"
     */
    protected String getBuildSuccessfulMessage() {
        return "BUILD SUCCESSFUL";
    }

    /**
     * Logs a message to say that the target has started if this
     * logger allows information-level messages.
     *
     * @param event An event with any relevant extra information.
     *              Must not be <code>null</code>.
      */
    public void targetStarted(BuildEvent event) {
        if (Project.MSG_INFO <= msgOutputLevel
                && !event.getTarget().getName().isEmpty()) {
            String msg = String.format("%n%s:", event.getTarget().getName());
            printMessage(msg, out, event.getPriority());
            log(msg);
        }
    }

    /**
     * No-op implementation.
     *
     * @param event Ignored.
     */
    public void targetFinished(BuildEvent event) {
    }

    /**
     * No-op implementation.
     *
     * @param event Ignored.
     */
    public void taskStarted(BuildEvent event) {
    }

    /**
     * No-op implementation.
     *
     * @param event Ignored.
     */
    public void taskFinished(BuildEvent event) {
    }

    /**
     * Logs a message, if the priority is suitable.
     * In non-emacs mode, task level messages are prefixed by the
     * task name which is right-justified.
     *
     * @param event A BuildEvent containing message information.
     *              Must not be <code>null</code>.
     */
    public void messageLogged(BuildEvent event) {
        int priority = event.getPriority();
        // Filter out messages based on priority
        if (priority <= msgOutputLevel) {

            StringBuilder message = new StringBuilder();
            if (event.getTask() == null || emacsMode) {
                // emacs mode or there is no task
                message.append(event.getMessage());
            } else {
                // Print out the name of the task if we're in one
                String name = event.getTask().getTaskName();
                String label = "[" + name + "] ";
                int size = LEFT_COLUMN_SIZE - label.length();
                final String prefix = size > 0 ? Stream.generate(() -> " ")
                    .limit(size).collect(Collectors.joining()) + label : label;

                try (BufferedReader r =
                    new BufferedReader(new StringReader(event.getMessage()))) {
                    message.append(r.lines()
                        .collect(Collectors.joining(System.lineSeparator() + prefix, prefix, "")));
                } catch (IOException e) {
                    // shouldn't be possible
                    message.append(label).append(event.getMessage());
                }
            }
            Throwable ex = event.getException();
            if (Project.MSG_DEBUG <= msgOutputLevel && ex != null) {
                message.append(String.format("%n%s: ", ex.getClass().getSimpleName()))
                        .append(StringUtils.getStackTrace(ex));
            }

            String msg = message.toString();
            if (priority != Project.MSG_ERR) {
                printMessage(msg, out, priority);
            } else {
                printMessage(msg, err, priority);
            }
            log(msg);
        }
    }

    /**
     * Convenience method to format a specified length of time.
     *
     * @param millis Length of time to format, in milliseconds.
     *
     * @return the time as a formatted string.
     *
     * @see DateUtils#formatElapsedTime(long)
     */
    protected static String formatTime(final long millis) {
        return DateUtils.formatElapsedTime(millis);
    }

    /**
     * Prints a message to a PrintStream.
     *
     * @param message  The message to print.
     *                 Should not be <code>null</code>.
     * @param stream   A PrintStream to print the message to.
     *                 Must not be <code>null</code>.
     * @param priority The priority of the message.
     *                 (Ignored in this implementation.)
     */
    protected void printMessage(final String message,
                                final PrintStream stream,
                                final int priority) {
        stream.println(message);
    }

    /**
     * Empty implementation which allows subclasses to receive the
     * same output that is generated here.
     *
     * @param message Message being logged. Should not be <code>null</code>.
     */
    protected void log(String message) {
    }

    /**
     * Get the current time.
     * @return the current time as a formatted string.
     * @since Ant1.7.1
     */
    protected String getTimestamp() {
        Date date = new Date(System.currentTimeMillis());
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        return formatter.format(date);
    }

    /**
     * Get the project name or null
     * @param event the event
     * @return the project that raised this event
     * @since Ant1.7.1
     */
    protected String extractProjectName(BuildEvent event) {
        Project project = event.getProject();
        return (project != null) ? project.getName() : null;
    }
}
