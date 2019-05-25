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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 */
public class DeleteTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/delete.xml");
    }

    /**
     * Expected failure due to required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test1() {
        buildRule.executeTarget("test1");
        // TODO Assert exception message
    }

    @Test
    public void test2() {
        buildRule.executeTarget("test2");
    }

    //where oh where has my test case 3 gone?
    @Test
    public void test4() {
        buildRule.executeTarget("test4");
    }

    @Test
    public void test5() {
        buildRule.executeTarget("test5");
    }

    @Test
    public void test6() {
        buildRule.executeTarget("test6");
    }

    @Test
    public void test7() {
        buildRule.executeTarget("test7");
    }

    @Test
    public void test8() {
        buildRule.executeTarget("test8");
    }

    @Test
    public void test9() {
        buildRule.executeTarget("test9");
    }

    @Test
    public void test10() {
        buildRule.executeTarget("test10");
    }

    @Test
    public void test11() {
        buildRule.executeTarget("test11");
    }

    @Test
    public void test12() {
        buildRule.executeTarget("test12");
    }

    @Test
    public void test13() {
        buildRule.executeTarget("test13");
    }

    @Test
    public void test14() {
        buildRule.executeTarget("test14");
    }

    @Test
    public void test15() {
        buildRule.executeTarget("test15");
    }

    @Test
    public void test16() {
        buildRule.executeTarget("test16");
    }

    @Test
    public void test17() {
        buildRule.executeTarget("test17");
    }
}
