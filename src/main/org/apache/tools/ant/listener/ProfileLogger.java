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

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.DefaultLogger;

/**
 * This is a special logger that is designed to profile builds.
 *
 * @since Ant1.8
 */
public class ProfileLogger extends DefaultLogger {

    private Map<Object, Date> profileData = new ConcurrentHashMap<>();

    /**
     * Logs a message to say that the target has started.
     *
     * @param event
     *            An event with any relevant extra information. Must not be
     *            <code>null</code>.
     */
    @Override
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
    @Override
    public void targetFinished(BuildEvent event) {
        Date start = profileData.remove(event.getTarget());
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
    @Override
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
    @Override
    public void taskFinished(BuildEvent event) {
        Date start = profileData.remove(event.getTask());
        String name = event.getTask().getTaskName();
        logFinish(event, start, name);
    }

    private void logFinish(BuildEvent event, Date start, String name) {
        Date now = new Date();
        String msg;
        if (start != null) {
            long diff = now.getTime() - start.getTime();
            msg = String.format("%n%s: finished %s (%d)", name, now, diff);
        } else {
            msg = String.format("%n%s: finished %s (unknown duration, start not detected)",
                    name, now);
        }
        printMessage(msg, out, event.getPriority());
        log(msg);
    }

    private void logStart(BuildEvent event, Date start, String name) {
        String msg = String.format("%n%s: started %s", name, start);
        printMessage(msg, out, event.getPriority());
        log(msg);
    }

}
