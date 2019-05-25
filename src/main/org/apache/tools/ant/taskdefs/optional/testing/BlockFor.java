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
package org.apache.tools.ant.taskdefs.optional.testing;

import org.apache.tools.ant.taskdefs.WaitFor;

/**
 * @since Ant 1.8
 */

public class BlockFor extends WaitFor {

    /**
     * Text to include in a message
     */
    private String text;


    /**
     * Constructor that takes the name of the task in the task name.
     *
     */
    public BlockFor() {
        super("blockfor");
        text = getTaskName() + " timed out";
    }

    /**
     * Constructor that takes the name of the task in the task name.
     *
     * @param taskName the name of the task.
     */
    public BlockFor(String taskName) {
        super(taskName);
    }

    /**
     * If the wait fails, a BuildException is thrown. All the superclasses actions are called first.
     * @throws BuildTimeoutException on timeout, using the text in {@link #text}
     *
     */
    @Override
    protected void processTimeout() throws BuildTimeoutException {
        super.processTimeout();
        throw new BuildTimeoutException(text, getLocation());
    }

    /**
     * Set the error text; all properties are expanded in the message.
     *
     * @param message the text to use in a failure message
     */
    public void addText(String message) {
        text = getProject().replaceProperties(message);
    }

}
