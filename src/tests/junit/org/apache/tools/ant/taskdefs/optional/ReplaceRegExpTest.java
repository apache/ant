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
package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

/**
 * JUnit Testcase for the optional replaceregexp task.
 *
 */
public class ReplaceRegExpTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/replaceregexp.xml");
    }

    @Test
    public void testReplace() throws IOException {
        Properties original = new Properties();
        try (FileInputStream propsFile = new FileInputStream(new File(
                buildRule.getProject().getBaseDir() + "/replaceregexp.properties"))) {
            original.load(propsFile);
        }

        assertEquals("Def", original.get("OldAbc"));

        buildRule.executeTarget("testReplace");

        Properties after = new Properties();
        try (FileInputStream propsFile = new FileInputStream(new File(buildRule.getOutputDir(),
                "test.properties"))) {
            after.load(propsFile);
        }

        assertNull(after.get("OldAbc"));
        assertEquals("AbcDef", after.get("NewProp"));
    }

    // inspired by bug 22541
    @Test
    public void testDirectoryDateDoesNotChange() {
        buildRule.executeTarget("touchDirectory");
        File myFile = buildRule.getOutputDir();
        long timeStampBefore = myFile.lastModified();
        buildRule.executeTarget("testDirectoryDateDoesNotChange");
        long timeStampAfter = myFile.lastModified();
        assertEquals("directory date should not change",
            timeStampBefore, timeStampAfter);
    }

    @Test
    public void testDontAddNewline1() throws IOException {
        buildRule.executeTarget("testDontAddNewline1");
        assertEquals(FileUtilities.getFileContents(new File(buildRule.getOutputDir(), "test.properties")),
                FileUtilities.getFileContents(new File(buildRule.getProject().getBaseDir(), "replaceregexp2.result.properties")));
    }

    @Test
    public void testDontAddNewline2() throws IOException {
        buildRule.executeTarget("testDontAddNewline2");
        assertEquals(FileUtilities.getFileContents(new File(buildRule.getOutputDir(), "test.properties")),
                FileUtilities.getFileContents(new File(buildRule.getProject().getBaseDir(), "replaceregexp2.result.properties")));
    }

    @Test
    public void testNoPreserveLastModified() {
        buildRule.executeTarget("lastModifiedSetup");
        File testFile = new File(buildRule.getOutputDir(), "test.txt");
        assumeTrue(testFile.setLastModified(testFile.lastModified()
                - FileUtils.getFileUtils().getFileTimestampGranularity() * 3));
        long ts1 = testFile.lastModified();
        buildRule.executeTarget("testNoPreserve");
        assertTrue(ts1 < testFile.lastModified());
    }

    @Test
    public void testPreserveLastModified() {
        buildRule.executeTarget("lastModifiedSetup");
        File testFile = new File(buildRule.getOutputDir(), "test.txt");
        assumeTrue(testFile.setLastModified(testFile.lastModified()
                - FileUtils.getFileUtils().getFileTimestampGranularity() * 3));
        long ts1 = testFile.lastModified();
        buildRule.executeTarget("testPreserve");
        assertEquals(ts1, testFile.lastModified());
    }

}
