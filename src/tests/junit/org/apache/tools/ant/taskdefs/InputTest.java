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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.input.PropertyFileInputHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class InputTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    private InputStream originalStdIn;

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/input.xml");
        System.getProperties().put(PropertyFileInputHandler.FILE_NAME_KEY,
                buildRule.getProject().resolveFile("input.properties").getAbsolutePath());
        buildRule.getProject().setInputHandler(new PropertyFileInputHandler());
        originalStdIn = System.in;
    }

    @After
    public void tearDown() {
        System.setIn(originalStdIn);
    }

    @Test
    public void test1() {
        buildRule.executeTarget("test1");
    }

    @Test
    public void test2() {
        buildRule.executeTarget("test2");
    }

    @Test
    public void test3() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Found invalid input test for 'All data is going to be deleted from DB continue?'");
        buildRule.executeTarget("test3");
    }

    @Test
    public void test5() {
        buildRule.executeTarget("test5");
    }

    @Test
    public void test6() {
        buildRule.executeTarget("test6");
        assertEquals("scott", buildRule.getProject().getProperty("db.user"));
    }

    @Test
    public void testPropertyFileInlineHandler() {
        buildRule.executeTarget("testPropertyFileInlineHandler");
    }

    @Test
    public void testDefaultInlineHandler() throws IOException {
        stdin();
        buildRule.executeTarget("testDefaultInlineHandler");
    }

    @Test
    public void testGreedyInlineHandler() throws IOException {
        stdin();
        buildRule.executeTarget("testGreedyInlineHandler");
    }

    @Test
    public void testGreedyInlineHandlerClassname() throws IOException {
        stdin();
        buildRule.executeTarget("testGreedyInlineHandlerClassname");
    }

    @Test
    public void testGreedyInlineHandlerRefid() throws IOException {
        stdin();
        buildRule.executeTarget("testGreedyInlineHandlerRefid");
    }

    private void stdin() throws IOException {
        System.setIn(new FileInputStream(buildRule.getProject().resolveFile("input.stdin")));
    }

}
