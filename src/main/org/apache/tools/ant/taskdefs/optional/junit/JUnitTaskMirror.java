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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.types.Permissions;

/**
 * Handles the portions of {@link JUnitTask} which need to directly access
 * actual JUnit classes, so that junit.jar need not be on Ant's startup classpath.
 * Neither JUnitTask.java nor JUnitTaskMirror.java nor their transitive static
 * deps may import any junit.** classes!
 * Specifically, need to not refer to
 * - JUnitResultFormatter or its subclasses
 * - JUnitVersionHelper
 * - JUnitTestRunner
 * Cf.  JUnitTask.SplitLoader#isSplit(String)
 * Public only to permit access from classes in this package; do not use directly.
 *
 * @since 1.7
 * @see "bug #38799"
 */
public interface JUnitTaskMirror {

    /**
     * Add the formatter to be called when the jvm exits before
     * the test suite finishes.
     * @param test the test.
     * @param formatter the formatter to use.
     * @param out the output stream to use.
     * @param message the message to write out.
     * @param testCase the name of the test.
     */
    void addVmExit(JUnitTest test, JUnitResultFormatterMirror formatter,
            OutputStream out, String message, String testCase);

    /**
     * Create a new test runner for a test.
     * @param test the test to run.
     * @param methods names of the test methods to be run.
     * @param haltOnError if true halt the tests if an error occurs.
     * @param filterTrace if true filter the stack traces.
     * @param haltOnFailure if true halt the test if a failure occurs.
     * @param showOutput    if true show output.
     * @param logTestListenerEvents if true log test listener events.
     * @param classLoader      the classloader to use to create the runner.
     * @return the test runner.
     */
    JUnitTestRunnerMirror newJUnitTestRunner(JUnitTest test, String[] methods, boolean haltOnError,
            boolean filterTrace, boolean haltOnFailure, boolean showOutput,
            boolean logTestListenerEvents, AntClassLoader classLoader);

    /**
     * Create a summary result formatter.
     * @return the created formatter.
     */
    SummaryJUnitResultFormatterMirror newSummaryJUnitResultFormatter();


    /** The interface that JUnitResultFormatter extends. */
    interface JUnitResultFormatterMirror {
        /**
         * Set the output stream.
         * @param outputStream the stream to use.
         */
        void setOutput(OutputStream outputStream);
    }

    /** The interface that SummaryJUnitResultFormatter extends. */
    interface SummaryJUnitResultFormatterMirror
        extends JUnitResultFormatterMirror {

        /**
         * Set where standard out and standard error should be included.
         * @param value if true include the outputs in the summary.
         */
        void setWithOutAndErr(boolean value);
    }

    /** Interface that test runners implement. */
    interface JUnitTestRunnerMirror {

        /**
         * Used in formatter arguments as a placeholder for the basename
         * of the output file (which gets replaced by a test specific
         * output file name later).
         *
         * @since Ant 1.6.3
         */
        String IGNORED_FILE_NAME = "IGNORETHIS";

        /**
         * No problems with this test.
         */
        int SUCCESS = 0;

        /**
         * Some tests failed.
         */
        int FAILURES = 1;

        /**
         * An error occurred.
         */
        int ERRORS = 2;

        /**
         * Permissions for the test run.
         * @param perm the permissions to use.
         */
        void setPermissions(Permissions perm);

        /** Run the test. */
        void run();

        /**
         * Add a formatter to the test.
         * @param formatter the formatter to use.
         */
        void addFormatter(JUnitResultFormatterMirror formatter);

        /**
         * Returns what System.exit() would return in the standalone version.
         *
         * @return 2 if errors occurred, 1 if tests failed else 0.
         */
        int getRetCode();

        /**
         * Handle output sent to System.err.
         *
         * @param output coming from System.err
         */
        void handleErrorFlush(String output);

        /**
         * Handle output sent to System.err.
         *
         * @param output output for System.err
         */
        void handleErrorOutput(String output);

        /**
         * Handle output sent to System.out.
         *
         * @param output output for System.out.
         */
        void handleOutput(String output);

        /**
         * Handle an input request.
         *
         * @param buffer the buffer into which data is to be read.
         * @param offset the offset into the buffer at which data is stored.
         * @param length the amount of data to read.
         *
         * @return the number of bytes read.
         *
         * @exception IOException if the data cannot be read.
         */
        int handleInput(byte[] buffer, int offset, int length) throws IOException;

        /**
         * Handle output sent to System.out.
         *
         * @param output output for System.out.
         */
       void handleFlush(String output);

    }
}
