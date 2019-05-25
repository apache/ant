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

package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PvcsTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/pvcs.xml");
    }

    /**
     * Expected failure due to missing argument "repository"
     */
    @Test(expected = BuildException.class)
    public void test1() {
            buildRule.executeTarget("test1");
    }

    @Test
    public void test2() {
        buildRule.executeTarget("test2");
    }

    @Test
    public void test3() {
        buildRule.executeTarget("test3");
    }

    @Test
    public void test4() {
        buildRule.executeTarget("test4");
    }

    @Test
    public void test5() {
        buildRule.executeTarget("test5");
    }

    /**
     * Expected failure due to nonexistent directory structure
     */
    @Test(expected = BuildException.class)
    public void test6() {
        buildRule.executeTarget("test6");
        //TODO assert exception message
    }
}
