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

package org.apache.tools.ant.filters;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class LineContainsTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/filters/build.xml");
    }

    @Test
    public void testLineContains() throws IOException {
        buildRule.executeTarget("testLineContains");
        File expected = buildRule.getProject().resolveFile("expected/linecontains.test");
        File result = new File(buildRule.getProject().getProperty("output"),"linecontains.test");
        assertEquals(FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testNegateLineContains() {
        buildRule.executeTarget("testNegateLineContains");
    }

    /**
     * Tests that the {@code matchAny} attribute of {@link LineContains} works as expected
     *
     * @throws IOException
     */
    @Test
    public void testLineContainsMatchAny() throws IOException {
        buildRule.executeTarget("testMatchAny");
        File expected = buildRule.getProject().resolveFile("expected/linecontains-matchany.test");
        File result = new File(buildRule.getProject().getProperty("output"), "linecontains.test");
        assertEquals(FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    /**
     * Tests that the {@code matchAny} attribute when used with the {@code negate} attribute
     * of {@link LineContains} works as expected
     *
     * @throws IOException
     */
    @Test
    public void testLineContainsMatchAnyNegate() throws IOException {
        buildRule.executeTarget("testMatchAnyNegate");
        File expected = buildRule.getProject().resolveFile("expected/linecontains-matchany-negate.test");
        File result = new File(buildRule.getProject().getProperty("output"), "linecontains.test");
        assertEquals(FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }
}
