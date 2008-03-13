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

package org.apache.tools.ant;

import org.apache.tools.ant.BuildFileTest;

/**
 * Simple tests of build file processing
 */
public class CaseTest extends BuildFileTest {

    public CaseTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/core/case.xml");
    }

    /**
     * Test whether the build file treats nested elements without
     * regard to case. This should not cause an exception.
     */
    public void testCaseSensitivity() {
        executeTarget("case-sensitivity");
    }

    /**
     * Test whether the build file uses case when determining
     * task names.
     */
    public void testTaskCase() {
        expectBuildExceptionContaining("taskcase",
            "Task names are case sensitive",
            "Problem: failed to create task or type ecHO");
    }
}

