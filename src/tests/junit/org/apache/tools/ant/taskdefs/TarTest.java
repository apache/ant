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

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TarTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/tar.xml");
        buildRule.executeTarget("setUp");
    }

    /**
     * Expected failure: required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test1() {
        buildRule.executeTarget("test1");
        // TODO assert value
    }

    /**
     * Expected failure: required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test2() {
        buildRule.executeTarget("test2");
        // TODO assert value
    }

    /**
     * Expected failure: required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test3() {
        buildRule.executeTarget("test3");
        // TODO assert value
    }

    /**
     * Expected failure: tar cannot include itself
     */
    @Test(expected = BuildException.class)
    public void test4() {
        buildRule.executeTarget("test4");
        // TODO assert value
    }

    @Test
    public void test5() {
        buildRule.executeTarget("test5");
        File f = new File(buildRule.getProject().getProperty("output"), "test5.tar");
        assertTrue("Tarring a directory failed", f.exists());
    }

    /**
     * Expected failure due to invalid value specified for longfile attribute.
     */
    @Test(expected = BuildException.class)
    public void test6() {
        buildRule.executeTarget("test6");
        // TODO assert value
    }

    @Test
    public void test7() {
        test7("test7");
    }

    @Test
    public void test7UsingPlainFileSet() {
        test7("test7UsingPlainFileSet");
    }

    @Test
    public void test7UsingFileList() {
        test7("test7UsingFileList");
    }

    private void test7(String target) {
        buildRule.executeTarget(target);
        final String output = buildRule.getProject().getProperty("output");
        File f1 = new File(output, "untar/test7-prefix");
        assertTrue("The prefix attribute is not working properly.", f1.exists() && f1.isDirectory());
        File f2 = new File(output, "untar/test7dir");
        assertTrue("The prefix attribute is not working properly.", f2.exists() && f2.isDirectory());
    }

    @Test
    public void test8() {
        test8("test8");
    }

    @Test
    public void test8UsingZipFileset() {
        test8("test8UsingZipFileset");
    }

    @Test
    public void test8UsingZipFilesetSrc() {
        test8("test8UsingZipFilesetSrc");
    }

    @Test
    public void test8UsingTarFilesetSrc() {
        test8("test8UsingTarFilesetSrc");
    }

    @Test
    public void test8UsingZipEntry() {
        test8("test8UsingZipEntry");
    }

    private void test8(String target) {
        buildRule.executeTarget(target);
        File f1 = new File(buildRule.getProject().getProperty("output"), "untar/test8.xml");
        assertTrue("The fullpath attribute or the preserveLeadingSlashes attribute does not work properly",
                f1.exists());
    }

    /**
     * Expected failure due to invalid value specified for compression attribute.
     */
    @Test(expected = BuildException.class)
    public void test9() {
        buildRule.executeTarget("test9");
        // TODO assert value
    }

    @Test
    public void test10() {
        buildRule.executeTarget("test10");
        File f1 = new File(buildRule.getProject().getProperty("output"), "untar/test10.xml");
        assertTrue("The fullpath attribute or the preserveLeadingSlashes attribute does not work properly",
                f1.exists());
    }

    @Test
    public void test11() {
        buildRule.executeTarget("test11");
        File f1 = new File(buildRule.getProject().getProperty("output"), "untar/test11.xml");
        assertTrue("The fullpath attribute or the preserveLeadingSlashes attribute does not work properly",
                f1.exists());
    }

    @Test
    public void testGZipResource() throws IOException {
        buildRule.executeTarget("testGZipResource");
        assertEquals(FileUtilities.getFileContents(buildRule.getProject().resolveFile("../asf-logo.gif")),
                FileUtilities.getFileContents(new File(buildRule.getProject().getProperty("output"), "untar/asf-logo.gif.gz")));
    }

    @Test
    public void testtestTarFilesetWithReference() {
        buildRule.executeTarget("testTarFilesetWithReference");
    }
}
