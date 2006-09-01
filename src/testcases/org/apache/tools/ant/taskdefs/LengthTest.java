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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileTest;

public class LengthTest extends BuildFileTest {

    public LengthTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/length.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testEach() {
        executeTarget("testEach");
    }

    public void testEachCondition() {
        executeTarget("testEachCondition");
    }

    public void testAll() {
        executeTarget("testAll");
    }

    public void testAllCondition() {
        executeTarget("testAllCondition");
    }

    public void testFile() {
        executeTarget("testFile");
    }

    public void testFileCondition() {
        executeTarget("testFileCondition");
    }

    public void testBoth() {
        executeTarget("testBoth");
    }

    public void testBothCondition() {
        executeTarget("testBothCondition");
    }

    public void testDupes() {
        executeTarget("testDupes");
    }

    public void testDupesCondition() {
        executeTarget("testDupesCondition");
    }

    public void testString() {
        executeTarget("testString");
    }

    public void testStringCondition() {
        executeTarget("testStringCondition");
    }

    public void testTrimString() {
        executeTarget("testTrimString");
    }

    public void testTrimStringCondition() {
        executeTarget("testTrimStringCondition");
    }

    public void testNoTrimString() {
        executeTarget("testNoTrimString");
    }

    public void testNoTrimStringCondition() {
        executeTarget("testNoTrimStringCondition");
    }

    public void testStringFile() {
        expectBuildExceptionContaining("testStringFile",
            "should fail", "incompatible");
    }

    public void testTrimFile() {
        expectBuildExceptionContaining("testTrimFile",
            "should fail", "string length function only");
    }

    public void testImmutable() {
        executeTarget("testImmutable");
    }

    public void testZipFileSet() {
        executeTarget("testZipFileSet");
    }

    public void testZipFileSetCondition() {
        executeTarget("testZipFileSetCondition");
    }

}
