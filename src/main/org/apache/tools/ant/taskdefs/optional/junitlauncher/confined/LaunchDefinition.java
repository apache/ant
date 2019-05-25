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

package org.apache.tools.ant.taskdefs.optional.junitlauncher.confined;

import java.util.List;

/**
 * Defines the necessary context for launching the JUnit platform for running
 * tests.
 */
public interface LaunchDefinition {

    /**
     * @return Returns the {@link TestDefinition tests} that have to be launched
     */
    List<TestDefinition> getTests();

    /**
     * @return Returns the default {@link ListenerDefinition listeners} that will be used
     * for the tests, if the {@link #getTests() tests} themselves don't specify any
     */
    List<ListenerDefinition> getListeners();

    /**
     * @return Returns true if a summary needs to be printed out after the execution of the
     * tests. False otherwise.
     */
    boolean isPrintSummary();

    /**
     * @return Returns true if any remaining tests launch need to be stopped if any test execution
     * failed. False otherwise.
     */
    boolean isHaltOnFailure();

    /**
     * @return Returns the {@link ClassLoader} that has to be used for launching and execution of the
     * tests
     */
    ClassLoader getClassLoader();

    /**
     * @return Returns the list of tags which will be used to evaluate tests that need to be included
     * in the test execution
     */
    List<String> getIncludeTags();

    /**
     * @return Returns the list of tags which will be used to evaluate tests that need to be excluded
     * from the test execution
     */
    List<String> getExcludeTags();
}
