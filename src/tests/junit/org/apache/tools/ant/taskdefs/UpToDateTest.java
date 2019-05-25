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
import org.apache.tools.ant.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

public class UpToDateTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/uptodate.xml");
        buildRule.executeTarget("setUp");
        File srcDir = buildRule.getProject().resolveFile("source");
        assumeTrue("Could not change modification timestamp of source directory",
                srcDir.setLastModified(srcDir.lastModified()
                - 3 * FileUtils.getFileUtils().getFileTimestampGranularity()));
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("tearDown");
    }

    @Test
    public void testFilesetUpToDate() {
        buildRule.executeTarget("testFilesetUpToDate");
        assertEquals("true", buildRule.getProject().getProperty("foo"));
    }

    @Test
    public void testFilesetOutOfDate() {
        buildRule.executeTarget("testFilesetOutOfDate");
        assertNull(buildRule.getProject().getProperty("foo"));
    }

    @Test
    public void testRCUpToDate() {
        buildRule.executeTarget("testRCUpToDate");
        assertEquals("true", buildRule.getProject().getProperty("foo"));
    }

    @Test
    public void testRCOutOfDate() {
        buildRule.executeTarget("testRCOutOfDate");
        assertNull(buildRule.getProject().getProperty("foo"));
    }
}
