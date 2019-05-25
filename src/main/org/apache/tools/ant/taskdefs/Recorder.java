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

import java.util.Hashtable;
import java.util.Map;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.SubBuildListener;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.LogLevel;

/**
 * Adds a listener to the current build process that records the
 * output to a file.
 * <p>Several recorders can exist at the same time.  Each recorder is
 * associated with a file.  The filename is used as a unique identifier for
 * the recorders.  The first call to the recorder task with an unused filename
 * will create a recorder (using the parameters provided) and add it to the
 * listeners of the build.  All subsequent calls to the recorder task using
 * this filename will modify that recorders state (recording or not) or other
 * properties (like logging level).</p>
 * <p>Some technical issues: the file's print stream is flushed for &quot;finished&quot;
 * events (buildFinished, targetFinished and taskFinished), and is closed on
 * a buildFinished event.</p>
 * @see RecorderEntry
 * @version 0.5
 * @since Ant 1.4
 * @ant.task name="record" category="utility"
 */
public class Recorder extends Task implements SubBuildListener {

    //////////////////////////////////////////////////////////////////////
    // ATTRIBUTES

    /** The name of the file to record to. */
    private String filename = null;
    /**
     * Whether or not to append. Need Boolean to record an unset state (null).
     */
    private Boolean append = null;
    /**
     * Whether to start or stop recording. Need Boolean to record an unset
     * state (null).
     */
    private Boolean start = null;
    /** The level to log at. A level of -1 means not initialized yet. */
    private int loglevel = -1;
    /** Strip task banners if true.  */
    private boolean emacsMode = false;
    /** The list of recorder entries. */
    private static Map<String, RecorderEntry> recorderEntries = new Hashtable<>();

    //////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS / INITIALIZERS

    /**
     * Overridden so we can add the task as build listener.
     *
     * @since Ant 1.7
     */
    public void init() {
        getProject().addBuildListener(this);
    }

    //////////////////////////////////////////////////////////////////////
    // ACCESSOR METHODS

    /**
     * Sets the name of the file to log to, and the name of the recorder
     * entry.
     *
     * @param fname File name of logfile.
     */
    public void setName(String fname) {
        filename = fname;
    }


    /**
     * Sets the action for the associated recorder entry.
     *
     * @param action The action for the entry to take: start or stop.
     */
    public void setAction(ActionChoices action) {
        if (action.getValue().equalsIgnoreCase("start")) {
            start = Boolean.TRUE;
        } else {
            start = Boolean.FALSE;
        }
    }


    /**
     * Whether or not the logger should append to a previous file.
     * @param append if true, append to a previous file.
     */
    public void setAppend(boolean append) {
        this.append = (append ? Boolean.TRUE : Boolean.FALSE);
    }


    /**
     * Set emacs mode.
     * @param emacsMode if true use emacs mode
     */
    public void setEmacsMode(boolean emacsMode) {
        this.emacsMode = emacsMode;
    }


    /**
     * Sets the level to which this recorder entry should log to.
     * @param level the level to set.
     * @see VerbosityLevelChoices
     */
    public void setLoglevel(VerbosityLevelChoices level) {
        loglevel = level.getLevel();
    }

    //////////////////////////////////////////////////////////////////////
    // CORE / MAIN BODY

    /**
     * The main execution.
     * @throws BuildException on error
     */
    public void execute() throws BuildException {
        if (filename == null) {
            throw new BuildException("No filename specified");
        }

        getProject().log("setting a recorder for name " + filename,
            Project.MSG_DEBUG);

        // get the recorder entry
        RecorderEntry recorder = getRecorder(filename, getProject());
        // set the values on the recorder
        recorder.setMessageOutputLevel(loglevel);
        recorder.setEmacsMode(emacsMode);
        if (start != null) {
            if (start) {
                recorder.reopenFile();
                recorder.setRecordState(start);
            } else {
                recorder.setRecordState(start);
                recorder.closeFile();
            }
        }
    }

    //////////////////////////////////////////////////////////////////////
    // INNER CLASSES

    /**
     * A list of possible values for the <code>setAction()</code> method.
     * Possible values include: start and stop.
     */
    public static class ActionChoices extends EnumeratedAttribute {
        private static final String[] VALUES = {"start", "stop"};

        /**
         * @see EnumeratedAttribute#getValues()
         * {@inheritDoc}.
         */
        public String[] getValues() {
            return VALUES;
        }
    }


    /**
     * A list of possible values for the <code>setLoglevel()</code> method.
     * Possible values include: error, warn, info, verbose, debug.
     */
    public static class VerbosityLevelChoices extends LogLevel {
    }


    /**
     * Gets the recorder that's associated with the passed in name. If the
     * recorder doesn't exist, then a new one is created.
     * @param name the name of the recorder
     * @param proj the current project
     * @return a recorder
     * @throws BuildException on error
     */
    protected RecorderEntry getRecorder(String name, Project proj)
         throws BuildException {
        RecorderEntry entry = recorderEntries.get(name);

        if (entry == null) {
            // create a recorder entry
            entry = new RecorderEntry(name);

            if (append == null) {
                entry.openFile(false);
            } else {
                entry.openFile(append);
            }
            entry.setProject(proj);
            recorderEntries.put(name, entry);
        }

        return entry;
    }

    /**
     * Empty implementation required by SubBuildListener interface.
     * @param event ignored.
     * @since Ant 1.7
     */
    public void buildStarted(BuildEvent event) {
    }

    /**
     * Empty implementation required by SubBuildListener interface.
     * @param event ignored.
     * @since Ant 1.7
     */
    public void subBuildStarted(BuildEvent event) {
    }

    /**
     * Empty implementation required by SubBuildListener interface.
     * @param event ignored.
     * @since Ant 1.7
     */
    public void targetStarted(BuildEvent event) {
    }

    /**
     * Empty implementation required by SubBuildListener interface.
     * @param event ignored.
     * @since Ant 1.7
     */
    public void targetFinished(BuildEvent event) {
    }

    /**
     * Empty implementation required by SubBuildListener interface.
     * @param event ignored.
     * @since Ant 1.7
     */
    public void taskStarted(BuildEvent event) {
    }

    /**
     * Empty implementation required by SubBuildListener interface.
     * @param event ignored.
     * @since Ant 1.7
     */
    public void taskFinished(BuildEvent event) {
    }

    /**
     * Empty implementation required by SubBuildListener interface.
     * @param event ignored.
     * @since Ant 1.7
     */
    public void messageLogged(BuildEvent event) {
    }

    /**
     * Cleans recorder registry.
     * @param event ignored.
     * @since Ant 1.7
     */
    public void buildFinished(BuildEvent event) {
        cleanup();
    }

    /**
     * Cleans recorder registry, if this is the subbuild the task has
     * been created in.
     * @param event ignored.
     * @since Ant 1.7
     */
    public void subBuildFinished(BuildEvent event) {
        if (event.getProject() == getProject()) {
            cleanup();
        }
    }

    /**
     * cleans recorder registry and removes itself from BuildListener list.
     *
     * @since Ant 1.7
     */
    private void cleanup() {
        recorderEntries.entrySet().removeIf(e -> e.getValue().getProject() == getProject());
        getProject().removeBuildListener(this);
    }
}
