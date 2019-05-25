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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;

/**
 * Retries the nested task a set number of times
 * @since Ant 1.7.1
 */
public class Retry extends Task implements TaskContainer {

    /**
     * task to execute n times
     */
    private Task nestedTask;

    /**
     * set retryCount to 1 by default
     */
    private int retryCount = 1;

    /**
     * The time to wait between retries in milliseconds, default to 0.
     */
    private int retryDelay = 0;

    /**
     * set the task
     * @param t the task to retry.
     */
    @Override
    public synchronized void addTask(Task t) {
        if (nestedTask != null) {
            throw new BuildException(
                "The retry task container accepts a single nested task (which may be a sequential task container)");
        }
        nestedTask = t;
    }

    /**
     * set the number of times to retry the task
     * @param n the number to use.
     */
    public void setRetryCount(int n) {
        retryCount = n;
    }

    /**
     * set the delay between retries (in milliseconds)
     * @param retryDelay the time between retries.
     * @since Ant 1.8.3
     */
    public void setRetryDelay(int retryDelay) {
        if (retryDelay < 0) {
            throw new BuildException("retryDelay must be a non-negative number");
        }
        this.retryDelay = retryDelay;
    }

    /**
     * perform the work
     * @throws BuildException if there is an error.
     */
    @Override
    public void execute() throws BuildException {
        StringBuilder errorMessages = new StringBuilder();
        for (int i = 0; i <= retryCount; i++) {
            try {
                nestedTask.perform();
                break;
            } catch (Exception e) {
                errorMessages.append(e.getMessage());
                if (i >= retryCount) {
                    throw new BuildException(String.format(
                            "Task [%s] failed after [%d] attempts; giving up.%nError messages:%n%s",
                            nestedTask.getTaskName(), retryCount, errorMessages), getLocation());
                }
                String msg;
                if (retryDelay > 0) {
                    msg = "Attempt [" + i + "]: error occurred; retrying after " + retryDelay + " ms...";
                } else {
                    msg = "Attempt [" + i + "]: error occurred; retrying...";
                }
                log(msg, e, Project.MSG_INFO);
                errorMessages.append(System.lineSeparator());
                if (retryDelay > 0) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        // Ignore Exception
                    }
                }
            }
        }
    }
}