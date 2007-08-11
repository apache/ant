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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;

/**
 * Retries the nested task a set number of times
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
     * set the task
     */
    public void addTask(Task t) {
        nestedTask = t;
    }

    /**
     * set the number of times to retry the task
     * @param n
     */
    public void setRetryCount(int n) {
        retryCount = n;
    }

    /**
     * perform the work
     */
    public void execute() throws BuildException {
        StringBuffer errorMessages = new StringBuffer();
        for(int i=0; i<=retryCount; i++) {
            try {
                nestedTask.perform();
                break;
            } catch (Exception e) {
                if (i<retryCount) {
                    log("Attempt ["+i+"] error occured, retrying...", e, Project.MSG_INFO);
                    errorMessages.append(e.getMessage());
                    errorMessages.append(getProject().getProperty("line.separator"));
                } else {
                    errorMessages.append(e.getMessage());
                    StringBuffer exceptionMessage = new StringBuffer();
                    exceptionMessage.append("Task [").append(nestedTask.getTaskName());
                    exceptionMessage.append("] failed after [").append(retryCount);
                    exceptionMessage.append("] attempts, giving up.");
                    exceptionMessage.append(getProject().getProperty("line.separator"));
                    exceptionMessage.append("Error messages:").append(getProject().getProperty("line.separator"));
                    exceptionMessage.append(errorMessages);
                    throw new BuildException(exceptionMessage.toString(), getLocation());
                }
            }
        }
    }
}