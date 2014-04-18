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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.tools.ant.BuildException;
import org.junit.Test;

/**
 * Small testcase for the runner, tests are very very very basics.
 * They must be enhanced with time.
 *
 */
public class JUnitTestRunnerTest{

  

    // check that a valid method name generates no errors
    @Test
    public void testValidMethod(){
        TestRunner runner = createRunnerForTestMethod(ValidMethodTestCase.class,"testA");
        runner.run();
        assertEquals(runner.getFormatter().getError(), JUnitTestRunner.SUCCESS, runner.getRetCode());
    }

    // check that having an invalid method name generates an error
    @Test
    public void testInvalidMethod(){
        TestRunner runner = createRunnerForTestMethod(InvalidMethodTestCase.class,"testInvalid");
        runner.run();
        String error = runner.getFormatter().getError();
        // might be FAILURES or ERRORS depending on JUnit version?
        assertTrue(error, runner.getRetCode() != JUnitTestRunner.SUCCESS);
    }    
    
    // check that having no suite generates no errors
    @Test
    public void testNoSuite(){
        TestRunner runner = createRunner(NoSuiteTestCase.class);
        runner.run();
        assertEquals(runner.getFormatter().getError(), JUnitTestRunner.SUCCESS, runner.getRetCode());
    }

    // check that a suite generates no errors
    @Test
    public void testSuite(){
        TestRunner runner = createRunner(SuiteTestCase.class);
        runner.run();
        assertEquals(runner.getFormatter().getError(), JUnitTestRunner.SUCCESS, runner.getRetCode());
    }

    // check that an invalid suite generates an error.
    @Test
    public void testInvalidSuite(){
        TestRunner runner = createRunner(InvalidSuiteTestCase.class);
        runner.run();
        String error = runner.getFormatter().getError();
        assertEquals(error, JUnitTestRunner.ERRORS, runner.getRetCode());
        assertTrue(error, error.indexOf("thrown on purpose") != -1);
    }

    // check that something which is not a testcase generates no errors
    // at first even though this is incorrect.
    @Test
    public void testNoTestCase(){
        TestRunner runner = createRunner(NoTestCase.class);
        runner.run();
        // On junit3 this is a FAILURE, on junit4 this is an ERROR
        int ret = runner.getRetCode();
        
        if (ret != JUnitTestRunner.FAILURES && ret != JUnitTestRunner.ERRORS) {
            fail("Unexpected result " + ret + " from junit runner");
        }
        // JUnit3 test
        //assertEquals(runner.getFormatter().getError(), JUnitTestRunner.FAILURES, runner.getRetCode());
    }

    // check that an exception in the constructor is noticed
    @Test
    public void testInvalidTestCase(){
        TestRunner runner = createRunner(InvalidTestCase.class);
        runner.run();
        // On junit3 this is a FAILURE, on junit4 this is an ERROR
        int ret = runner.getRetCode();
        if (ret != JUnitTestRunner.FAILURES && ret != JUnitTestRunner.ERRORS) {
            fail("Unexpected result " + ret + " from junit runner");
        }
        // JUNIT3 test
        //assertEquals(error, JUnitTestRunner.FAILURES, runner.getRetCode());
        //@fixme as of now does not report the original stacktrace.
        //assertTrue(error, error.indexOf("thrown on purpose") != -1);
    }

    protected TestRunner createRunner(Class<?> clazz){
        return new TestRunner(new JUnitTest(clazz.getName()), null, 
                                            true, true, true);
    }

    protected TestRunner createRunnerForTestMethod(Class<?> clazz, String method){
        return new TestRunner(new JUnitTest(clazz.getName()), new String[] {method},
                                            true, true, true);
    }    
    
    // the test runner that wrap the dummy formatter that interests us
    private final static class TestRunner extends JUnitTestRunner {
        private ResultFormatter formatter = new ResultFormatter();
        TestRunner(JUnitTest test, String[] methods, boolean haltonerror,
                   boolean filtertrace, boolean haltonfailure){
            super(test, methods, haltonerror, filtertrace,  haltonfailure, 
                  false, false, TestRunner.class.getClassLoader());
            // use the classloader that loaded this class otherwise
            // it will not be able to run inner classes if this test
            // is ran in non-forked mode.
            addFormatter(formatter);
        }
        ResultFormatter getFormatter(){
            return formatter;
        }
    }

    // dummy formatter just to catch the error
    private final static class ResultFormatter implements JUnitResultFormatter {
        private Throwable error;
        public void setSystemOutput(String output){}
        public void setSystemError(String output){}
        public void startTestSuite(JUnitTest suite) throws BuildException{}
        public void endTestSuite(JUnitTest suite) throws BuildException{}
        public void setOutput(java.io.OutputStream out){}
        public void startTest(junit.framework.Test t) {}
        public void endTest(junit.framework.Test test) {}
        public void addFailure(junit.framework.Test test, AssertionFailedError t) { }
        public void addError(junit.framework.Test test, Throwable t) {
            error = t;
        }
        String getError(){
            if (error == null){
                return "";
            }
            StringWriter sw = new StringWriter();
            error.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }
    }

    public static class NoTestCase {
    }

    public static class InvalidMethodTestCase extends TestCase {
        public InvalidMethodTestCase(String name){ super(name); }
        public void testA(){
            throw new NullPointerException("thrown on purpose");
        }
    }

    public static class ValidMethodTestCase extends TestCase {
        public ValidMethodTestCase(String name){ super(name); }
        public void testA(){
            // expected to be executed
        }
        public void testB(){
            // should not be executed
            throw new NullPointerException("thrown on purpose");
        }
    }    
    
    public static class InvalidTestCase extends TestCase {
        public InvalidTestCase(String name){
            super(name);
            throw new NullPointerException("thrown on purpose");
        }
    }

    public static class NoSuiteTestCase extends TestCase {
        public NoSuiteTestCase(String name){ super(name); }
        public void testA(){}
    }

    public static class SuiteTestCase extends NoSuiteTestCase {
        public SuiteTestCase(String name){ super(name); }
        public static junit.framework.Test suite(){
            return new TestSuite(SuiteTestCase.class);
        }
    }

    public static class InvalidSuiteTestCase extends NoSuiteTestCase {
        public InvalidSuiteTestCase(String name){ super(name); }
        public static junit.framework.Test suite(){
            throw new NullPointerException("thrown on purpose");
        }
    }
}

