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
package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildFileTest;

/**
 * Tests the Dotnet tasks, based off WsdlToDotnetTest
 *
 * @since Ant 1.6
 */
public class DotnetTest extends BuildFileTest {

    /**
     * Description of the Field
     */
    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";


    /**
     * Constructor
     *
     * @param name testname
     */
    public DotnetTest(String name) {
        super(name);
    }


    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject(TASKDEFS_DIR + "dotnet.xml");
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
    public void testCSC() throws Exception {
        executeTarget("testCSC");
    }


    /**
     * A unit test for JUnit
     */
    public void testCSCintrinsicFileset() throws Exception {
        executeTarget("testCSCintrinsicFileset");
    }


    /**
     * A unit test for JUnit
     */
    public void testCSCdll() throws Exception {
        executeTarget("testCSCdll");
    }

    /**
     * A unit test for JUnit
     */
    public void testCscReferences() throws Exception {
        executeTarget("testCscReferences");
    }

    /**
     * A unit test for JUnit
     */
    public void testCscResources() throws Exception {
        executeTarget("testCSCResources");
    }

    /**
     * test we can assemble
     */
    public void testILASM() throws Exception {
        executeTarget("testILASM");
    }

    /**
     * test we can disassemble
     */
    public void testILDASM() throws Exception {
        executeTarget("testILDASM");
    }

    /**
     * test we can disassemble
     */
    public void testILDASM_empty() throws Exception {
        expectBuildExceptionContaining("testILDASM_empty",
                "parameter validation",
                "invalid");
    }

    /**
     * test we can handle jsharp (if found)
     */
    public void testJsharp() throws Exception {
        executeTarget("jsharp");
    }

    /**
     * test we can handle jsharp (if found)
     */
    public void testResponseFile() throws Exception {
        executeTarget("testCSCresponseFile");
    }

}

