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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;

/**
 * Records ignored and skipped tests reported as part of the execution of
 * JUnit 4 tests.
 *
 */
public class IgnoredTestResult extends TestResult {


    private List<IgnoredTestListener> listeners = new ArrayList<>();
    private List<TestIgnored> ignored = new ArrayList<>();
    private List<TestIgnored> skipped = new ArrayList<>();

    public IgnoredTestResult() {
        super();
    }

    @Override
    public synchronized void addListener(TestListener listener) {
        if (listener instanceof IgnoredTestListener) {
            listeners.add((IgnoredTestListener) listener);
        }
        super.addListener(listener);
    }

    @Override
    public synchronized  void removeListener(TestListener listener) {
        if (listener instanceof IgnoredTestListener) {
            listeners.remove(listener);
        }
        super.removeListener(listener);
    }

    /**
     * Record a test as having been ignored, normally by the @Ignore annotation.
     * @param test the test that was ignored.
     * @throws Exception is the listener thrown an exception on handling the notification.
     */
    public synchronized void testIgnored(Test test) throws Exception {
        ignored.add(new TestIgnored(test));
        for (IgnoredTestListener listener : listeners) {
            listener.testIgnored(test);
        }
    }

    /**
     * Report how many tests were ignored.
     * @return the number of tests reported as ignored during the current execution.
     */
    public long ignoredCount() {
        return ignored.size();
    }

    /**
     * Records a test as having an assumption failure so JUnit will no longer be executing it.
     * Under normal circumstances this would be counted as a skipped test.
     * @param test the test to record
     * @param cause the details of the test and assumption failure.
     */
    public void testAssumptionFailure(Test test, Throwable cause) {
        skipped.add(new TestIgnored(test));
        for (IgnoredTestListener listener : listeners) {
            listener.testAssumptionFailure(test, cause);
        }
    }

    /**
     * Report how many tests has assumption failures.
     * @return the number of tests that reported assumption failures during the current execution.
     */
    public long skippedCount() {
        return skipped.size();
    }
}
