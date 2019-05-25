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

import junit.framework.Test;
import junit.framework.TestListener;

/**
 * Provides the functionality for TestListeners to be able to be notified of
 * the necessary JUnit4 events for test being ignored (@Ignore annotation)
 * or skipped (Assume failures). Tests written in JUnit4 will report against
 * the methods in this interface alongside the methods in the existing TestListener
 */
public interface IgnoredTestListener extends TestListener {

    /**
     * Reports when a test has been marked with the @Ignore annotation. The parameter
     * should normally be typed to JUnit's {@link junit.framework.JUnit4TestCaseFacade}
     * so implementing classes should be able to get the details of the ignore by casting
     * the argument and retrieving the descriptor from the test.
     * @param test the details of the test and failure that have triggered this report.
     */
    void testIgnored(Test test);

    /**
     * Receive a report that a test has failed an assumption. Within JUnit4
     * this is normally treated as a test being skipped, although how any
     * listener handles this is up to that specific listener.
     * <p><b>Note:</b> Tests that throw assumption failures will still report
     * the endTest method, which may differ from how the addError and addFailure
     * methods work, it's up for any implementing classes to handle this.</p>
     * @param test the details of the test and failure that have triggered this report.
     * @param exception the AssumptionViolatedException thrown from the current assumption failure.
     */
    void testAssumptionFailure(Test test, Throwable exception);
}
