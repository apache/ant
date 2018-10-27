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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 */
public class GzipTest {
    
    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();


    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/gzip.xml");
    }

    @Test
    public void test1() {
        try {
			buildRule.executeTarget("test1");
			fail("BuildException expected: required argument missing");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void test2() {
        try {
			buildRule.executeTarget("test2");
			fail("BuildException expected: required argument missing");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void test3() {
        try {
			buildRule.executeTarget("test3");
			fail("BuildException expected: required argument missing");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void test4() {
        try {
			buildRule.executeTarget("test4");
			fail("BuildException expected: zipfile must not point to a directory");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void testGZip(){
        buildRule.executeTarget("realTest");
        String log = buildRule.getLog();
        assertTrue("Expecting message starting with 'Building:' but got '"
            + log + "'", log.startsWith("Building:"));
        assertTrue("Expecting message ending with 'asf-logo.gif.gz' but got '"
            + log + "'", log.endsWith("asf-logo.gif.gz"));
    }

    @Test
    public void testResource(){
        buildRule.executeTarget("realTestWithResource");
    }

    @Test
    public void testDateCheck(){
        buildRule.executeTarget("testDateCheck");
        String log = buildRule.getLog();
        assertTrue(
            "Expecting message ending with 'asf-logo.gif.gz is up to date.' but got '" + log + "'",
            log.endsWith("asf-logo.gif.gz is up to date."));
    }

    @After
    public void tearDown(){
        buildRule.executeTarget("cleanup");
    }

}
