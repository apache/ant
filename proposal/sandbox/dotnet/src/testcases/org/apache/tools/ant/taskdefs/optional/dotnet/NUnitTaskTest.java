/*
 * Copyright  2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.BuildFileTest;

/**
 * Tests the NUnitTask task.
 */
public class NUnitTaskTest extends BuildFileTest {

    /**
     * Description of the Field
     */
    private final static String TASKDEFS_DIR = "src/etc/testcases/";

    /**
     * Constructor 
     *
     * @param name testname
     */
    public NUnitTaskTest(String name) {
        super(name);
    }

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject(TASKDEFS_DIR + "nunit.xml");
    }

    /**
     * The teardown method for JUnit
     */
    public void tearDown() {
        executeTarget("teardown");
    }

    public void testNoAssembly() {
        expectSpecificBuildException("no-assembly", "no assembly", 
                                     "You must specify at least one test assembly.");
    }

    public void testPass() {
        if (getProject().getProperty("nunit.found") != null) {
            expectLogContaining("passing-test", 
                                "Tests run: 1, Failures: 0, Not run: 0");
        }
    }

    public void testFail() {
        if (getProject().getProperty("nunit.found") != null) {
            expectLogContaining("failing-test", 
                                "Tests run: 1, Failures: 1, Not run: 0");
        }
    }

    public void testFailOnFail() {
        if (getProject().getProperty("nunit.found") != null) {
            expectBuildException("failing-test-with-fail", "test should fail");
        }
    }

}
