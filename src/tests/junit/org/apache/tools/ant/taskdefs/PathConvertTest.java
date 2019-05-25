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

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for the &lt;pathconvert&gt; task.
 */
public class PathConvertTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    private static final String BUILD_FILENAME = "pathconvert.xml";

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/" + BUILD_FILENAME);
    }

    @Test
    public void testMap() {
        test("testmap");
    }

    @Test
    public void testMapper() {
        test("testmapper");
    }

    @Test
    public void testNoTargetOs() {
        buildRule.executeTarget("testnotargetos");
    }

    /**
     * Tests that if a {@code mappedresource}, that excludes certain resources, is used in a {@code pathconvert},
     * then it doesn't lead to a {@link NullPointerException}.
     *
     * @see <a href="https://bz.apache.org/bugzilla/show_bug.cgi?id=62076">bz-62076</a> for more details
     */
    @Test
    public void testNonMatchingMapper() {
        buildRule.executeTarget("test-nonmatching-mapper");
    }

    private void test(String target) {
        buildRule.executeTarget(target);
        assertEquals("test#" + BUILD_FILENAME, buildRule.getProject().getProperty("result"));
    }

}
