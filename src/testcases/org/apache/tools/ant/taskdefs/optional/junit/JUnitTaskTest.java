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

import org.apache.tools.ant.BuildFileTest;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class JUnitTaskTest extends BuildFileTest {

    /**
     * Constructor for the JUnitTaskTest object
     */
    public JUnitTaskTest(String name) {
        super(name);
    }


    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/optional/junit.xml");
    }


    /**
     * The teardown method for JUnit
     */
    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testCrash() {
       expectPropertySet("crash", "crashed");
    }

    public void testNoCrash() {
       expectPropertyUnset("nocrash", "crashed");
    }

    public void testTimeout() {
       expectPropertySet("timeout", "timeout");
    }

    public void testNoTimeout() {
       expectPropertyUnset("notimeout", "timeout");
    }

    public void testNonForkedCapture() throws IOException {
        executeTarget("capture");
        assertNoPrint(getLog(), "log");
        assertNoPrint(getFullLog(), "debug log");
    }

    public void testForkedCapture() throws IOException {
        getProject().setProperty("fork", "true");
        testNonForkedCapture();
        // those would fail because of the way BuildFileTest captures output
        assertNoPrint(getOutput(), "output");
        assertNoPrint(getError(), "error output");
        assertOutput();
    }

    public void testBatchTestForkOnceToDir() {
        assertResultFilesExist("testBatchTestForkOnceToDir", ".xml");
    }

    /** Bugzilla Report 32973 */
    public void testBatchTestForkOnceExtension() {
        assertResultFilesExist("testBatchTestForkOnceExtension", ".foo");
    }

    public void testBatchTestForkOnceCustomFormatter() {
        assertResultFilesExist("testBatchTestForkOnceCustomFormatter", "foo");
    }

    private void assertResultFilesExist(String target, String extension) {
        executeTarget(target);
        assertResultFileExists("JUnitClassLoader", extension);
        assertResultFileExists("JUnitTestRunner", extension);
        assertResultFileExists("JUnitVersionHelper", extension);
    }

    private void assertResultFileExists(String classNameFragment, String ext) {
        assertTrue("result for " + classNameFragment + "Test" + ext + " exists",
                   getProject().resolveFile("out/TEST-org.apache.tools.ant."
                                            + "taskdefs.optional.junit."
                                            + classNameFragment + "Test" + ext)
                   .exists());
    }

    private void assertNoPrint(String result, String where) {
        assertTrue(where + " '" + result + "' must not contain print statement",
                   result.indexOf("print to System.") == -1);
    }

    private void assertOutput() throws IOException {
        FileReader inner = new FileReader(getProject()
                                          .resolveFile("testlog.txt"));
        BufferedReader reader = new BufferedReader(inner);
        try {
            String line = reader.readLine();
            assertEquals("Testsuite: org.apache.tools.ant.taskdefs.optional.junit.Printer",
                         line);
            line = reader.readLine();
            assertNotNull(line);
            assertTrue(line.startsWith("Tests run: 1, Failures: 0, Errors: 0, Time elapsed:"));
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

}

