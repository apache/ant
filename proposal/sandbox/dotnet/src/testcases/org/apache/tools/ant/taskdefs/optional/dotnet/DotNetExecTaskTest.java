/*
 * Copyright  2003-2004 The Apache Software Foundation
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
 * Tests the DotNetExecTask task, based off Ant's DotnetTest.
 *
 */
public class DotNetExecTaskTest extends BuildFileTest {

    /**
     * Description of the Field
     */
    private final static String TASKDEFS_DIR = "src/etc/testcases/";


    /**
     * Constructor 
     *
     * @param name testname
     */
    public DotNetExecTaskTest(String name) {
        super(name);
    }


    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject(TASKDEFS_DIR + "dotnetexec.xml");
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
}
