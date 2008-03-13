/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.tools.ant.NoBannerLogger;
import org.apache.tools.ant.SubBuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.StringUtils;

import java.io.File;

/**
 * This is a special logger that is designed to make it easier to work
 * with big projects, those that use imports and
 * subant to build complex systems.
 *
 * @since Ant1.7.1
 */

public class BigProjectLogger extends NoBannerLogger implements SubBuildListener {

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
     * @param event
     */
    public void buildStarted(BuildEvent event) {
        super.buildStarted(event);
        subBuildStarted(event);
    }

    /**
     * {@inheritDoc}
     *
     * @param event
     */
    public void buildFinished(BuildEvent event) {
        subBuildFinished(event);
        super.buildFinished(event);
    }

    /**
     * Override point, extract the target name
     *
     * @param event the event to work on
     * @return the target name -including the owning project name (if non-null)
     */
    protected String extractTargetName(BuildEvent event) {
        String targetName = event.getTarget().getName();
        String projectName = extractProjectName(event);
        if (projectName != null && targetName != null) {
            return projectName + '.' + targetName;
        } else {
            return targetName;
        }
    }


    /**
     * {@inheritDoc}
     *
     * @param event An event with any relevant extra information. Must not be <code>null</code>.
     */
    public void subBuildStarted(BuildEvent event) {
        String name = extractNameOrDefault(event);
        Project project = event.getProject();

        File base = project == null ? null : project.getBaseDir();
        String path =
            (base == null)
            ? "With no base directory"
            : "In " + base.getAbsolutePath();
        printMessage(StringUtils.LINE_SEP + getHeader()
                + StringUtils.LINE_SEP + "Entering project " + name
                        + StringUtils.LINE_SEP + path
                        + StringUtils.LINE_SEP + getFooter(),
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
        String name = extractNameOrDefault(event);
        String failed = event.getException() != null ? "failing " : "";
        printMessage(StringUtils.LINE_SEP + getHeader()
                + StringUtils.LINE_SEP + "Exiting " + failed + "project "
                + name
                + StringUtils.LINE_SEP + getFooter(),
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

}
