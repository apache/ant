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


package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.taskdefs.rmic.RmicAdapterFactory;

/**
 * Date: 04-Aug-2004
 * Time: 22:15:46
 */
public class RmicAdvancedTest extends BuildFileTest {

    public RmicAdvancedTest(String name) {
        super(name);
    }

    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/rmic/";

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject(TASKDEFS_DIR + "rmic.xml");
    }

    /**
     * The teardown method for JUnit
     */
    public void tearDown() {
        executeTarget("teardown");
    }

    /**
     * A unit test for JUnit
     */
    public void testRmic() throws Exception {
        executeTarget("testRmic");
    }
    /**
     * A unit test for JUnit
     */
    public void testKaffe() throws Exception {
        executeTarget("testKaffe");
    }

    /**
     * A unit test for JUnit
     */
    public void testWlrmic() throws Exception {
        executeTarget("testWlrmic");
    }

    /**
     * A unit test for JUnit
     */
    public void testForking() throws Exception {
        executeTarget("testForking");
    }

    /**
     * A unit test for JUnit
     */
    public void testBadName() throws Exception {
        expectBuildExceptionContaining("testBadName",
                "compiler not known",
                RmicAdapterFactory.ERROR_UNKNOWN_COMPILER);
    }

    /**
     * A unit test for JUnit
     */
    public void testWrongClass() throws Exception {
        expectBuildExceptionContaining("testWrongClass",
                "class not an RMIC adapter",
                RmicAdapterFactory.ERROR_NOT_RMIC_ADAPTER);
    }
}

