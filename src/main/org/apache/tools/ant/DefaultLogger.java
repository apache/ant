/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;

import org.apache.tools.ant.util.StringUtils;
import org.apache.tools.ant.util.DateUtils;

/**
 * Writes build events to a PrintStream. Currently, it
 * only writes which targets are being executed, and
 * any messages that get logged.
 *
 * @author Matt Foemmel
 */
public class DefaultLogger implements BuildLogger {
    /** 
     * Size of left-hand column for right-justified task name.
     * @see #messageLogged(BuildEvent)
     */
    public static final int LEFT_COLUMN_SIZE = 12;

    /** PrintStream to write non-error messages to */
    protected PrintStream out;
    /** PrintStream to write error messages to */
    protected PrintStream err;
    /** Lowest level of message to write out */
    protected int msgOutputLevel = Project.MSG_ERR;
    /** Time of the start of the build */
    private long startTime = System.currentTimeMillis();

    /** Line separator */
    protected static final String lSep = StringUtils.LINE_SEP;
    
    /** Whether or not to use emacs-style output */
    protected boolean emacsMode = false;

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
     * <P>
     * Constants for the message levels are in the 
     * {@link Project Project} class. The order of the levels, from least 
     * to most verbose, is <code>MSG_ERR</code>, <code>MSG_WARN</code>, 
     * <code>MSG_INFO</code>, <code>MSG_VERBOSE</code>, 
     * <code>MSG_DEBUG</code>.
     * <P>
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

    /**
     * Prints whether the build succeeded or failed,
     * any errors the occured during the build, and
     * how long the build took.
     * 
     * @param event An event with any relevant extra information.
     *              Must not be <code>null</code>.
     */
    public void buildFinished(BuildEvent event) {
        Throwable error = event.getException();
        StringBuffer message = new StringBuffer();

        if (error == null) {
            message.append(StringUtils.LINE_SEP);
            message.append("BUILD SUCCESSFUL");
        } else {
            message.append(StringUtils.LINE_SEP);
            message.append("BUILD FAILED");
            message.append(StringUtils.LINE_SEP);

            if (Project.MSG_VERBOSE <= msgOutputLevel ||
                !(error instanceof BuildException)) {
                message.append(StringUtils.getStackTrace(error));
            } else {
                if (error instanceof BuildException) {
                    message.append(error.toString()).append(lSep);
                } else {
                    message.append(error.getMessage()).append(lSep);
                }
            }
        }
        message.append(StringUtils.LINE_SEP);
        message.append("Total time: ");
        message.append(formatTime(System.currentTimeMillis() - startTime));

        String msg = message.toString();
        if (error == null) {
            printMessage(msg, out, Project.MSG_VERBOSE);
        } else {
            printMessage(msg, err, Project.MSG_ERR);
        }
        log(msg);
    }

    /**
     * Logs a message to say that the target has started if this
     * logger allows information-level messages.
     * 
     * @param event An event with any relevant extra information.
     *              Must not be <code>null</code>.
      */
    public void targetStarted(BuildEvent event) {
        if (Project.MSG_INFO <= msgOutputLevel) {
            String msg = StringUtils.LINE_SEP 
                + event.getTarget().getName() + ":";
            printMessage(msg, out, event.getPriority());
            log(msg);
        }
    }

    /**
     * No-op implementation.
     * 
     * @param event Ignored.
     */
    public void targetFinished(BuildEvent event) {}

    /**
     * No-op implementation.
     * 
     * @param event Ignored.
     */
    public void taskStarted(BuildEvent event) {}

    /**
     * No-op implementation.
     * 
     * @param event Ignored.
     */
    public void taskFinished(BuildEvent event) {}

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

            StringBuffer message = new StringBuffer();
            if (event.getTask() != null && !emacsMode) {
                // Print out the name of the task if we're in one
                String name = event.getTask().getTaskName();
                String label = "[" + name + "] ";
                int size = LEFT_COLUMN_SIZE - label.length();
                StringBuffer tmp = new StringBuffer();
                for (int i = 0; i < size; i++) {
                    tmp.append(" ");
                }
                tmp.append(label);
                label = tmp.toString();

                try {
                    BufferedReader r = 
                        new BufferedReader(
                            new StringReader(event.getMessage()));
                    String line = r.readLine();
                    boolean first = true;
                    while (line != null) {
                        if (!first) {
                            message.append(StringUtils.LINE_SEP);
                        }
                        first = false;
                        message.append(label).append(line);
                        line = r.readLine();
                    }
                } catch (IOException e) {
                    // shouldn't be possible
                    message.append(label).append(event.getMessage());
                }
            } else {
                message.append(event.getMessage());
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
     * @param millis Length of time to format, in milliseonds.
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
    protected void log(String message) {}
}
