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
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @since Ant 1.6
 */
public class AntLikeTasksAtTopLevelTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Test
    public void testAnt() {
        try {
            buildRule.configureProject("src/etc/testcases/taskdefs/toplevelant.xml");
            fail("no exception thrown");
        } catch (BuildException e) {
            assertEquals("ant task at the top level must not invoke its own"
                         + " build file.", e.getMessage());
        }
    }

    @Test
    public void testSubant() {
        try {
            buildRule.configureProject("src/etc/testcases/taskdefs/toplevelsubant.xml");
            fail("no exception thrown");
        } catch (BuildException e) {
            assertEquals("subant task at the top level must not invoke its own"
                         + " build file.", e.getMessage());
        }
    }

    @Test
    public void testAntcall() {
        try {
            buildRule.configureProject("src/etc/testcases/taskdefs/toplevelantcall.xml");
            fail("no exception thrown");
        } catch (BuildException e) {
            assertEquals("antcall must not be used at the top level.",
                         e.getMessage());
        }
    }

}// AntLikeTasksAtTopLevelTest
