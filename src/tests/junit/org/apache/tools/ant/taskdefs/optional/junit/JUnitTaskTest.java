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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.apache.tools.ant.AntAssert.assertNotContains;
import static org.apache.tools.ant.AntAssert.assertContains;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class JUnitTaskTest {

	@Rule
	public BuildFileRule buildRule = new BuildFileRule();
	
    /**
     * The JUnit setup method.
     */
	@Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/junit.xml");
    }

	@Test
    public void testCrash() {
    	buildRule.executeTarget("crash");
    	assertEquals("true", buildRule.getProject().getProperty("crashed"));
    }

	@Test
    public void testNoCrash() {
    	buildRule.executeTarget("nocrash");
    	assertNull(buildRule.getProject().getProperty("crashed"));
    }

	@Test
    public void testTimeout() {
    	buildRule.executeTarget("timeout");
    	assertEquals("true", buildRule.getProject().getProperty("timeout"));
    }

    @Test
    public void testNoTimeout() {
       buildRule.executeTarget("notimeout");
   	   assertNull(buildRule.getProject().getProperty("timeout"));
    }

    @Test
    public void testNonForkedCapture() throws IOException {
        buildRule.executeTarget("capture");
        assertNoPrint(buildRule.getLog(), "log");
        assertNoPrint(buildRule.getFullLog(), "debug log");
    }

    @Test
    public void testForkedCapture() throws IOException {
        buildRule.getProject().setProperty("fork", "true");
        testNonForkedCapture();
        // those would fail because of the way BuildFileRule captures output
        assertNoPrint(buildRule.getOutput(), "output");
        assertNoPrint(buildRule.getError(), "error output");
        assertOutput();
    }

    @Test
    public void testBatchTestForkOnceToDir() {
        assertResultFilesExist("testBatchTestForkOnceToDir", ".xml");
    }

    /** Bugzilla Report 32973 */
    @Test
    public void testBatchTestForkOnceExtension() {
        assertResultFilesExist("testBatchTestForkOnceExtension", ".foo");
    }


    /* Bugzilla Report 42984 */
    //TODO This scenario works from command line, but not from JUnit ...
    //     Running these steps from the junit.xml-directory work
    //     $ ant -f junit.xml failureRecorder.prepare
    //     $ ant -f junit.xml failureRecorder.runtest
    //     $ ant -f junit.xml failureRecorder.runtest
    //     $ ant -f junit.xml failureRecorder.fixing
    //     $ ant -f junit.xml failureRecorder.runtest
    //     $ ant -f junit.xml failureRecorder.runtest
    //     But running the JUnit testcase fails in 4th run.
    @Test
    public void testFailureRecorder() {
        if (JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_1_5)) {
            try {
            	Class<?> clazz =Class.forName("junit.framework.JUnit4TestAdapter");
                Assume.assumeFalse("Skipping test since it fails with JUnit 4", clazz != null);
            } catch (ClassNotFoundException e) {
                // OK, this is JUnit3, can run test
            }
        }

        File testDir = new File(buildRule.getOutputDir(), "out");
        File collectorFile = new File(buildRule.getOutputDir(),
                "out/FailedTests.java");

        // ensure that there is a clean test environment
        assertFalse("Test directory '" + testDir.getAbsolutePath()
                    + "' must not exist before the test preparation.", 
                    testDir.exists());
        assertFalse("The collector file '"
                    + collectorFile.getAbsolutePath()
                    + "'must not exist before the test preparation.", 
                    collectorFile.exists());

    
        // prepare the test environment
        buildRule.executeTarget("failureRecorder.prepare");
        assertTrue("Test directory '" + testDir.getAbsolutePath()
                   + "' was not created.", testDir.exists());
        assertTrue("There should be one class.",
                   (new File(testDir, "A.class")).exists());
        assertFalse("The collector file '"
                    + collectorFile.getAbsolutePath() 
                    + "' should not exist before the 1st run.",
                    collectorFile.exists());
    
    
        // 1st junit run: should do all tests - failing and not failing tests
        buildRule.executeTarget("failureRecorder.runtest");
        assertTrue("The collector file '" + collectorFile.getAbsolutePath() 
                   + "' should exist after the 1st run.",
                   collectorFile.exists());
        // the passing test cases
        buildRule.executeTarget("A.test01");
        assertContains("1st run: should run A.test01", buildRule.getOutput());
        buildRule.executeTarget("B.test05");
        assertContains("1st run: should run B.test05", buildRule.getOutput());
        buildRule.executeTarget("B.test06");
        assertContains("1st run: should run B.test06", buildRule.getOutput());
        buildRule.executeTarget("C.test07");
        assertContains("1st run: should run C.test07", buildRule.getOutput());
        buildRule.executeTarget("C.test08");
        assertContains("1st run: should run C.test08", buildRule.getOutput());
        buildRule.executeTarget("C.test09");
        assertContains("1st run: should run C.test09", buildRule.getOutput());
        // the failing test cases
        buildRule.executeTarget("A.test02");
        assertContains("1st run: should run A.test02", buildRule.getOutput());
        buildRule.executeTarget("A.test03");
        assertContains("1st run: should run A.test03", buildRule.getOutput());
        buildRule.executeTarget("B.test04");
        assertContains("1st run: should run B.test04", buildRule.getOutput());
        buildRule.executeTarget("D.test10");
        assertContains("1st run: should run D.test10", buildRule.getOutput());

    
        // 2nd junit run: should do only failing tests
        buildRule.executeTarget("failureRecorder.runtest");
        assertTrue("The collector file '" + collectorFile.getAbsolutePath() 
                   + "' should exist after the 2nd run.",
                   collectorFile.exists());
        // the passing test cases
        buildRule.executeTarget("A.test01");
        assertNotContains("2nd run: should not run A.test01", buildRule.getOutput());
        buildRule.executeTarget("B.test05");
        assertNotContains("2nd run: should not run A.test05", buildRule.getOutput());
        buildRule.executeTarget("B.test06");
        assertNotContains("2nd run: should not run B.test06", buildRule.getOutput());
        buildRule.executeTarget("C.test07");
        assertNotContains("2nd run: should not run C.test07", buildRule.getOutput());
        buildRule.executeTarget("C.test08");
        assertNotContains("2nd run: should not run C.test08", buildRule.getOutput());
        buildRule.executeTarget("C.test09");
        assertNotContains("2nd run: should not run C.test09", buildRule.getOutput());
        // the failing test cases
        buildRule.executeTarget("A.test02");
        assertContains("2nd run: should run A.test02", buildRule.getOutput());
        buildRule.executeTarget("A.test03");
        assertContains("2nd run: should run A.test03", buildRule.getOutput());
        buildRule.executeTarget("B.test04");
        assertContains("2nd run: should run B.test04", buildRule.getOutput());
        buildRule.executeTarget("D.test10");
        assertContains("2nd run: should run D.test10", buildRule.getOutput());
    
    
        // "fix" errors in class A
        buildRule.executeTarget("failureRecorder.fixing");
    
        // 3rd run: four running tests with two errors
        buildRule.executeTarget("failureRecorder.runtest");
        assertTrue("The collector file '" + collectorFile.getAbsolutePath() 
                   + "' should exist after the 3rd run.",
                   collectorFile.exists());
        buildRule.executeTarget("A.test02");
        assertContains("3rd run: should run A.test02", buildRule.getOutput());
        buildRule.executeTarget("A.test03");
        assertContains("3rd run: should run A.test03", buildRule.getOutput());
        buildRule.executeTarget("B.test04");
        assertContains("3rd run: should run B.test04", buildRule.getOutput());
        buildRule.executeTarget("D.test10");
        assertContains("3rd run: should run D.test10", buildRule.getOutput());
    
    
        // 4rd run: two running tests with errors
        buildRule.executeTarget("failureRecorder.runtest");
        assertTrue("The collector file '" + collectorFile.getAbsolutePath() 
                   + "' should exist after the 4th run.",
                   collectorFile.exists());
        //TODO: these two statements fail
        //buildRule.executeTarget("A.test02");assertNotContains("4th run: should not run A.test02", buildRule.getOutput());
        //buildRule.executeTarget("A.test03");assertNotContains("4th run: should not run A.test03", buildRule.getOutput());
        buildRule.executeTarget("B.test04");
        assertContains("4th run: should run B.test04", buildRule.getOutput());
        buildRule.executeTarget("D.test10");
        assertContains("4th run: should run D.test10", buildRule.getOutput());

    }

    @Test
    public void testBatchTestForkOnceCustomFormatter() {
        assertResultFilesExist("testBatchTestForkOnceCustomFormatter", "foo");
    }

    // Bugzilla Issue 45411
    @Test
    public void testMultilineAssertsNoFork() {
        buildRule.executeTarget("testMultilineAssertsNoFork");
        assertNotContains("messaged up", buildRule.getLog());
        assertNotContains("crashed)", buildRule.getLog());
    }

    // Bugzilla Issue 45411
    @Test
    public void testMultilineAssertsFork() {
        buildRule.executeTarget("testMultilineAssertsFork");
        assertNotContains("messaged up", buildRule.getLog());
        assertNotContains("crashed)", buildRule.getLog());
    }

    private void assertResultFilesExist(String target, String extension) {
        buildRule.executeTarget(target);
        assertResultFileExists("JUnitClassLoader", extension);
        assertResultFileExists("JUnitTestRunner", extension);
        assertResultFileExists("JUnitVersionHelper", extension);
    }

    private void assertResultFileExists(String classNameFragment, String ext) {
        assertTrue("result for " + classNameFragment + "Test" + ext + " exists",

                   new File(buildRule.getOutputDir(), "TEST-org.apache.tools.ant."
                                            + "taskdefs.optional.junit."
                                            + classNameFragment + "Test" + ext)
                   .exists());
    }

    private void assertNoPrint(String result, String where) {
        assertNotContains(where + " '" + result + "' must not contain print statement",
                   "print to System.", result);
    }

    private void assertOutput() throws IOException {
        FileReader inner = new FileReader(new File(buildRule.getOutputDir(),
                                          "testlog.txt"));
        BufferedReader reader = new BufferedReader(inner);
        try {
            String line = reader.readLine();
            assertEquals("Testsuite: org.apache.tools.ant.taskdefs.optional.junit.Printer",
                         line);
            line = reader.readLine();
            assertNotNull(line);
            assertTrue(line.startsWith("Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed:"));
            line = reader.readLine();
            assertEquals("------------- Standard Output ---------------",
                         line);
            assertPrint(reader.readLine(), "static", "out");
            assertPrint(reader.readLine(), "constructor", "out");
            assertPrint(reader.readLine(), "method", "out");
            line = reader.readLine();
            assertEquals("------------- ---------------- ---------------",
                         line);
            line = reader.readLine();
            assertEquals("------------- Standard Error -----------------",
                         line);
            assertPrint(reader.readLine(), "static", "err");
            assertPrint(reader.readLine(), "constructor", "err");
            assertPrint(reader.readLine(), "method", "err");
            line = reader.readLine();
            assertEquals("------------- ---------------- ---------------",
                         line);
            line = reader.readLine();
            assertEquals("", line);
            line = reader.readLine();
            assertNotNull(line);
            assertTrue(line.startsWith("Testcase: testNoCrash took "));
        } finally {
            inner.close();
        }
    }

    private void assertPrint(String line, String from, String to) {
        String search = from + " print to System." + to;
        assertEquals(search, line);
    }

    @Test
    public void testJUnit4Skip() throws Exception {
        buildRule.executeTarget("testSkippableTests");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File(buildRule.getOutputDir(), "TEST-org.example.junit.JUnit4Skippable.xml"));

        assertEquals("Incorrect number of nodes created", 8, doc.getElementsByTagName("testcase").getLength());

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        assertEquals("Incorrect number of skipped tests in header", 4, Integer.parseInt(xpath.compile("//testsuite/@skipped").evaluate(doc)));
        assertEquals("Incorrect number of error tests in header", 1, Integer.parseInt(xpath.compile("//testsuite/@errors").evaluate(doc)));
        assertEquals("Incorrect number of failure tests in header", 2, Integer.parseInt(xpath.compile("//testsuite/@failures").evaluate(doc)));
        assertEquals("Incorrect number of tests in header", 8, Integer.parseInt(xpath.compile("//testsuite/@tests").evaluate(doc)));


        assertEquals("Incorrect ignore message on explicit ignored test", "Please don't ignore me!", xpath.compile("//testsuite/testcase[@name='explicitIgnoreTest']/skipped/@message").evaluate(doc));
        assertEquals("No message should be set on Ignored tests with no Ignore annotation text", 0, ((Node)xpath.compile("//testsuite/testcase[@name='explicitlyIgnoreTestNoMessage']/skipped").evaluate(doc, XPathConstants.NODE)).getAttributes().getLength());
        assertEquals("Incorrect ignore message on implicit ignored test", "This test will be ignored", xpath.compile("//testsuite/testcase[@name='implicitlyIgnoreTest']/skipped/@message").evaluate(doc));
        assertNotNull("Implicit ignore test should have an ignore element", xpath.compile("//testsuite/testcase[@name='implicitlyIgnoreTestNoMessage']/skipped").evaluate(doc, XPathConstants.NODE));

    }

    @Test
    public void testTestMethods() throws Exception {
        buildRule.executeTarget("testTestMethods");
    }

    @Test
    public void testNonTestsSkipped() throws Exception {

        buildRule.executeTarget("testNonTests");
        assertFalse("Test result should not exist as test was skipped - TEST-org.example.junit.NonTestMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.NonTestMissed.xml").exists());
        assertFalse("Test result should not exist as test was skipped - TEST-org.example.junit.JUnit3NonTestMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.JUnit3TestMissed.xml").exists());
        assertFalse("Test result should not exist as test was skipped - TEST-org.example.junit.AbstractTestMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.AbstractTestMissed.xml").exists());
        assertFalse("Test result should not exist as test was skipped - TEST-org.example.junit.AbstractJUnit3TestMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.AbstractJUnit3TestMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.AbstractTestNotMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.AbstractTestNotMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.AbstractJUnit3TestNotMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.AbstractJUnit3TestNotMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.TestNotMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.TestNotMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.JUnit3TestNotMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.JUnit3TestNotMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.TestWithSuiteNotMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.TestWithSuiteNotMissed.xml").exists());

        buildRule.executeTarget("testNonTestsRun");
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.NonTestMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.NonTestMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.JUnit3NonTestMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.JUnit3NonTestMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.TestNotMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.TestNotMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.JUnit3TestNotMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.JUnit3TestNotMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.AbstractTestMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.AbstractTestMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.AbstractTestNotMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.AbstractTestNotMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.AbstractJUnit3TestMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.AbstractJUnit3TestMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.JUnit3NonTestMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.JUnit3NonTestMissed.xml").exists());
        assertTrue("Test result should exist as test was not skipped - TEST-org.example.junit.TestWithSuiteNotMissed.xml", new File(buildRule.getOutputDir(), "TEST-org.example.junit.TestWithSuiteNotMissed.xml").exists());


    }

}
