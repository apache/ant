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

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestResult;

import org.apache.tools.ant.AntClassLoader;

/**
 * Implementation of the part of the junit task which can directly refer to junit.* classes.
 * Public only to permit use of reflection; do not use directly.
 * @see JUnitTaskMirror
 * @see "bug #38799"
 * @since 1.7
 */
public final class JUnitTaskMirrorImpl implements JUnitTaskMirror {

    @SuppressWarnings("unused")
    private final JUnitTask task;

    /**
     * Constructor.
     * @param task the junittask that uses this mirror.
     */
    public JUnitTaskMirrorImpl(JUnitTask task) {
        this.task = task;
    }

    /** {@inheritDoc}. */
    @Override
    public void addVmExit(JUnitTest test, JUnitTaskMirror.JUnitResultFormatterMirror aFormatter,
            OutputStream out, String message, String testCase) {
        JUnitResultFormatter formatter = (JUnitResultFormatter) aFormatter;
        formatter.setOutput(out);
        formatter.startTestSuite(test);
        //the trick to integrating test output to the formatter, is to
        //create a special test class that asserts an error
        //and tell the formatter that it raised.
        TestCase t = new VmExitErrorTest(message, test, testCase);
        formatter.startTest(t);
        formatter.addError(t, new AssertionFailedError(message));
        formatter.endTestSuite(test);
    }

    /** {@inheritDoc}. */
    @Override
    public JUnitTaskMirror.JUnitTestRunnerMirror newJUnitTestRunner(JUnitTest test,
            String[] methods,
            boolean haltOnError, boolean filterTrace, boolean haltOnFailure,
            boolean showOutput, boolean logTestListenerEvents, AntClassLoader classLoader) {
        return new JUnitTestRunner(test, methods, haltOnError, filterTrace, haltOnFailure,
                showOutput, logTestListenerEvents, classLoader);
    }

    /** {@inheritDoc}. */
    @Override
    public JUnitTaskMirror.SummaryJUnitResultFormatterMirror newSummaryJUnitResultFormatter() {
        return new SummaryJUnitResultFormatter();
    }

    static class VmExitErrorTest extends TestCase {

        private String message;
        private JUnitTest test;
        private String testCase;

        VmExitErrorTest(String aMessage, JUnitTest anOriginalTest, String aTestCase) {
            message = aMessage;
            test = anOriginalTest;
            testCase = aTestCase;
        }

        @Override
        public int countTestCases() {
            return 1;
        }

        @Override
        public void run(TestResult r) {
            throw new AssertionFailedError(message);
        }

        @Override
        public String getName() {
            return testCase;
        }

        String getClassName() {
            return test.getName();
        }

        @Override
        public String toString() {
            return test.getName() + ":" + testCase;
        }
    }
}
