/*
 * Copyright  2002,2004 Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildFileTest;

/**
 * Tests the style/xslt task.
 *
 * @author steve loughran
 * @since Ant 1.5
 */
public class XsltTest extends BuildFileTest {

    /**
     * where tasks run
     */
    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";


    /**
     * Constructor
     *
     * @param name testname
     */
    public XsltTest(String name) {
        super(name);
    }


    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject(TASKDEFS_DIR + "xslt.xml");
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
    public void testCatchNoDtd() throws Exception {
        expectBuildExceptionContaining("testCatchNoDtd",
                                       "expected failure",
                                       "Fatal error during transformation");
    }

    /**
     * A unit test for JUnit
     */
    public void testCatalog() throws Exception {
         executeTarget("testCatalog");
    }

    public void testOutputProperty() throws Exception {
      executeTarget("testOutputProperty");
    }

    public void testFactory() throws Exception {
        executeTarget("testFactory");
    }

    public void testAttribute() throws Exception {
        executeTarget("testAttribute");
    }
}

