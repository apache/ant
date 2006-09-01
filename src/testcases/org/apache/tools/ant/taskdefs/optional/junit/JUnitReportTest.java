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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

/**
 * Small testcase for the junitreporttask.
 * First test added to reproduce an fault, still a lot to improve
 *
 */
public class JUnitReportTest extends BuildFileTest {

    public JUnitReportTest(String name){
        super(name);
    }

    protected void setUp() {
        configureProject("src/etc/testcases/taskdefs/optional/junitreport.xml");
    }

    protected void tearDown() {
        executeTarget("clean");
    }

    /**
     * Verifies that no empty junit-noframes.html is generated when frames
     * output is selected via the default.
     * Needs reports1 task from junitreport.xml.
     */
    public void testNoFileJUnitNoFrames() {
        executeTarget("reports1");
        if (new File(System.getProperty("root"), "src/etc/testcases/taskdefs/optional/junitreport/test/html/junit-noframes.html").exists())
        {
            fail("No file junit-noframes.html expected");
        }
    }

    public void assertIndexCreated() {
        if (!new File(System.getProperty("root"),
                "src/etc/testcases/taskdefs/optional/junitreport/test/html/index.html").exists()) {
            fail("No file index file found");
        }

    }

    /**
     * run a target, assert the index file is there, look for text in the log
     * @param targetName target
     * @param text optional text to look for
     */
    private void expectReportWithText(String targetName, String text) {
        executeTarget(targetName);
        assertIndexCreated();
        if(text!=null) {
            assertLogContaining(text);
        }
    }


    public void testEmptyFile() throws Exception {
        expectReportWithText("testEmptyFile",
                XMLResultAggregator.WARNING_EMPTY_FILE);
    }

    public void testIncompleteFile() throws Exception {
        expectReportWithText("testIncompleteFile",
                XMLResultAggregator.WARNING_IS_POSSIBLY_CORRUPTED);
    }
    public void testWrongElement() throws Exception {
        expectReportWithText("testWrongElement",
                XMLResultAggregator.WARNING_INVALID_ROOT_ELEMENT);
    }

    // Bugzilla Report 34963
    public void testStackTraceLineBreaks() throws Exception {
        expectReportWithText("testStackTraceLineBreaks", null);
        FileReader r = null;
        try {
            r = new FileReader(new File(System.getProperty("root"),
                                        "src/etc/testcases/taskdefs/optional/junitreport/test/html/sampleproject/coins/0_CoinTest.html"));
            String report = FileUtils.readFully(r);
            assertTrue("output must contain <br>",
                       report.indexOf("junit.framework.AssertionFailedError: DOEG<br/>")
                   > -1);
        } finally {
            FileUtils.close(r);
        }
    }


    // Bugzilla Report 38477
    public void testSpecialSignsInSrcPath() throws Exception {
        executeTarget("testSpecialSignsInSrcPath");
        File reportFile = new File(System.getProperty("root"), "src/etc/testcases/taskdefs/optional/junitreport/test/html/index.html");
        // tests one the file object
        assertTrue("No index.html present. Not generated?", reportFile.exists() );
        assertTrue("Cant read the report file.", reportFile.canRead() );
        assertTrue("File shouldnt be empty.", reportFile.length() > 0 );
        // conversion to URL via FileUtils like in XMLResultAggregator, not as suggested in the bug report
        URL reportUrl = new URL( FileUtils.getFileUtils().toURI(reportFile.getAbsolutePath()) );
        InputStream reportStream = reportUrl.openStream();
        assertTrue("This shouldnt be an empty stream.", reportStream.available() > 0);
    }

    public void testSpecialSignsInHtmlPath() throws Exception {
        executeTarget("testSpecialSignsInHtmlPath");
        File reportFile = new File(System.getProperty("root"), "src/etc/testcases/taskdefs/optional/junitreport/test/html# $%\u00A7&-!report/index.html");
        // tests one the file object
        assertTrue("No index.html present. Not generated?", reportFile.exists() );
        assertTrue("Cant read the report file.", reportFile.canRead() );
        assertTrue("File shouldnt be empty.", reportFile.length() > 0 );
        // conversion to URL via FileUtils like in XMLResultAggregator, not as suggested in the bug report
        URL reportUrl = new URL( FileUtils.getFileUtils().toURI(reportFile.getAbsolutePath()) );
        InputStream reportStream = reportUrl.openStream();
        assertTrue("This shouldnt be an empty stream.", reportStream.available() > 0);
    }

    //Bugzilla Report 39708
    public void testWithStyleFromDir() throws Exception {
        executeTarget("testWithStyleFromDir");
        File reportFile = new File(System.getProperty("root"), "src/etc/testcases/taskdefs/optional/junitreport/test/html/index.html");
        // tests one the file object
        assertTrue("No index.html present. Not generated?", reportFile.exists() );
        assertTrue("Cant read the report file.", reportFile.canRead() );
        assertTrue("File shouldnt be empty.", reportFile.length() > 0 );
        // conversion to URL via FileUtils like in XMLResultAggregator, not as suggested in the bug report
        URL reportUrl = new URL( FileUtils.getFileUtils().toURI(reportFile.getAbsolutePath()) );
        InputStream reportStream = reportUrl.openStream();
        assertTrue("This shouldnt be an empty stream.", reportStream.available() > 0);
    }

    //Bugzilla Report 40021
    public void testNoFrames() throws Exception {
        executeTarget("testNoFrames");
        File reportFile = new File(System.getProperty("root"), "src/etc/testcases/taskdefs/optional/junitreport/test/html/junit-noframes.html");
        // tests one the file object
        assertTrue("No junit-noframes.html present. Not generated?", reportFile.exists() );
        assertTrue("Cant read the report file.", reportFile.canRead() );
        assertTrue("File shouldnt be empty.", reportFile.length() > 0 );
        // conversion to URL via FileUtils like in XMLResultAggregator, not as suggested in the bug report
        URL reportUrl = new URL( FileUtils.getFileUtils().toURI(reportFile.getAbsolutePath()) );
        InputStream reportStream = reportUrl.openStream();
        assertTrue("This shouldnt be an empty stream.", reportStream.available() > 0);
    }
    //Bugzilla Report 39708
    public void testWithStyleFromDirAndXslImport() throws Exception {
        executeTarget("testWithStyleFromDirAndXslImport");
        File reportFile = new File(System.getProperty("root"), "src/etc/testcases/taskdefs/optional/junitreport/test/html/index.html");
        // tests one the file object
        assertTrue("No index.html present. Not generated?", reportFile.exists() );
        assertTrue("Cant read the report file.", reportFile.canRead() );
        assertTrue("File shouldnt be empty.", reportFile.length() > 0 );
        // conversion to URL via FileUtils like in XMLResultAggregator, not as suggested in the bug report
        URL reportUrl = new URL( FileUtils.getFileUtils().toURI(reportFile.getAbsolutePath()) );
        InputStream reportStream = reportUrl.openStream();
        assertTrue("This shouldnt be an empty stream.", reportStream.available() > 0);
    }

    public void testWithStyleFromClasspath() throws Exception {
        executeTarget("testWithStyleFromClasspath");
        File reportFile = new File(System.getProperty("root"), "src/etc/testcases/taskdefs/optional/junitreport/test/html/index.html");
        // tests one the file object
        assertTrue("No index.html present. Not generated?", reportFile.exists() );
        assertTrue("Cant read the report file.", reportFile.canRead() );
        assertTrue("File shouldnt be empty.", reportFile.length() > 0 );
        // conversion to URL via FileUtils like in XMLResultAggregator, not as suggested in the bug report
        URL reportUrl = new URL( FileUtils.getFileUtils().toURI(reportFile.getAbsolutePath()) );
        InputStream reportStream = reportUrl.openStream();
        assertTrue("This shouldnt be an empty stream.", reportStream.available() > 0);
    }

    public void testWithParams() throws Exception {
        expectLogContaining("testWithParams", "key1=value1,key2=value2");
        File reportFile = new File(System.getProperty("root"), "src/etc/testcases/taskdefs/optional/junitreport/test/html/index.html");
        // tests one the file object
        assertTrue("No index.html present. Not generated?", reportFile.exists() );
        assertTrue("Cant read the report file.", reportFile.canRead() );
        assertTrue("File shouldnt be empty.", reportFile.length() > 0 );
        // conversion to URL via FileUtils like in XMLResultAggregator, not as suggested in the bug report
        URL reportUrl = new URL( FileUtils.getFileUtils().toURI(reportFile.getAbsolutePath()) );
        InputStream reportStream = reportUrl.openStream();
        assertTrue("This shouldnt be an empty stream.", reportStream.available() > 0);
    }
}
