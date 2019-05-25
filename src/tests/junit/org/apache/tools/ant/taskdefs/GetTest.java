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

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 */
public class GetTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/get.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
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
     * Fail due to invalid src argument
     */
    @Test(expected = BuildException.class)
    public void test4() {
        buildRule.executeTarget("test4");
        // TODO assert value
    }

    /**
     * Fail due to invalid dest argument or no HTTP server on localhost
     */
    @Test(expected = BuildException.class)
    public void test5() {
        buildRule.executeTarget("test5");
        // TODO assert value
    }

    @Test
    public void test6() {
        buildRule.executeTarget("test6");
    }

    /**
     * Fail due to null or empty userAgent argument
     */
    @Test
    public void test7() {
        thrown.expect(BuildException.class);
        try {
            buildRule.executeTarget("test7");
        } finally {
            // post-mortem
            assertThat(buildRule.getLog(), not(containsString("Adding header")));
        }
    }

    @Test
    public void testUseTimestamp() {
        buildRule.executeTarget("testUseTimestamp");
    }

    @Test
    public void testUseTomorrow() {
        buildRule.executeTarget("testUseTomorrow");
    }

    @Test
    public void testTwoHeadersAreAddedOK() {
        buildRule.executeTarget("testTwoHeadersAreAddedOK");
        assertThat(buildRule.getLog(), both(containsString("Adding header 'header1'"))
                        .and(containsString("Adding header 'header2'")));
    }

    @Test
    public void testEmptyHeadersAreNeverAdded() {
        buildRule.executeTarget("testEmptyHeadersAreNeverAdded");
        assertThat(buildRule.getLog(), not(containsString("Adding header")));
    }

    @Test
    public void testThatWhenMoreThanOneHeaderHaveSameNameOnlyLastOneIsAdded() {
        buildRule.executeTarget("testThatWhenMoreThanOneHeaderHaveSameNameOnlyLastOneIsAdded");
        String log = buildRule.getLog();
        assertThat(log, containsString("Adding header 'header1'"));

        int actualHeaderCount = log.split("Adding header ").length - 1;

        assertEquals("Only one header has been added", 1, actualHeaderCount);
    }

    @Test
    public void testHeaderSpaceTrimmed() {
        buildRule.executeTarget("testHeaderSpaceTrimmed");
        assertThat(buildRule.getLog(), containsString("Adding header 'header1'"));
    }

}
