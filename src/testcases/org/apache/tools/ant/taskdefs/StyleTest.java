/*
 * Copyright  2003-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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


import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.FileWriter;


/**
 * TestCases for <style> / <xslt> task.
 * @author Jan Materne
 * @version 2003-08-05
 */
public class StyleTest extends BuildFileTest {

    public StyleTest(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        configureProject("src/etc/testcases/taskdefs/style/build.xml");
        //executeTarget("setup");
        //commented out for performance while target is empty
    }

    protected void tearDown() throws Exception {
        executeTarget("teardown");
    }

    public void testStyleIsSet() throws Exception {
        expectBuildException("testStyleIsSet", "no stylesheet specified");
    }

    public void testTransferParameterSet() throws Exception {
        expectFileContains("testTransferParameterSet",  // target
                           "out/out.xml",               // file
                           "set='myvalue'");            // exptected string
    }

    public void testTransferParameterEmpty() throws Exception {
        expectFileContains("testTransferParameterEmpty",
                           "out/out.xml",
                           "empty=''");
    }

    public void testTransferParameterUnset() throws Exception {
        expectFileContains("testTransferParameterUnset",
                           "out/out.xml",
                           "undefined='${value}'");
    }

    public void testTransferParameterUnsetWithIf() throws Exception {
        expectFileContains("testTransferParameterUnsetWithIf",
                           "out/out.xml",
                           "undefined='undefined default value'");
    }

    public void testNewerStylesheet() throws Exception {
        expectFileContains("testNewerStylesheet",
                           "out/out.xml",
                           "new-value");
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
            return  FileUtils.newFileUtils().readFully(r);
        }
        finally {
            try {r.close();} catch (Throwable ignore) {}
        }

    }

    private String getFileString(String target, String filename)
        throws IOException
    {
        executeTarget(target);
        return getFileString(filename);
    }

    private void expectFileContains(
        String target, String filename, String contains)
        throws IOException
    {
        String content = getFileString(target, filename);
        assertTrue(
            "expecting file " + filename + " to contain " +
            contains +
            " but got " + content, content.indexOf(contains) > -1);
    }

}
