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
package org.apache.tools.ant.taskdefs;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.SubBuildListener;
import org.apache.tools.ant.util.FileUtils;

/**
 * This is a class that represents a recorder. This is the listener to the
 * build process.
 *
 * @since Ant 1.4
 */
public class RecorderEntry implements BuildLogger, SubBuildListener {

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
    private long targetStartTime = 0L;
    /** Strip task banners if true.  */
    private boolean emacsMode = false;
    /** project instance the recorder is associated with */
    private Project project;

    //////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS / INITIALIZERS

    /**
     * @param name The name of this recorder (used as the filename).
     */
    protected RecorderEntry(String name) {
        targetStartTime = System.currentTimeMillis();
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
            flush();
            record = state;
        }
    }

    /**
     * @see org.apache.tools.ant.BuildListener#buildStarted(BuildEvent)
     * {@inheritDoc}.
     */
    public void buildStarted(BuildEvent event) {
        log("> BUILD STARTED", Project.MSG_DEBUG);
    }

    /**
     * @see org.apache.tools.ant.BuildListener#buildFinished(BuildEvent)
     * {@inheritDoc}.
     */
    public void buildFinished(BuildEvent event) {
        log("< BUILD FINISHED", Project.MSG_DEBUG);

        if (record && out != null) {
            Throwable error = event.getException();

            if (error == null) {
                out.printf("%nBUILD SUCCESSFUL%n");
            } else {
                out.printf("%nBUILD FAILED%n%n");
                error.printStackTrace(out); //NOSONAR
            }
        }
        cleanup();
    }

    /**
     * Cleans up any resources held by this recorder entry at the end
     * of a subbuild if it has been created for the subbuild's project
     * instance.
     *
     * @param event the buildFinished event
     *
     * @since Ant 1.6.2
     */
    public void subBuildFinished(BuildEvent event) {
        if (event.getProject() == project) {
            cleanup();
        }
    }

    /**
     * Empty implementation to satisfy the BuildListener interface.
     *
     * @param event the buildStarted event
     *
     * @since Ant 1.6.2
     */
    public void subBuildStarted(BuildEvent event) {
    }

    /**
     * @see org.apache.tools.ant.BuildListener#targetStarted(BuildEvent)
     * {@inheritDoc}.
     */
    public void targetStarted(BuildEvent event) {
        log(">> TARGET STARTED -- " + event.getTarget(), Project.MSG_DEBUG);
        log(String.format("%n%s:", event.getTarget().getName()), Project.MSG_INFO);
        targetStartTime = System.currentTimeMillis();
    }

    /**
     * @see org.apache.tools.ant.BuildListener#targetFinished(BuildEvent)
     * {@inheritDoc}.
     */
    public void targetFinished(BuildEvent event) {
        log("<< TARGET FINISHED -- " + event.getTarget(), Project.MSG_DEBUG);

        String time = formatTime(System.currentTimeMillis() - targetStartTime);

        log(event.getTarget() + ":  duration " + time, Project.MSG_VERBOSE);
        flush();
    }

    /**
     * @see org.apache.tools.ant.BuildListener#taskStarted(BuildEvent)
     * {@inheritDoc}.
     */
    public void taskStarted(BuildEvent event) {
        log(">>> TASK STARTED -- " + event.getTask(), Project.MSG_DEBUG);
    }

    /**
     * @see org.apache.tools.ant.BuildListener#taskFinished(BuildEvent)
     * {@inheritDoc}.
     */
    public void taskFinished(BuildEvent event) {
        log("<<< TASK FINISHED -- " + event.getTask(), Project.MSG_DEBUG);
        flush();
    }

    /**
     * @see org.apache.tools.ant.BuildListener#messageLogged(BuildEvent)
     * {@inheritDoc}.
     */
    public void messageLogged(BuildEvent event) {
        log("--- MESSAGE LOGGED", Project.MSG_DEBUG);

        final StringBuilder buf = new StringBuilder();

        if (event.getTask() != null) {
            String name = event.getTask().getTaskName();

            if (!emacsMode) {
                String label = "[" + name + "] ";
                int size = DefaultLogger.LEFT_COLUMN_SIZE - label.length();

                for (int i = 0; i < size; i++) {
                    buf.append(" ");
                }
                buf.append(label);
            }
        }
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
        if (record && (level <= loglevel) && out != null) {
            out.println(mesg);
        }
    }

    private void flush() {
        if (record && out != null) {
            out.flush();
        }
    }

    /**
     * @see BuildLogger#setMessageOutputLevel(int)
     * {@inheritDoc}.
     */
    public void setMessageOutputLevel(int level) {
        if (level >= Project.MSG_ERR && level <= Project.MSG_DEBUG) {
            loglevel = level;
        }
    }

    /**
     * @see BuildLogger#setOutputPrintStream(PrintStream)
     * {@inheritDoc}.
     */
    public void setOutputPrintStream(PrintStream output) {
        closeFile();
        out = output;
    }


    /**
     * @see BuildLogger#setEmacsMode(boolean)
     * {@inheritDoc}.
     */
    public void setEmacsMode(boolean emacsMode) {
        this.emacsMode = emacsMode;
    }


    /**
     * @see BuildLogger#setErrorPrintStream(PrintStream)
     * {@inheritDoc}.
     */
    public void setErrorPrintStream(PrintStream err) {
        setOutputPrintStream(err);
    }


    private static String formatTime(long millis) {
        // CheckStyle:MagicNumber OFF
        long seconds = millis / 1000;
        long minutes = seconds / 60;


        if (minutes > 0) {
            return minutes + " minute"
                 + (minutes == 1 ? " " : "s ")
                 + seconds % 60 + " second"
                 + (seconds % 60 == 1 ? "" : "s");
        } else {
            return seconds + " second"
                 + (seconds % 60 == 1 ? "" : "s");
        }
        // CheckStyle:MagicNumber ON
    }

    /**
     * Set the project associated with this recorder entry.
     *
     * @param project the project instance
     *
     * @since 1.6.2
     */
    public void setProject(Project project) {
        this.project = project;
        if (project != null) {
            project.addBuildListener(this);
        }
    }

    /**
     * Get the project associated with this recorder entry.
     *
     * @return Project
     * @since 1.8.0
     */
    public Project getProject() {
        return project;
    }

    /**
     * @since 1.6.2
     */
    public void cleanup() {
        closeFile();
        if (project != null) {
            project.removeBuildListener(this);
        }
        project = null;
    }

    /**
     * Initially opens the file associated with this recorder.
     * Used by Recorder.
     * @param append Indicates if output must be appended to the logfile or that
     * the logfile should be overwritten.
     * @throws BuildException if something goes wrong
     * @since 1.6.3
     */
    void openFile(boolean append) throws BuildException {
        openFileImpl(append);
    }

    /**
     * Closes the file associated with this recorder.
     * Used by Recorder.
     * @since 1.6.3
     */
    void closeFile() {
        if (out != null) {
            out.close();
            out = null;
        }
    }

    /**
     * Re-opens the file associated with this recorder.
     * Used by Recorder.
     * @throws BuildException if something goes wrong
     * @since 1.6.3
     */
    void reopenFile() throws BuildException {
        openFileImpl(true);
    }

    private void openFileImpl(boolean append) throws BuildException {
        if (out == null) {
            try {
                out = new PrintStream(FileUtils.newOutputStream(Paths.get(filename), append));
            } catch (IOException ioe) {
                throw new BuildException("Problems opening file using a "
                                         + "recorder entry", ioe);
            }
        }
    }

}
