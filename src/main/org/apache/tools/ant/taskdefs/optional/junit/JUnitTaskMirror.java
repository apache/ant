/*
 * Copyright  2006 The Apache Software Foundation
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
 * Cf. {@link JUnitTask.SplitLoader#isSplit}
 * Public only to permit access from classes in this package; do not use directly.
 * 
 * @since 1.7
 * @see "bug #38799"
 */
public interface JUnitTaskMirror {
    
    void addVmExit(JUnitTest test, JUnitResultFormatterMirror formatter,
            OutputStream out, String message, String testCase);
    
    
    JUnitTestRunnerMirror newJUnitTestRunner(JUnitTest test, boolean haltOnError,
            boolean filterTrace, boolean haltOnFailure, boolean showOutput,
            boolean logTestListenerEvents, AntClassLoader classLoader);

    SummaryJUnitResultFormatterMirror newSummaryJUnitResultFormatter();

    public interface JUnitResultFormatterMirror {

        void setOutput(OutputStream outputStream);

    }

    public interface SummaryJUnitResultFormatterMirror extends JUnitResultFormatterMirror {

        void setWithOutAndErr(boolean value);

    }

    public interface JUnitTestRunnerMirror {

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

        void setPermissions(Permissions perm);
    
        void run();

        void addFormatter(JUnitResultFormatterMirror formatter);
    
        int getRetCode();
    
        void handleErrorFlush(String output);
    
        void handleErrorOutput(String output);
    
        void handleOutput(String output);
    
        int handleInput(byte[] buffer, int offset, int length) throws IOException;
    
        void handleFlush(String output);

    }
    
}
