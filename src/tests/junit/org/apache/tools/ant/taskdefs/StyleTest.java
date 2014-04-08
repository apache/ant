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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;


/**
 * TestCases for {@link XSLTProcess} task.
 * TODO merge with {@link XsltTest}?
 * @version 2003-08-05
 */
public class StyleTest extends BuildFileTest {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    public StyleTest(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        configureProject("src/etc/testcases/taskdefs/style/build.xml");
        //executeTarget("setup");
        //commented out for performance while target is empty
    }

    public void testStyleIsSet() throws Exception {
        expectSpecificBuildException("testStyleIsSet",
                "no stylesheet specified", "specify the " +
                "stylesheet either as a filename in style " +
                "attribute or as a nested resource");
    }

    public void testTransferParameterSet() throws Exception {
        expectFileContains("testTransferParameterSet",  // target
                           getOutputDir().getAbsoluteFile() + "/out.xml",               // file
                           "set='myvalue'");            // exptected string
    }

    public void testTransferParameterEmpty() throws Exception {
        expectFileContains("testTransferParameterEmpty",
                           getOutputDir().getAbsoluteFile() + "/out.xml",
                           "empty=''");
    }

    public void testTransferParameterUnset() throws Exception {
        expectFileContains("testTransferParameterUnset",
                           getOutputDir().getAbsoluteFile() + "/out.xml",
                           "undefined='${value}'");
    }

    public void testTransferParameterUnsetWithIf() throws Exception {
        expectFileContains("testTransferParameterUnsetWithIf",
                           getOutputDir().getAbsoluteFile() + "/out.xml",
                           "undefined='undefined default value'");
    }

    public void testNewerStylesheet() throws Exception {
        expectFileContains("testNewerStylesheet",
                           getOutputDir().getAbsoluteFile() + "/out.xml",
                           "new-value");
    }

    public void testDefaultMapper() throws Exception {
        testDefaultMapper("testDefaultMapper");
    }

    public void testExplicitFileset() throws Exception {
        testDefaultMapper("testExplicitFileset");
    }

    public void testDefaultMapper(String target) throws Exception {
        assertTrue(!(
                new File(getOutputDir().getAbsoluteFile(), "data.html").exists()));
        expectFileContains(target,
                           getOutputDir().getAbsoluteFile() + "/data.html",
                           "set='myvalue'");
    }

    public void testCustomMapper() throws Exception {
        assertTrue(!new File(getOutputDir().getAbsoluteFile(),  "out.xml").exists());
        expectFileContains("testCustomMapper",
                           getOutputDir().getAbsoluteFile() + "/out.xml",
                           "set='myvalue'");
    }

    public void testTypedMapper() throws Exception {
        assertTrue(!new File(getOutputDir().getAbsoluteFile(),  "out.xml").exists());
        expectFileContains("testTypedMapper",
                           getOutputDir().getAbsoluteFile() + "/out.xml",
                           "set='myvalue'");
    }

    public void testDirectoryHierarchyWithDirMatching() throws Exception {
        executeTarget("testDirectoryHierarchyWithDirMatching");
        assertTrue(new File(getOutputDir().getAbsoluteFile(),  "dest/level1/data.html")
                   .exists());
    }

    public void testDirsWithSpaces() throws Exception {
        executeTarget("testDirsWithSpaces");
        assertTrue(new File(getOutputDir().getAbsoluteFile(),  "d est/data.html")
                   .exists());
    }

    public void testWithStyleAttrAndResource() throws Exception {
        expectSpecificBuildException("testWithStyleAttrAndResource",
                "Must throws a BuildException", "specify the " +
                "stylesheet either as a filename in style " +
                "attribute or as a nested resource but not " +
                "as both");
    }

    public void testWithFileResource() throws Exception {
        expectFileContains("testWithFileResource", getOutputDir().getAbsoluteFile() + "/out.xml", "set='value'");
    }

    public void testWithUrlResource() throws Exception {
        expectFileContains("testWithUrlResource", getOutputDir().getAbsoluteFile() + "/out.xml", "set='value'");
    }

    public void testFilenameAsParam() throws Exception {
        executeTarget("testFilenameAsParam");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/one.txt",      "filename='one.xml'");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/two.txt",      "filename='two.xml'");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/three.txt",    "filename='three.xml'");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/dir/four.txt", "filename='four.xml'");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/dir/four.txt", "filedir ='-not-set-'");
    }

    public void testFilenameAsParamNoSetting() throws Exception {
        executeTarget("testFilenameAsParamNoSetting");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/one.txt",      "filename='-not-set-'");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/two.txt",      "filename='-not-set-'");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/three.txt",    "filename='-not-set-'");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/dir/four.txt", "filename='-not-set-'");
    }

    public void testFilenameAndFiledirAsParam() throws Exception {
        executeTarget("testFilenameAndFiledirAsParam");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/one.txt",      "filename='one.xml'");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/one.txt",      "filedir ='.'");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/dir/four.txt", "filename='four.xml'");
        assertFileContains(getOutputDir().getAbsoluteFile() + "/dir/four.txt", "filedir ='dir'");
    }


    // *************  copied from ConcatTest  *************

    // ------------------------------------------------------
    //   Helper methods - should be in BuildFileTest
    // -----------------------------------------------------

    private String getFileString(String filename)
        throws IOException
    {
        Reader r = null;
        try {
            r = new FileReader(getProject().resolveFile(filename));
            return  FileUtils.readFully(r);
        }
        finally {
            FileUtils.close(r);
        }
    }

    private void expectFileContains(
        String target, String filename, String contains)
        throws IOException
    {
        executeTarget(target);
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
