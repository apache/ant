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
 * Tests the NAntTask task.
 */
public class NAntTaskTest extends BuildFileTest {

    /**
     * Description of the Field
     */
    private final static String TASKDEFS_DIR = "src/etc/testcases/";

    /**
     * Constructor 
     *
     * @param name testname
     */
    public NAntTaskTest(String name) {
        super(name);
    }

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject(TASKDEFS_DIR + "nant.xml");
    }

    public void testEcho() throws Exception {
        if (getProject().getProperty("nant.found") != null) {
            expectLogContaining("echo", "foo is bar");
        }
    }

    public void testNestedFile() throws Exception {
        if (getProject().getProperty("nant.found") != null) {
            expectLogContaining("nested-file", "foo is bar");
        }
    }

    public void testNestedTask() throws Exception {
        if (getProject().getProperty("nant.found") != null) {
            expectLogContaining("nested-task", "foo is bar");
        }
    }
}
