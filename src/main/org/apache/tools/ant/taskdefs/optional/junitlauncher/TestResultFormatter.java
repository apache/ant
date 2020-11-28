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
package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.junit.platform.launcher.TestExecutionListener;

import java.io.Closeable;
import java.io.OutputStream;

/**
 * A {@code TestExecutionListener} which lets implementing classes
 * format and write out the test execution results.
 */
public interface TestResultFormatter extends TestExecutionListener, Closeable {

    /**
     * This method will be invoked by the <code>junitlauncher</code> and will be passed the
     * {@link OutputStream} to a file, to which the formatted result is expected to be written
     * to.
     * <p>
     * This method will be called once, early on, during the initialization of this
     * {@link TestResultFormatter}, typically before the test execution itself has started.
     * </p>
     *
     * @param os The output stream to which to write out the result
     */
    void setDestination(OutputStream os);

    /**
     * This method will be invoked by the <code>junitlauncher</code> and will be passed a
     * {@link TestExecutionContext}. This allows the {@link TestResultFormatter} to have access
     * to any additional contextual information to use in the test reports.
     *
     * @param context The context of the execution of the test
     */
    void setContext(TestExecutionContext context);

    /**
     * This method will be invoked by the {@code junitlauncher} to let the result formatter implementation
     * know whether or not to use JUnit 4 style, legacy reporting names for test identifiers that get
     * displayed in the test reports. Result formatter implementations are allowed to default to a specific
     * reporting style for test identifiers, if this method isn't invoked.
     * @param useLegacyReportingName {@code true} if legacy reporting name is to be used, {@code false}
     *                               otherwise.
     * @since Ant 1.10.10
     */
    void setUseLegacyReportingName(boolean useLegacyReportingName);

    /**
     * This method will be invoked by the <code>junitlauncher</code>, <strong>regularly/multiple times</strong>,
     * as and when any content is generated on the standard output stream during the test execution.
     * This method will be only be called if the <code>sendSysOut</code> attribute of the <code>listener</code>,
     * to which this {@link TestResultFormatter} is configured for, is enabled
     *
     * @param data The content generated on standard output stream
     */
    default void sysOutAvailable(byte[] data) {
    }

    /**
     * This method will be invoked by the <code>junitlauncher</code>, <strong>regularly/multiple times</strong>,
     * as and when any content is generated on the standard error stream during the test execution.
     * This method will be only be called if the <code>sendSysErr</code> attribute of the <code>listener</code>,
     * to which this {@link TestResultFormatter} is configured for, is enabled
     *
     * @param data The content generated on standard error stream
     */
    default void sysErrAvailable(byte[] data) {
    }

}
