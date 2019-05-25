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
package org.apache.tools.ant.util;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * A facade that makes logging nicer to use.
 */
public final class TaskLogger {
    /**
     * Task to use to do logging.
     */
    private Task task;

    /**
     * Constructor for the TaskLogger
     * @param task the task
     */
    public TaskLogger(final Task task) {
        this.task = task;
    }

    /**
     * Log a message with <code>MSG_INFO</code> priority
     * @param message the message to log
     */
    public void info(final String message) {
        task.log(message, Project.MSG_INFO);
    }

    /**
     * Log a message with <code>MSG_ERR</code> priority
     * @param message the message to log
     */
    public void error(final String message) {
        task.log(message, Project.MSG_ERR);
    }

    /**
     * Log a message with <code>MSG_WARN</code> priority
     * @param message the message to log
     */
    public void warning(final String message) {
        task.log(message, Project.MSG_WARN);
    }

    /**
     * Log a message with <code>MSG_VERBOSE</code> priority
     * @param message the message to log
     */
    public void verbose(final String message) {
        task.log(message, Project.MSG_VERBOSE);
    }

    /**
     * Log a message with <code>MSG_DEBUG</code> priority
     * @param message the message to log
     */
    public void debug(final String message) {
        task.log(message, Project.MSG_DEBUG);
    }
}
