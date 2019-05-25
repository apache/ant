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

import junit.framework.JUnit4TestAdapter;
import junit.framework.JUnit4TestAdapterCache;
import junit.framework.TestResult;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

/**
 * Provides a custom implementation of the notifier for a JUnit4TestAdapter
 * so that skipped and ignored tests can be reported to the existing
 * <code>TestListener</code>s.
 *
 */
public class CustomJUnit4TestAdapterCache extends JUnit4TestAdapterCache {
    private static final long serialVersionUID = 1L;
    private static final CustomJUnit4TestAdapterCache INSTANCE = new CustomJUnit4TestAdapterCache();

    public static CustomJUnit4TestAdapterCache getInstance() {
        return INSTANCE;
    }

    private CustomJUnit4TestAdapterCache() {
        super();
    }

    @Override
    public RunNotifier getNotifier(final TestResult result, final JUnit4TestAdapter adapter) {
        return getNotifier(result);
    }

    public RunNotifier getNotifier(final TestResult result) {

        final IgnoredTestResult resultWrapper = (IgnoredTestResult) result;

        RunNotifier notifier = new RunNotifier();
        notifier.addListener(new RunListener() {
            @Override
            public void testFailure(Failure failure) throws Exception {
                result.addError(asTest(failure.getDescription()), failure.getException());
            }

            @Override
            public void testFinished(Description description) throws Exception {
                result.endTest(asTest(description));
            }

            @Override
            public void testStarted(Description description) throws Exception {
                result.startTest(asTest(description));
            }

            @Override
            public void testIgnored(Description description) throws Exception {
                if (resultWrapper != null) {
                    resultWrapper.testIgnored(asTest(description));
                }
            }

            @Override
            public void testAssumptionFailure(Failure failure) {
                if (resultWrapper != null) {
                    resultWrapper.testAssumptionFailure(asTest(failure.getDescription()), failure.getException());
                }
            }
        });

        return notifier;
    }
}
