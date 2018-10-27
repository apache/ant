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
import static org.junit.Assert.fail;


/**
 * TestCases for {@link XSLTProcess} task.
 * TODO merge with {@link org.apache.tools.ant.taskdefs.optional.XsltTest}?
 * @version 2003-08-05
 */
public class StyleTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() throws Exception {
        buildRule.configureProject("src/etc/testcases/taskdefs/style/build.xml");
    }

    @Test
    public void testStyleIsSet() throws Exception {

        try {
            buildRule.executeTarget("testStyleIsSet");
            fail("Must throws a BuildException: no stylesheet specified");
        } catch (BuildException ex) {
            assertEquals("specify the stylesheet either as a filename in style attribute or as a nested resource",
                    ex.getMessage());
        }
    }

    @Test
    public void testTransferParameterSet() throws Exception {
        expectFileContains("testTransferParameterSet",  // target
                           buildRule.getOutputDir().getAbsoluteFile() + "/out.xml",               // file
                           "set='myvalue'");            // exptected string
    }

    @Test
    public void testTransferParameterEmpty() throws Exception {
        expectFileContains("testTransferParameterEmpty",
                           buildRule.getOutputDir().getAbsoluteFile() + "/out.xml",
                           "empty=''");
    }

    @Test
    public void testTransferParameterUnset() throws Exception {
        expectFileContains("testTransferParameterUnset",
                           buildRule.getOutputDir().getAbsoluteFile() + "/out.xml",
                           "undefined='${value}'");
    }

    @Test
    public void testTransferParameterUnsetWithIf() throws Exception {
        expectFileContains("testTransferParameterUnsetWithIf",
                           buildRule.getOutputDir().getAbsoluteFile() + "/out.xml",
                           "undefined='undefined default value'");
    }

    @Test
    public void testNewerStylesheet() throws Exception {
        expectFileContains("testNewerStylesheet",
                           buildRule.getOutputDir().getAbsoluteFile() + "/out.xml",
                           "new-value");
    }

    @Test
    public void testDefaultMapper() throws Exception {
        testDefaultMapper("testDefaultMapper");
    }

    @Test
    public void testExplicitFileset() throws Exception {
        testDefaultMapper("testExplicitFileset");
    }

    public void testDefaultMapper(String target) throws Exception {
        assertTrue(!(
                new File(buildRule.getOutputDir().getAbsoluteFile(), "data.html").exists()));
        expectFileContains(target,
                           buildRule.getOutputDir().getAbsoluteFile() + "/data.html",
                           "set='myvalue'");
    }

    @Test
    public void testCustomMapper() throws Exception {
        assertTrue(!new File(buildRule.getOutputDir().getAbsoluteFile(),  "out.xml").exists());
        expectFileContains("testCustomMapper",
                           buildRule.getOutputDir().getAbsoluteFile() + "/out.xml",
                           "set='myvalue'");
    }

    @Test
    public void testTypedMapper() throws Exception {
        assertTrue(!new File(buildRule.getOutputDir().getAbsoluteFile(),  "out.xml").exists());
        expectFileContains("testTypedMapper",
                           buildRule.getOutputDir().getAbsoluteFile() + "/out.xml",
                           "set='myvalue'");
    }

    @Test
    public void testDirectoryHierarchyWithDirMatching() throws Exception {
        buildRule.executeTarget("testDirectoryHierarchyWithDirMatching");
        assertTrue(new File(buildRule.getOutputDir().getAbsoluteFile(),  "dest/level1/data.html")
                   .exists());
    }

    @Test
    public void testDirsWithSpaces() throws Exception {
        buildRule.executeTarget("testDirsWithSpaces");
        assertTrue(new File(buildRule.getOutputDir().getAbsoluteFile(),  "d est/data.html")
                   .exists());
    }

    @Test
    public void testWithStyleAttrAndResource() {
        try {
            buildRule.executeTarget("testWithStyleAttrAndResource");
            fail("Must throws a BuildException");
        } catch (BuildException ex) {
            assertEquals("specify the stylesheet either as a filename in style attribute or as a "
                    + "nested resource but not as both", ex.getMessage());
        }
    }

    @Test
    public void testWithFileResource() throws Exception {
        expectFileContains("testWithFileResource", buildRule.getOutputDir().getAbsoluteFile() + "/out.xml", "set='value'");
    }

    @Test
    public void testWithUrlResource() throws Exception {
        expectFileContains("testWithUrlResource", buildRule.getOutputDir().getAbsoluteFile() + "/out.xml", "set='value'");
    }

    @Test
    public void testFilenameAsParam() throws Exception {
        buildRule.executeTarget("testFilenameAsParam");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/one.txt",      "filename='one.xml'");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/two.txt",      "filename='two.xml'");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/three.txt",    "filename='three.xml'");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/dir/four.txt", "filename='four.xml'");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/dir/four.txt", "filedir ='-not-set-'");
    }

    @Test
    public void testFilenameAsParamNoSetting() throws Exception {
        buildRule.executeTarget("testFilenameAsParamNoSetting");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/one.txt",      "filename='-not-set-'");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/two.txt",      "filename='-not-set-'");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/three.txt",    "filename='-not-set-'");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/dir/four.txt", "filename='-not-set-'");
    }

    @Test
    public void testFilenameAndFiledirAsParam() throws Exception {
        buildRule.executeTarget("testFilenameAndFiledirAsParam");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/one.txt",      "filename='one.xml'");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/one.txt",      "filedir ='.'");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/dir/four.txt", "filename='four.xml'");
        assertFileContains(buildRule.getOutputDir().getAbsoluteFile() + "/dir/four.txt", "filedir ='dir'");
    }


    // *************  copied from ConcatTest  *************

    // ------------------------------------------------------
    //   Helper methods - should be in BuildFileTest
    // -----------------------------------------------------

    private String getFileString(String filename)
        throws IOException
    {
        return FileUtilities.getFileContents(new File(filename));
    }

    private void expectFileContains(
        String target, String filename, String contains)
        throws IOException
    {
        buildRule.executeTarget(target);
        assertFileContains(filename, contains);
    }

    private void assertFileContains(String filename, String contains) throws IOException {
        String content = getFileString(filename);
        assertTrue(
              "expecting file " + filename
            + " to contain " + contains
            + " but got " + content,
            content.indexOf(contains) > -1);
    }

}
