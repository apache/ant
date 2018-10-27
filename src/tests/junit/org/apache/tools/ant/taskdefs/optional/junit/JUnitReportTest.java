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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.security.Permission;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Small testcase for the junitreporttask.
 * First test added to reproduce an fault, still a lot to improve
 *
 */
public class JUnitReportTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/junitreport.xml");
    }

    /**
     * Verifies that no empty junit-noframes.html is generated when frames
     * output is selected via the default.
     * Needs reports1 task from junitreport.xml.
     */
    @Test
    public void testNoFileJUnitNoFrames() {
        buildRule.executeTarget("reports1");
        assertFalse("No file junit-noframes.html expected", (new File(System.getProperty("root"), "src/etc/testcases/taskdefs/optional/junitreport/test/html/junit-noframes.html").exists()));

    }

    public void assertIndexCreated() {
        try {
            commonIndexFileAssertions();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private File commonIndexFileAssertions() throws IOException {
        File reportFile = new File(buildRule.getOutputDir(), "html/index.html");
        commonIndexFileAssertions(reportFile);
        return reportFile;
    }

    private void commonIndexFileAssertions(File reportFile) throws IOException {
        // tests one the file object
        assertTrue("No index.html present. Not generated?", reportFile.exists() );
        assertTrue("Cant read the report file.", reportFile.canRead() );
        assertTrue("File shouldn't be empty.", reportFile.length() > 0 );
        // conversion to URL via FileUtils like in XMLResultAggregator, not as suggested in the bug report
        URL reportUrl = new URL( FileUtils.getFileUtils().toURI(reportFile.getAbsolutePath()) );
        InputStream reportStream = reportUrl.openStream();
        try {
            assertTrue("This shouldn't be an empty stream.", reportStream.available() > 0);
        } finally {
            FileUtils.getFileUtils().close(reportStream);
        }
    }

    @Test
    public void testEmptyFile() throws Exception {
        buildRule.executeTarget("testEmptyFile");
        assertIndexCreated();
        assertContains("Required text not found in log", XMLResultAggregator.WARNING_EMPTY_FILE, buildRule.getLog());
    }

    @Test
    public void testIncompleteFile() throws Exception {
        buildRule.executeTarget("testIncompleteFile");
        assertIndexCreated();
        assertContains("Required text not found in log", XMLResultAggregator.WARNING_IS_POSSIBLY_CORRUPTED, buildRule.getLog());
    }

    @Test
    public void testWrongElement() throws Exception {
        buildRule.executeTarget("testWrongElement");
        assertIndexCreated();
        assertContains("Required text not found in log", XMLResultAggregator.WARNING_INVALID_ROOT_ELEMENT, buildRule.getLog());
    }

    // Bugzilla Report 34963
    @Test
    public void testStackTraceLineBreaks() throws Exception {
        buildRule.executeTarget("testStackTraceLineBreaks");
        assertIndexCreated();
        FileReader r = null;
        try {
            r = new FileReader(new File(buildRule.getOutputDir(), "html/sampleproject/coins/0_CoinTest.html"));
            String report = FileUtils.readFully(r);
            assertContains("output must contain <br>:\n" + report, "junit.framework.AssertionFailedError: DOEG<br>", report);
            assertContains("#51049: output must translate line breaks:\n" + report, "cur['line.separator'] = '\\r\\n';", report);
        } finally {
            FileUtils.close(r);
        }
    }


    // Bugzilla Report 38477
    @Test
    public void testSpecialSignsInSrcPath() throws Exception {
        buildRule.executeTarget("testSpecialSignsInSrcPath");
        commonIndexFileAssertions();
    }

    @Test
    public void testSpecialSignsInHtmlPath() throws Exception {
        buildRule.executeTarget("testSpecialSignsInHtmlPath");
        File reportFile = new File(buildRule.getOutputDir(), "html# $%\u00A7&-!report/index.html");
        commonIndexFileAssertions(reportFile);
    }

    //Bugzilla Report 39708
    @Test
    public void testWithStyleFromDir() throws Exception {
        buildRule.executeTarget("testWithStyleFromDir");
        commonIndexFileAssertions();
    }

    //Bugzilla Report 40021
    @Test
    public void testNoFrames() throws Exception {
        buildRule.executeTarget("testNoFrames");
        File reportFile = new File(buildRule.getOutputDir(), "html/junit-noframes.html");
        commonIndexFileAssertions(reportFile);
    }

    //Bugzilla Report 39708
    @Test
    public void testWithStyleFromDirAndXslImport() throws Exception {
        buildRule.executeTarget("testWithStyleFromDirAndXslImport");
        commonIndexFileAssertions();
    }

    @Test
    public void testWithStyleFromClasspath() throws Exception {
        buildRule.executeTarget("testWithStyleFromClasspath");
        commonIndexFileAssertions();
    }

    @Test
    public void testWithParams() throws Exception {
        buildRule.executeTarget("testWithParams");
        assertContains("key1=value1,key2=value2", buildRule.getLog());
        commonIndexFileAssertions();
    }

    @Test
    public void testWithSecurityManagerAndXalanFactory() throws Exception {
        try {
            String factoryName = TransformerFactory.newInstance().getClass().getName();
            Assume.assumeTrue("TraxFactory is " + factoryName + " and not Xalan",
                              "org.apache.xalan.processor.TransformerFactoryImpl"
                              .equals(factoryName));
        } catch (TransformerFactoryConfigurationError exc) {
            throw new RuntimeException(exc);
        }
        try {
            System.setSecurityManager(new SecurityManager() {public void checkPermission(Permission perm) {}});
            buildRule.executeTarget("testWithStyleFromClasspath");
            commonIndexFileAssertions();
        } finally {
            System.setSecurityManager(null);
        }
    }

    @Test
    public void testWithSecurityManagerAndJDKFactory() throws Exception {
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new ClassLoader(ClassLoader.getSystemClassLoader().getParent()) {
                public InputStream getResourceAsStream(String name) {
                    if (name.startsWith("META-INF/services/")) {
                        // work around JAXP #6723276 in JDK 6
                        return new ByteArrayInputStream(new byte[0]);
                    }
                    return super.getResourceAsStream(name);
                }
            });
            System.setSecurityManager(new SecurityManager() {public void checkPermission(Permission perm) {}});
            buildRule.executeTarget("testWithStyleFromClasspath");
            commonIndexFileAssertions();
        } finally {
            System.setSecurityManager(null);
            Thread.currentThread().setContextClassLoader(orig);
        }
    }

}
