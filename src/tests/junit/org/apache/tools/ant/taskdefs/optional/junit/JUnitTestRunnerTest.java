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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.tools.ant.BuildException;
import org.junit.Test;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Small testcase for the runner, tests are very very very basics.
 * They must be enhanced with time.
 *
 */
public class JUnitTestRunnerTest {

    // check that a valid method name generates no errors
    @Test
    public void testValidMethod() {
        TestRunner runner = createRunnerForTestMethod(ValidMethodTestCase.class, "testA");
        runner.run();
        assertEquals(runner.getFormatter().getError(), JUnitTestRunner.SUCCESS, runner.getRetCode());
    }

    // check that having an invalid method name generates an error
    @Test
    public void testInvalidMethod() {
        TestRunner runner = createRunnerForTestMethod(InvalidMethodTestCase.class, "testInvalid");
        runner.run();
        String error = runner.getFormatter().getError();
        // might be FAILURES or ERRORS depending on JUnit version?
        assertNotEquals(error, JUnitTestRunner.SUCCESS, runner.getRetCode());
    }

    // check that having no suite generates no errors
    @Test
    public void testNoSuite() {
        TestRunner runner = createRunner(NoSuiteTestCase.class);
        runner.run();
        assertEquals(runner.getFormatter().getError(), JUnitTestRunner.SUCCESS, runner.getRetCode());
    }

    // check that a suite generates no errors
    @Test
    public void testSuite() {
        TestRunner runner = createRunner(SuiteTestCase.class);
        runner.run();
        assertEquals(runner.getFormatter().getError(), JUnitTestRunner.SUCCESS, runner.getRetCode());
    }

    // check that an invalid suite generates an error.
    @Test
    public void testInvalidSuite() {
        TestRunner runner = createRunner(InvalidSuiteTestCase.class);
        runner.run();
        String error = runner.getFormatter().getError();
        assertEquals(error, JUnitTestRunner.ERRORS, runner.getRetCode());
        assertThat(error, error, containsString("thrown on purpose"));
    }

    // check that something which is not a testcase generates no errors
    // at first even though this is incorrect.
    @Test
    public void testNoTestCase() {
        TestRunner runner = createRunner(NoTestCase.class);
        runner.run();
        // On junit3 this is a FAILURE, on junit4 this is an ERROR
        int ret = runner.getRetCode();
        assertTrue("Unexpected result " + ret + " from junit runner",
                ret == JUnitTestRunner.FAILURES || ret == JUnitTestRunner.ERRORS);
        // JUnit3 test
        //assertEquals(runner.getFormatter().getError(), JUnitTestRunner.FAILURES, runner.getRetCode());
    }

    // check that something which is not a testcase doesn't generate an error
    // when skipping non-test classes
    @Test
    public void testSkipNonTestsNoTestCase() {
        TestRunner runner = createRunner(NoTestCase.class, true);
        runner.run();
        assertEquals(runner.getFormatter().getError(), JUnitTestRunner.SUCCESS, runner.getRetCode());
    }

    // check that something which is not a testcase with a failing static init doesn't generate an error
    // when skipping non-test classes
    @Test
    public void testSkipNonTestsNoTestCaseFailingStaticInit() {
        TestRunner runner = createRunner(NoTestCaseStaticInitializerError.class, true);
        runner.run();
        assertEquals(runner.getFormatter().getError(), JUnitTestRunner.SUCCESS, runner.getRetCode());
    }

    @Test
    public void testStaticInitializerErrorTestCase() {
        TestRunner runner = createRunner(StaticInitializerErrorTestCase.class);
        runner.run();
        // For JUnit 3 this is a FAILURE, for JUnit 4 this is an ERROR
        int ret = runner.getRetCode();
        assertTrue("Unexpected result " + ret + " from junit runner",
                ret == JUnitTestRunner.FAILURES || ret == JUnitTestRunner.ERRORS);
    }

    // check that an exception in the constructor is noticed
    @Test
    public void testInvalidTestCase() {
        TestRunner runner = createRunner(InvalidTestCase.class);
        runner.run();
        // For JUnit 3 this is a FAILURE, for JUnit 4 this is an ERROR
        int ret = runner.getRetCode();
        assertTrue("Unexpected result " + ret + " from junit runner",
                ret == JUnitTestRunner.FAILURES || ret == JUnitTestRunner.ERRORS);
        // JUNIT3 test
        //assertEquals(error, JUnitTestRunner.FAILURES, runner.getRetCode());
        //@fixme as of now does not report the original stacktrace.
        //assertThat(error, error, containsString("thrown on purpose"));
    }

    // check that JUnit 4 synthetic AssertionFailedError gets message and cause from AssertionError
    @Test
    public void testJUnit4AssertionError() {
        TestRunner runner = createRunnerForTestMethod(AssertionErrorTest.class, "throwsAssertionError");
        runner.run();

        AssertionFailedError failure = runner.getFormatter().getFailure();
        assertEquals("failure message", failure.getMessage());

        Throwable cause = failure.getCause();
        assertEquals(RuntimeException.class, cause.getClass());
        assertEquals("cause message", cause.getMessage());
    }

    protected TestRunner createRunner(Class<?> clazz) {
        return new TestRunner(new JUnitTest(clazz.getName()), null,
                                            true, true, true);
    }

    protected TestRunner createRunner(Class<?> clazz, boolean skipNonTests) {
        JUnitTest test = new JUnitTest(clazz.getName());
        test.setSkipNonTests(skipNonTests);
        return new TestRunner(test, null, true, true, true);
    }

    protected TestRunner createRunnerForTestMethod(Class<?> clazz, String method) {
        return new TestRunner(new JUnitTest(clazz.getName()), new String[] {method},
                                            true, true, true);
    }

    // the test runner that wrap the dummy formatter that interests us
    private static final class TestRunner extends JUnitTestRunner {
        private ResultFormatter formatter = new ResultFormatter();
        TestRunner(JUnitTest test, String[] methods, boolean haltonerror,
                   boolean filtertrace, boolean haltonfailure) {
            super(test, methods, haltonerror, filtertrace,  haltonfailure,
                  false, false, TestRunner.class.getClassLoader());
            // use the classloader that loaded this class otherwise
            // it will not be able to run inner classes if this test
            // is ran in non-forked mode.
            addFormatter(formatter);
        }
        ResultFormatter getFormatter() {
            return formatter;
        }
    }

    // dummy formatter just to catch the error
    private static final class ResultFormatter implements JUnitResultFormatter {
        private AssertionFailedError failure;
        private Throwable error;
        public void setSystemOutput(String output) {
        }
        public void setSystemError(String output) {
        }
        public void startTestSuite(JUnitTest suite) throws BuildException {
        }
        public void endTestSuite(JUnitTest suite) throws BuildException {
        }
        public void setOutput(OutputStream out) {
        }
        public void startTest(junit.framework.Test t) {
        }
        public void endTest(junit.framework.Test test) {
        }
        public void addFailure(junit.framework.Test test, AssertionFailedError t) {
            failure = t;
        }
        AssertionFailedError getFailure() {
            return failure;
        }
        public void addError(junit.framework.Test test, Throwable t) {
            error = t;
        }
        String getError() {
            if (error == null) {
                return "";
            }
            StringWriter sw = new StringWriter();
            error.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }
    }

    public static class NoTestCase {
    }

    public static class NoTestCaseStaticInitializerError {
        static {
            error();
        }
        private static void error() {
            throw new NullPointerException("thrown on purpose");
        }
    }

    public static class InvalidMethodTestCase extends TestCase {
        public InvalidMethodTestCase(String name) {
            super(name);
        }
        public void testA() {
            throw new NullPointerException("thrown on purpose");
        }
    }

    public static class ValidMethodTestCase extends TestCase {
        public ValidMethodTestCase(String name) {
            super(name);
        }
        public void testA() {
            // expected to be executed
        }
        public void testB() {
            // should not be executed
            throw new NullPointerException("thrown on purpose");
        }
    }

    public static class InvalidTestCase extends TestCase {
        public InvalidTestCase(String name) {
            super(name);
            throw new NullPointerException("thrown on purpose");
        }
    }

    public static class NoSuiteTestCase extends TestCase {
        public NoSuiteTestCase(String name) {
            super(name);
        }
        public void testA() {
        }
    }

    public static class SuiteTestCase extends NoSuiteTestCase {
        public SuiteTestCase(String name) {
            super(name);
        }
        public static junit.framework.Test suite() {
            return new TestSuite(SuiteTestCase.class);
        }
    }

    public static class InvalidSuiteTestCase extends NoSuiteTestCase {
        public InvalidSuiteTestCase(String name) {
            super(name);
        }
        public static junit.framework.Test suite() {
            throw new NullPointerException("thrown on purpose");
        }
    }

    public static class StaticInitializerErrorTestCase extends TestCase {
        static {
            error();
        }
        private static void error() {
            throw new NullPointerException("thrown on purpose");
        }
        public void testA() {
        }
    }

    public static class AssertionErrorTest {
        @Test
        public void throwsAssertionError() {
            throw new AssertionError("failure message", new RuntimeException("cause message"));
        }
    }
}
