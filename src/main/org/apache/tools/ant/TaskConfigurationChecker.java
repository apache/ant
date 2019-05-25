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

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Helper class for the check of the configuration of a given task.
 * This class provides methods for making assumptions about the task configuration.
 * After collecting all violations with <code>assert*</code> and <code>fail</code>
 * methods the <code>checkErrors</code> will throw a BuildException with all collected
 * messages or does nothing if there wasn't any error.</p>
 *
 * <p>Example:</p>
 *
 * <pre>
 *     public class MyTask extends Task {
 *         ...
 *         public void execute() {
 *             TaskConfigurationChecker checker = TaskConfigurationChecker(this);
 *             checker.assertConfig(
 *                 srcdir != null,
 *                 "Attribute 'srcdir' must be set.
 *             );
 *             checker.assertConfig(
 *                 srcdir.exists(),
 *                 "Srcdir (" + srcdir + ") must exist."
 *             );
 *             if (someComplexCondition()) {
 *                 fail("Complex condition failed.");
 *             }
 *             checker.checkErrors();
 *         }
 *     }
 * </pre>
 *
 * @see <a href="https://martinfowler.com/eaaDev/Notification.html">Notification Pattern</a>
 */
public class TaskConfigurationChecker {

    /** List of all collected error messages. */
    private List<String> errors = new ArrayList<>();

    /** Task for which the configuration should be checked. */
    private final Task task;

    /**
     * Constructor.
     * @param task which task should be checked
     */
    public TaskConfigurationChecker(Task task) {
        this.task = task;
    }

    /**
     * Asserts that a condition is true.
     * @param condition     which condition to check
     * @param errormessage  errormessage to throw if a condition failed
     */
    public void assertConfig(boolean condition, String errormessage) {
        if (!condition) {
            errors.add(errormessage);
        }
    }

    /**
     * Registers an error.
     * @param errormessage the message for the registered error
     */
    public void fail(String errormessage) {
        errors.add(errormessage);
    }

    /**
     * Checks if there are any collected errors and throws a BuildException
     * with all messages if there was one or more.
     * @throws BuildException if one or more errors were registered
     */
    public void checkErrors() throws BuildException {
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder(String.format("Configuration error on <%s>:%n",
                    task.getTaskName()));
            for (String msg : errors) {
                sb.append(String.format("- %s%n", msg));
            }
            throw new BuildException(sb.toString(), task.getLocation());
        }
    }

}
