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

package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildFileTest;

/**
 
 */
public class ParserSupportsTest extends BuildFileTest {

    public ParserSupportsTest(String name) {
        super(name);
    }

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/conditions/parsersupports.xml");
    }

    public void testEmpty() throws Exception {
        expectBuildExceptionContaining("testEmpty",
                ParserSupports.ERROR_NO_ATTRIBUTES,
                ParserSupports.ERROR_NO_ATTRIBUTES);
    }

    public void testBoth() throws Exception {
        expectBuildExceptionContaining("testBoth",
                ParserSupports.ERROR_BOTH_ATTRIBUTES,
                ParserSupports.ERROR_BOTH_ATTRIBUTES);
    }

    public void testNamespaces() throws Exception {
        executeTarget("testNamespaces");
    }

    public void testPropertyNoValue() throws Exception {
        expectBuildExceptionContaining("testPropertyNoValue",
                ParserSupports.ERROR_NO_VALUE,
                ParserSupports.ERROR_NO_VALUE);
    }

    public void testUnknownProperty() throws Exception {
        executeTarget("testUnknownProperty");
    }
    public void NotestPropertyInvalid() throws Exception {
        executeTarget("testPropertyInvalid");
    }
    public void NotestXercesProperty() throws Exception {
        executeTarget("testXercesProperty");
    }
}
