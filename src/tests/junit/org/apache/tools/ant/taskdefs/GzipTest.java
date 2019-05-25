/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 */
public class GzipTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/gzip.xml");
    }

    /**
     * Fail due to missing required argument
     */
    @Test(expected = BuildException.class)
    public void test1() {
        buildRule.executeTarget("test1");
        // TODO assert value
    }

    /**
     * Fail due to missing required argument
     */
    @Test(expected = BuildException.class)
    public void test2() {
        buildRule.executeTarget("test2");
        // TODO assert value
    }

    /**
     * Fail due to missing required argument
     */
    @Test(expected = BuildException.class)
    public void test3() {
        buildRule.executeTarget("test3");
        // TODO assert value
    }

    /**
     * Fail due to zipfile pointing to a directory
     */
    @Test(expected = BuildException.class)
    public void test4() {
        buildRule.executeTarget("test4");
        // TODO assert value
    }

    @Test
    public void testGZip() {
        buildRule.executeTarget("realTest");
        String log = buildRule.getLog();
        assertThat("Expecting message starting with 'Building:' but got '"
                + log + "'", log, startsWith("Building:"));
        assertThat("Expecting message ending with 'asf-logo.gif.gz' but got '"
                + log + "'", log, endsWith("asf-logo.gif.gz"));
    }

    @Test
    public void testResource() {
        buildRule.executeTarget("realTestWithResource");
    }

    @Test
    public void testDateCheck() {
        buildRule.executeTarget("testDateCheck");
        String log = buildRule.getLog();
        assertThat("Expecting message ending with 'asf-logo.gif.gz is up to date.' but got '"
                + log + "'", log, endsWith("asf-logo.gif.gz is up to date."));
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

}
