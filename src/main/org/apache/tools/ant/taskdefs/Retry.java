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

import java.util.Enumeration;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;

/**
 * Retries the nested task a set number of times
 */
public class Retry extends Task implements TaskContainer {

    private Task nestedTask;
    
    private int retryCount;
    
    public void addTask(Task t) {
        nestedTask = t;
    }
    
    public void setRetryCount(int n) {
        retryCount = n;
    }
    
    public void execute() throws BuildException {
        for(int i=0; i<=retryCount; i++) {
            try {
                nestedTask.perform();
            } catch (Exception e) {
                if (i<retryCount) {
                    log("Attempt ["+i+"] error occured, retrying...", e, Project.MSG_INFO);
                } else {
                    throw new BuildException("Task ["+nestedTask.getTaskName()+"] failed after ["+retryCount+"] attempts, giving up");
                }
            }
        }
    }
}