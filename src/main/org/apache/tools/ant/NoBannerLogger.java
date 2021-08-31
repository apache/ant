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

/**
 * Extends DefaultLogger to strip out empty targets.
 *
 */
public class NoBannerLogger extends DefaultLogger {

    // CheckStyle:VisibilityModifier OFF - bc
    /**
     * Name of the current target, if it should
     * be displayed on the next message. This is
     * set when a target starts building, and reset
     * to <code>null</code> after the first message for
     * the target is logged.
     */
    protected String targetName;
    // CheckStyle:VisibilityModifier ON

    /** Sole constructor. */
    public NoBannerLogger() {
    }

    /**
     * Notes the name of the target so it can be logged
     * if it generates any messages.
     *
     * @param event A BuildEvent containing target information.
     *              Must not be <code>null</code>.
     */
    public synchronized void targetStarted(BuildEvent event) {
        targetName = extractTargetName(event);
    }

    /**
     * Override point, extract the target name
     * @param event the event to work on
     * @return the target name to print
     * @since Ant1.7.1
     */
    protected String extractTargetName(BuildEvent event) {
        return event.getTarget().getName();
    }

    /**
     * Resets the current target name to <code>null</code>.
     *
     * @param event Ignored in this implementation.
     */
    public synchronized void targetFinished(BuildEvent event) {
        targetName = null;
    }

    /**
     * Logs a message for a target if it is of an appropriate
     * priority, also logging the name of the target if this
     * is the first message which needs to be logged for the
     * target.
     *
     * @param event A BuildEvent containing message information.
     *              Must not be <code>null</code>.
     */
    public void messageLogged(BuildEvent event) {

        if (event.getPriority() > msgOutputLevel
            || null == event.getMessage()
            || event.getMessage().trim().isEmpty()) {
                return;
        }

        synchronized (this) {
            if (null != targetName) {
                out.printf("%n%s:%n", targetName);
                targetName = null;
            }
        }

        super.messageLogged(event);
    }
}
