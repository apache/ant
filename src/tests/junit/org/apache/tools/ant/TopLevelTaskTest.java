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

// This test will fail with embed, or if top-level is moved out of
// dependency - as 'echo' happens as part of configureProject stage.

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for builds with tasks at the top level
 *
 * @since Ant 1.6
 */
public class TopLevelTaskTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Test
    public void testNoTarget() {
        buildRule.configureProject("src/etc/testcases/core/topleveltasks/notarget.xml");
        buildRule.executeTarget("");
        assertEquals("Called", buildRule.getLog());
    }

    @Test
    public void testCalledFromTopLevelAnt() {
        buildRule.configureProject("src/etc/testcases/core/topleveltasks/toplevelant.xml");
        buildRule.executeTarget("");
        assertEquals("Called", buildRule.getLog());
    }

    @Test
    public void testCalledFromTargetLevelAnt() {
        buildRule.configureProject("src/etc/testcases/core/topleveltasks/targetlevelant.xml");
        buildRule.executeTarget("foo");
        assertEquals("Called", buildRule.getLog());
    }
}
