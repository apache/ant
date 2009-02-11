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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.util.StringUtils;

/**
 * This is a special logger that is designed to profile builds.
 *
 * @since Ant1.8
 */
public class ProfileLogger extends DefaultLogger {

    private Map profileData = new HashMap(); // <Object, Date>

    /**
     * Logs a message to say that the target has started.
     *
     * @param event
     *            An event with any relevant extra information. Must not be
     *            <code>null</code>.
     */
    public void targetStarted(BuildEvent event) {
        Date now = new Date();
        String name = "Target " + event.getTarget().getName();
        logStart(event, now, name);
        profileData.put(event.getTarget(), now);
    }

    /**
     * Logs a message to say that the target has finished.
     *
     * @param event
     *            An event with any relevant extra information. Must not be
     *            <code>null</code>.
     */
    public void targetFinished(BuildEvent event) {
        Date start = (Date) profileData.remove(event.getTarget());
        String name = "Target " + event.getTarget().getName();
        logFinish(event, start, name);
    }

    /**
     * Logs a message to say that the task has started.
     *
     * @param event
     *            An event with any relevant extra information. Must not be
     *            <code>null</code>.
     */
    public void taskStarted(BuildEvent event) {
        String name = event.getTask().getTaskName();
        Date now = new Date();
        logStart(event, now, name);
        profileData.put(event.getTask(), now);
    }

    /**
     * Logs a message to say that the task has finished.
     *
     * @param event
     *            An event with any relevant extra information. Must not be
     *            <code>null</code>.
     */
    public void taskFinished(BuildEvent event) {
        Date start = (Date) profileData.remove(event.getTask());
        String name = event.getTask().getTaskName();
        logFinish(event, start, name);
    }

    private void logFinish(BuildEvent event, Date start, String name) {
        Date now = new Date();
        String msg = null;
        if (start != null) {
            long diff = now.getTime() - start.getTime();
            msg = StringUtils.LINE_SEP + name + ": finished" + now + " ("
                    + diff + "ms)";
        } else {
            msg = StringUtils.LINE_SEP + name + ": finished" + now
                    + " (unknown duration, start not detected)";
        }
        printMessage(msg, out, event.getPriority());
        log(msg);
    }

    private void logStart(BuildEvent event, Date start, String name) {
        String msg = StringUtils.LINE_SEP + name + ": started " + start;
        printMessage(msg, out, event.getPriority());
        log(msg);
    }

}
