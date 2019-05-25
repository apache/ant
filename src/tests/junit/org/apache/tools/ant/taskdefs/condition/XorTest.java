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
package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that Xor follows the conventional boolean logic semantics
 * (a ^ b) === (a||b)&!(a&&b)
 */
public class XorTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();


    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/conditions/xor.xml");
    }

    @Test
    public void testEmpty() {
        buildRule.executeTarget("testEmpty");
    }

    @Test
    public void test0() {
        buildRule.executeTarget("test0");
    }

    @Test
    public void test1() {
        buildRule.executeTarget("test1");
    }

    @Test
    public void test00() {
        buildRule.executeTarget("test00");
    }

    @Test
    public void test10() {
        buildRule.executeTarget("test10");
    }

    @Test
    public void test01() {
        buildRule.executeTarget("test01");
    }

    @Test
    public void test11() {
        buildRule.executeTarget("test11");
    }

}
