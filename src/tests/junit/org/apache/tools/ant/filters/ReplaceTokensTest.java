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

package org.apache.tools.ant.filters;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReplaceTokensTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/filters/build.xml");
    }

    @Test
    public void testReplaceTokens() throws IOException {
        buildRule.executeTarget("testReplaceTokens");
        File expected = buildRule.getProject().resolveFile("expected/replacetokens.test");
        File result = new File(buildRule.getProject().getProperty("output"), "replacetokens.test");
        assertEquals(FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testReplaceTokensPropertyFile() throws IOException {
        buildRule.executeTarget("testReplaceTokensPropertyFile");
        File expected = buildRule.getProject().resolveFile("expected/replacetokens.test");
        File result = new File(buildRule.getProject().getProperty("output"), "replacetokensPropertyFile.test");
        assertEquals(FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testReplaceTokensDoubleEncoded() throws IOException {
        buildRule.executeTarget("testReplaceTokensDoubleEncoded");
        File expected = buildRule.getProject().resolveFile("expected/replacetokens.double.test");
        File result = new File(buildRule.getProject().getProperty("output"), "replacetokens.double.test");
        assertEquals(FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testReplaceTokensDoubleEncodedToSimple() throws IOException {
        buildRule.executeTarget("testReplaceTokensDoubleEncodedToSimple");
        File expected = buildRule.getProject().resolveFile("expected/replacetokens.test");
        File result = new File(buildRule.getProject().getProperty("output"), "replacetokens.double.test");
        assertEquals(FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testReplaceTokensMustacheStyle() throws IOException {
        buildRule.executeTarget("testReplaceTokensMustacheStyle");
        File expected = buildRule.getProject().resolveFile("expected/replacetokens.test");
        File result = new File(buildRule.getProject().getProperty("output"), "replacetokens.mustache.test");
        assertEquals(FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }
}
