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
package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.JUnitLauncherTask;

import java.util.Optional;
import java.util.Properties;

/**
 * A {@link TestExecutionContext} represents the execution context for a test
 * that has been launched by the {@link JUnitLauncherTask} and provides any necessary
 * contextual information about such tests.
 */
public interface TestExecutionContext {

    /**
     * @return Returns the properties that were used for the execution of the test
     */
    Properties getProperties();


    /**
     * @return Returns the {@link Project} in whose context the test is being executed.
     * The {@code Project} is sometimes not available, like in the case where
     * the test is being run in a forked mode, in such cases this method returns
     * {@link Optional#empty() an empty value}
     */
    Optional<Project> getProject();
}
