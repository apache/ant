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

import java.io.IOException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * A simple utility class to take a piece of code (that implements
 * <code>Retryable</code> interface) and executes that with possibility to
 * retry the execution in case of IOException.
 */
public class RetryHandler {

    private int retriesAllowed = 0;
    private Task task;

    /**
     * Create a new RetryingHandler.
     *
     * @param retriesAllowed how many times to retry
     * @param task the Ant task that is is executed from, used for logging only
     */
    public RetryHandler(int retriesAllowed, Task task) {
        this.retriesAllowed = retriesAllowed;
        this.task = task;
    }

    /**
     * Execute the <code>Retryable</code> code with specified number of retries.
     *
     * @param exe the code to execute
     * @param desc some descriptive text for this piece of code, used for logging
     * @throws IOException if the number of retries has exceeded the allowed limit
     */
    public void execute(Retryable exe, String desc) throws IOException {
        int retries = 0;
        while (true) {
            try {
                exe.execute();
                break;
            } catch (IOException e) {
                retries++;
                if (retries > this.retriesAllowed && this.retriesAllowed > -1) {
                    task.log("try #" + retries + ": IO error ("
                            + desc + "), number of maximum retries reached ("
                            + this.retriesAllowed + "), giving up", Project.MSG_WARN);
                    throw e;
                } else {
                    task.log("try #" + retries + ": IO error (" + desc
                             + "), retrying", Project.MSG_WARN);
                }
            }
        }
    }

}
