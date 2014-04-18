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

package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.AntAssert;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Testcase for the &lt;isfileselected&gt; condition.
 *
 */
public class IsFileSelectedTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/conditions/isfileselected.xml");
    }

    @Test
    public void testSimple() {
        buildRule.executeTarget("simple");
    }

    @Test
    public void testName() {
        buildRule.executeTarget("name");
    }

    @Test
    public void testBaseDir() {
        buildRule.executeTarget("basedir");
    }

    @Test
    public void testType() {
        buildRule.executeTarget("type");
    }

    @Test
    public void testNotSelector() {
        try {
            buildRule.executeTarget("not.selector");
            fail("Exception should have been thrown: checking for use as a selector (not allowed)");
        } catch(BuildException ex) {
            AntAssert.assertContains("fileset doesn't support the nested \"isfile",
                    ex.getMessage());
        }
    }
}
