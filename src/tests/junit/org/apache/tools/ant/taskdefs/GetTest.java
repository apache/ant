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

import static org.junit.Assert.fail;

/**
 */
public class GetTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/get.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    @Test
    public void test1() {
        try {
            buildRule.executeTarget("test1");
            fail("required argument missing");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void test2() {
        try {
            buildRule.executeTarget("test2");
            fail("required argument missing");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void test3() {
        try {
            buildRule.executeTarget("test3");
            fail("required argument missing");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void test4() {
        try {
            buildRule.executeTarget("test4");
            fail("src invalid");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void test5() {
        try {
            buildRule.executeTarget("test5");
            fail("dest invalid (or no http-server on local machine");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void test6() {
        buildRule.executeTarget("test6");
    }

    @Test
    public void test7() {
        try {
            buildRule.executeTarget("test7");
            fail("userAgent may not be null or empty");
        } catch (BuildException ex) {
            //TODO assert value
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

}
