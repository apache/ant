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
package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RedirectorElementTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/redirector.xml", Project.MSG_VERBOSE);
    }

    @Test
    public void test1() {
        buildRule.executeTarget("test1");
        assertTrue((buildRule.getProject().getReference("test1")
            instanceof RedirectorElement));
    }

    /**
     * Expected failure due to multiple attributes when using refid
     */
    @Test(expected = BuildException.class)
    public void test2() {
        buildRule.executeTarget("test2");
    }

    /**
     * Expected failure due to nested elements when using refid
     */
    @Test(expected = BuildException.class)
    public void test3() {
        buildRule.executeTarget("test3");
    }

    @Test
    public void test4() {
        buildRule.executeTarget("test4");
    }

    @Test
    public void testLogInputString() {
        buildRule.executeTarget("testLogInputString");
        if (buildRule.getLog().contains("testLogInputString can-cat")) {
            assertThat(buildRule.getFullLog(), containsString("Using input string"));
        }
    }

    @Test
    public void testRefid() {
        buildRule.executeTarget("testRefid");
    }

}
