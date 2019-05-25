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

import junit.framework.TestListener;

import org.apache.tools.ant.BuildException;

/**
 * This Interface describes classes that format the results of a JUnit
 * testrun.
 *
 */
public interface JUnitResultFormatter
    extends TestListener, JUnitTaskMirror.JUnitResultFormatterMirror {
    /**
     * The whole testsuite started.
     * @param suite the suite.
     * @throws BuildException on error.
     */
    void startTestSuite(JUnitTest suite) throws BuildException;

    /**
     * The whole testsuite ended.
     * @param suite the suite.
     * @throws BuildException on error.
     */
    void endTestSuite(JUnitTest suite) throws BuildException;

    /**
     * Sets the stream the formatter is supposed to write its results to.
     * @param out the output stream to use.
     */
    void setOutput(OutputStream out);

    /**
     * This is what the test has written to System.out
     * @param out the string to write.
     */
    void setSystemOutput(String out);

    /**
     * This is what the test has written to System.err
     * @param err the string to write.
     */
    void setSystemError(String err);
}
