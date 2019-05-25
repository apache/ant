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

import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.property.LocalProperties;

/**
 * Sequential is a container task - it can contain other Ant tasks. The nested
 * tasks are simply executed in sequence. Sequential's primary use is to support
 * the sequential execution of a subset of tasks within the {@link Parallel Parallel Task}

 * <p>
 * The sequential task has no attributes and does not support any nested
 * elements apart from Ant tasks. Any valid Ant task may be embedded within the
 * sequential task.</p>
 *
 * @since Ant 1.4
 * @ant.task category="control"
 */
public class Sequential extends Task implements TaskContainer {

    /** Optional Vector holding the nested tasks */
    private List<Task> nestedTasks = new Vector<>();

    /**
     * Add a nested task to Sequential.
     *
     * @param nestedTask  Nested task to execute Sequential
     */
    @Override
    public void addTask(Task nestedTask) {
        nestedTasks.add(nestedTask);
    }

    /**
     * Execute all nestedTasks.
     *
     * @throws BuildException if one of the nested tasks fails.
     */
    @Override
    public void execute() throws BuildException {
        LocalProperties localProperties
            = LocalProperties.get(getProject());
        localProperties.enterScope();
        try {
            nestedTasks.forEach(Task::perform);
        } finally {
            localProperties.exitScope();
        }
    }
}
