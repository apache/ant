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

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.SubBuildListener;

/**
 * This is a special logger that is designed to make it easier to work
 * with big projects, those that use imports and
 * subant to build complex systems.
 *
 * @since Ant1.7.1
 */

public class BigProjectLogger extends SimpleBigProjectLogger
    implements SubBuildListener {

    private volatile boolean subBuildStartedRaised = false;
    private final Object subBuildLock = new Object();

    /**
     * Header string for the log.
     * {@value}
     */
    public static final String HEADER
        = "======================================================================";
    /**
     * Footer string for the log.
     * {@value}
     */
    public static final String FOOTER = HEADER;

    /**
    * This is an override point: the message that indicates whether
    * a build failed. Subclasses can change/enhance the
    * message.
    *
    * @return The classic "BUILD FAILED" plus a timestamp
    */
    protected String getBuildFailedMessage() {
        return super.getBuildFailedMessage() + TimestampedLogger.SPACER + getTimestamp();
    }

    /**
     * This is an override point: the message that indicates that
     * a build succeeded. Subclasses can change/enhance the
     * message.
     *
     * @return The classic "BUILD SUCCESSFUL" plus a timestamp
     */
    protected String getBuildSuccessfulMessage() {
        return super.getBuildSuccessfulMessage() + TimestampedLogger.SPACER + getTimestamp();
    }

    /**
     * {@inheritDoc}
     *
     * @param event BuildEvent
     */
    public void targetStarted(BuildEvent event) {
        maybeRaiseSubBuildStarted(event);
        super.targetStarted(event);
    }

    /**
     * {@inheritDoc}
     *
     * @param event BuildEvent
     */
    public void taskStarted(BuildEvent event) {
        maybeRaiseSubBuildStarted(event);
        super.taskStarted(event);
    }

    /**
     * {@inheritDoc}
     *
     * @param event BuildEvent
     */
    public void buildFinished(BuildEvent event) {
        maybeRaiseSubBuildStarted(event);
        subBuildFinished(event);
        super.buildFinished(event);
    }

    /**
     * {@inheritDoc}
     *
     * @param event BuildEvent
     */
    public void messageLogged(BuildEvent event) {
        maybeRaiseSubBuildStarted(event);
        super.messageLogged(event);
    }


    /**
     * {@inheritDoc}
     *
     * @param event An event with any relevant extra information. Must not be <code>null</code>.
     */
    public void subBuildStarted(BuildEvent event) {
        Project project = event.getProject();
        String path = (project == null) ? "With no base directory"
                : "In " + project.getBaseDir().getAbsolutePath();
        printMessage(String.format("%n%s%nEntering project %s%n%s%n%s", getHeader(),
                extractNameOrDefault(event), path, getFooter()),
                out,
                event.getPriority());
    }

    /**
     * Get the name of an event
     *
     * @param event the event name
     * @return the name or a default string
     */
    protected String extractNameOrDefault(BuildEvent event) {
        String name = extractProjectName(event);
        if (name == null) {
            name = "";
        } else {
            name = '"' + name + '"';
        }
        return name;
    }

    /** {@inheritDoc} */
    public void subBuildFinished(BuildEvent event) {
        printMessage(String.format("%n%s%nExiting %sproject %s%n%s",
                getHeader(), event.getException() != null ? "failing " : "",
                extractNameOrDefault(event), getFooter()),
                out,
                event.getPriority());
    }

    /**
     * Override point: return the header string for the entry/exit message
     * @return the header string
     */
    protected String getHeader() {
        return HEADER;
    }

    /**
     * Override point: return the footer string for the entry/exit message
     * @return the footer string
     */
    protected String getFooter() {
        return FOOTER;
    }

    private void maybeRaiseSubBuildStarted(BuildEvent event) {
        // double checked locking should be OK since the flag is write-once
        if (!subBuildStartedRaised) {
            synchronized (subBuildLock) {
                if (!subBuildStartedRaised) {
                    subBuildStartedRaised = true;
                    subBuildStarted(event);
                }
            }
        }
    }
}
