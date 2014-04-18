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

package org.apache.tools.ant;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TaskContainerTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();
    
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/core/taskcontainer.xml");
    }

    @Test
    public void testPropertyExpansion() {
        buildRule.executeTarget("testPropertyExpansion");
        assertTrue("attribute worked",
                   buildRule.getLog().indexOf("As attribute: it worked") > -1);
        assertTrue("nested text worked",
                   buildRule.getLog().indexOf("As nested text: it worked") > -1);
    }

    @Test
    public void testTaskdef() {
        buildRule.executeTarget("testTaskdef");
        assertTrue("attribute worked",
                   buildRule.getLog().indexOf("As attribute: it worked") > -1);
        assertTrue("nested text worked",
                   buildRule.getLog().indexOf("As nested text: it worked") > -1);
        assertTrue("nested text worked",
                   buildRule.getLog().indexOf("As nested task: it worked") > -1);
    }

    @Test
    public void testCaseInsensitive() {
        buildRule.executeTarget("testCaseInsensitive");
        assertTrue("works outside of container",
                   buildRule.getLog().indexOf("hello ") > -1);
        assertTrue("works inside of container",
                   buildRule.getLog().indexOf("world") > -1);
    }

}
