/*
 * Copyright  2001-2002,2004 Apache Software Foundation
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

import java.io.OutputStream;
import junit.framework.TestListener;
import org.apache.tools.ant.BuildException;

/**
 * This Interface describes classes that format the results of a JUnit
 * testrun.
 *
 * @author Stefan Bodewig
 */
public interface JUnitResultFormatter extends TestListener {
    /**
     * The whole testsuite started.
     */
    void startTestSuite(JUnitTest suite) throws BuildException;

    /**
     * The whole testsuite ended.
     */
    void endTestSuite(JUnitTest suite) throws BuildException;

    /**
     * Sets the stream the formatter is supposed to write its results to.
     */
    void setOutput(OutputStream out);

    /**
     * This is what the test has written to System.out
     */
    void setSystemOutput(String out);

    /**
     * This is what the test has written to System.err
     */
    void setSystemError(String err);
}
