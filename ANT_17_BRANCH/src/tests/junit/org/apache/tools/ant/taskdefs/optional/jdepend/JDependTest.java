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

package org.apache.tools.ant.taskdefs.optional.jdepend;

import org.apache.tools.ant.BuildFileTest;

/**
 * Testcase for the JDepend optional task.
 *
 */
public class JDependTest extends BuildFileTest {
    public static final String RESULT_FILESET = "result";

    public JDependTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject(
            "src/etc/testcases/taskdefs/optional/jdepend/jdepend.xml");
    }

    /**
     * Test simple
     */
    public void testSimple() {
        expectOutputContaining(
            "simple", "Package: org.apache.tools.ant.util.facade");
    }

    /**
     * Test xml
     */
    public void testXml() {
        expectOutputContaining(
            "xml", "<DependsUpon>");
    }

    /**
     * Test fork
     * - forked output goes to log
     */
    public void testFork() {
        expectLogContaining(
            "fork", "Package: org.apache.tools.ant.util.facade");
    }

    /**
     * Test fork xml
     */
    public void testForkXml() {
        expectLogContaining(
            "fork-xml", "<DependsUpon>");
    }

    /**
     * Test timeout
     */
    public void testTimeout() {
        expectLogContaining(
            "fork-timeout", "JDepend FAILED - Timed out");
    }


    /**
     * Test timeout without timing out
     */
    public void testTimeoutNot() {
        expectLogContaining(
            "fork-timeout-not", "Package: org.apache.tools.ant.util.facade");
    }

    /**
     * Assert that the given message has been outputted
     */
    protected void expectOutputContaining(String target, String substring) {
        executeTarget(target);
        assertOutputContaining(substring);
    }

}
