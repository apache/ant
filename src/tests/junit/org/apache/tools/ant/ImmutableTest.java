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

package org.apache.tools.ant;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 */
public class ImmutableTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/core/immutable.xml");
    }

    // override allowed on <available>
    @Test
    public void test1() {
        buildRule.executeTarget("test1");
        assertEquals("override", buildRule.getProject().getProperty("test"));
    }

    // ensure <tstamp>'s new prefix attribute is working
    @Test
    public void test2() {
        buildRule.executeTarget("test2");
        assertNotNull(buildRule.getProject().getProperty("DSTAMP"));
        assertNotNull(buildRule.getProject().getProperty("start.DSTAMP"));
    }

    // ensure <tstamp> follows the immutability rule
    @Test
    public void test3() {
        buildRule.executeTarget("test3");
        assertEquals("original", buildRule.getProject().getProperty("DSTAMP"));
    }

    // ensure <condition> follows the immutability rule
    @Test
    public void test4() {
        buildRule.executeTarget("test4");
        assertEquals("original", buildRule.getProject().getProperty("test"));
    }

    // ensure <checksum> follows the immutability rule
    @Test
    public void test5() {
        buildRule.executeTarget("test5");
        assertEquals("original", buildRule.getProject().getProperty("test"));
    }

    // ensure <exec> follows the immutability rule
    @Test
    public void test6() {
        buildRule.executeTarget("test6");
        assertEquals("original", buildRule.getProject().getProperty("test1"));
        assertEquals("original", buildRule.getProject().getProperty("test2"));
    }

    // ensure <pathconvert> follows the immutability rule
    @Test
    public void test7() {
        buildRule.executeTarget("test7");
        assertEquals("original", buildRule.getProject().getProperty("test"));
    }
}

