/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.util.StringUtils;

import java.io.PrintStream;

/**
 * This is a class that represents a recorder. This is the listener to the
 * build process.
 *
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @version 0.5
 * @since Ant 1.4
 */
public class RecorderEntry implements BuildLogger {

    //////////////////////////////////////////////////////////////////////
    // ATTRIBUTES

    /** The name of the file associated with this recorder entry.  */
    private String filename = null;
    /** The state of the recorder (recorder on or off).  */
    private boolean record = true;
    /** The current verbosity level to record at.  */
    private int loglevel = Project.MSG_INFO;
    /** The output PrintStream to record to.  */
    private PrintStream out = null;
    /** The start time of the last know target.  */
    private long targetStartTime = 0l;
    /** Strip task banners if true.  */
    private boolean emacsMode = false;

    //////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS / INITIALIZERS

    /**
     * @param name The name of this recorder (used as the filename).
     */
    protected RecorderEntry(String name) {
        filename = name;
    }

    //////////////////////////////////////////////////////////////////////
    // ACCESSOR METHODS

    /**
     * @return the name of the file the output is sent to.
     */
    public String getFilename() {
        return filename;
    }


    /**
     * Turns off or on this recorder.
     *
     * @param state true for on, false for off, null for no change.
     */
    public void setRecordState(Boolean state) {
        if (state != null) {
            record = state.booleanValue();
        }
    }


    public void buildStarted(BuildEvent event) {
        log("> BUILD STARTED", Project.MSG_DEBUG);
    }


    public void buildFinished(BuildEvent event) {
        log("< BUILD FINISHED", Project.MSG_DEBUG);

        Throwable error = event.getException();

        if (error == null) {
            out.println(StringUtils.LINE_SEP + "BUILD SUCCESSFUL");
        } else {
            out.println(StringUtils.LINE_SEP + "BUILD FAILED"
                 + StringUtils.LINE_SEP);
            error.printStackTrace(out);
        }
        out.flush();
        out.close();
    }


    public void targetStarted(BuildEvent event) {
        log(">> TARGET STARTED -- " + event.getTarget(), Project.MSG_DEBUG);
        log(StringUtils.LINE_SEP + event.getTarget().getName() + ":",
            Project.MSG_INFO);
        targetStartTime = System.currentTimeMillis();
    }


    public void targetFinished(BuildEvent event) {
        log("<< TARGET FINISHED -- " + event.getTarget(), Project.MSG_DEBUG);

        String time = formatTime(System.currentTimeMillis() - targetStartTime);

        log(event.getTarget() + ":  duration " + time, Project.MSG_VERBOSE);
        out.flush();
    }


    public void taskStarted(BuildEvent event) {
        log(">>> TASK STARTED -- " + event.getTask(), Project.MSG_DEBUG);
    }


    public void taskFinished(BuildEvent event) {
        log("<<< TASK FINISHED -- " + event.getTask(), Project.MSG_DEBUG);
        out.flush();
    }


    public void messageLogged(BuildEvent event) {
        log("--- MESSAGE LOGGED", Project.MSG_DEBUG);

        StringBuffer buf = new StringBuffer();

        if (event.getTask() != null) {
            String name = event.getTask().getTaskName();

            if (!emacsMode) {
                String label = "[" + name + "] ";
                int size = DefaultLogger.LEFT_COLUMN_SIZE - label.length();

                for (int i = 0; i < size; i++) {
                    buf.append(" ");
                }// for
                buf.append(label);
            }// if
        }// if
        buf.append(event.getMessage());

        log(buf.toString(), event.getPriority());
    }


    /**
     * The thing that actually sends the information to the output.
     *
     * @param mesg The message to log.
     * @param level The verbosity level of the message.
     */
    private void log(String mesg, int level) {
        if (record && (level <= loglevel)) {
            out.println(mesg);
        }
    }


    public void setMessageOutputLevel(int level) {
        if (level >= Project.MSG_ERR && level <= Project.MSG_DEBUG) {
            loglevel = level;
        }
    }


    public void setOutputPrintStream(PrintStream output) {
        out = output;
    }


    public void setEmacsMode(boolean emacsMode) {
        this.emacsMode = emacsMode;
    }


    public void setErrorPrintStream(PrintStream err) {
        out = err;
    }


    private static String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;


        if (minutes > 0) {
            return Long.toString(minutes) + " minute"
                 + (minutes == 1 ? " " : "s ")
                 + Long.toString(seconds % 60) + " second"
                 + (seconds % 60 == 1 ? "" : "s");
        } else {
            return Long.toString(seconds) + " second"
                 + (seconds % 60 == 1 ? "" : "s");
        }

    }
}

