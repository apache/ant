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

/**
 */
public class GzipTest extends BuildFileTest {

    public GzipTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/gzip.xml");
    }

    public void test1() {
        expectBuildException("test1", "required argument missing");
    }

    public void test2() {
        expectBuildException("test2", "required argument missing");
    }

    public void test3() {
        expectBuildException("test3", "required argument missing");
    }

    public void test4() {
        expectBuildException("test4", "zipfile must not point to a directory");
    }

    public void testGZip(){
        executeTarget("realTest");
        String log = getLog();
        assertTrue("Expecting message starting with 'Building:' but got '"
            + log + "'", log.startsWith("Building:"));
        assertTrue("Expecting message ending with 'asf-logo.gif.gz' but got '"
            + log + "'", log.endsWith("asf-logo.gif.gz"));
    }

    public void testResource(){
        executeTarget("realTestWithResource");
    }

    public void testDateCheck(){
        executeTarget("testDateCheck");
        String log = getLog();
        assertTrue(
            "Expecting message ending with 'asf-logo.gif.gz is up to date.' but got '" + log + "'",
            log.endsWith("asf-logo.gif.gz is up to date."));
    }

    public void tearDown(){
        executeTarget("cleanup");
    }

}
