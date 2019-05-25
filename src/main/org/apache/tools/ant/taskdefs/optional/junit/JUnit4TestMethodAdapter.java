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

import java.util.List;
import java.util.function.Predicate;

import junit.framework.Test;
import junit.framework.TestResult;

import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;

/**
 * Adapter between JUnit 3.8.x API and JUnit 4.x API for execution of tests
 * and listening of events (test start, test finish, test failure, test skipped).
 * The constructor is passed a JUnit 4 test class and a list of name of methods
 * in it that should be executed. Method {@link #run run(TestResult)} executes
 * the given JUnit-4-style test methods and notifies the given {@code TestResult}
 * object using its old (JUnit 3.8.x style) API.
 *
 * @author  Marian Petras
 */
public class JUnit4TestMethodAdapter implements Test {

    private final Class<?> testClass;
    private final String[] methodNames;
    private final Runner runner;
    private final CustomJUnit4TestAdapterCache cache;

    /**
     * Creates a new adapter for the given class and a method within the class.
     *
     * @param testClass test class containing the method to be executed
     * @param methodNames names of the test methods that are to be executed
     * @exception  java.lang.IllegalArgumentException
     *             if any of the arguments is {@code null}
     *             or if any of the given method names is {@code null} or empty
     */
    public JUnit4TestMethodAdapter(final Class<?> testClass,
                                   final String[] methodNames) {
        if (testClass == null) {
            throw new IllegalArgumentException("testClass is <null>");
        }
        if (methodNames == null) {
            throw new IllegalArgumentException("methodNames is <null>");
        }
        for (int i = 0; i < methodNames.length; i++) {
            if (methodNames[i] == null) {
                throw new IllegalArgumentException("method name #" + i + " is <null>");
            }
            if (methodNames[i].isEmpty()) {
                throw new IllegalArgumentException("method name #" + i + " is empty");
            }
        }
        this.testClass = testClass;
        this.methodNames = methodNames.clone();
        this.cache = CustomJUnit4TestAdapterCache.getInstance();

        // Warning: If 'testClass' is an old-style (pre-JUnit-4) class,
        // then all its test methods will be executed by the returned runner!
        Request request;
        if (methodNames.length == 1) {
            request = Request.method(testClass, methodNames[0]);
        } else {
            request = Request.aClass(testClass).filterWith(
                            new MultipleMethodsFilter(testClass, methodNames));
        }
        runner = request.getRunner();
    }

    @Override
    public int countTestCases() {
        return runner.testCount();
    }

    public Description getDescription() {
        return runner.getDescription();
    }

    public List<Test> getTests() {
        return cache.asTestList(getDescription());
    }

    public Class<?> getTestClass() {
        return testClass;
    }

    @Override
    public void run(final TestResult result) {
        runner.run(cache.getNotifier(result));
    }

    @Override
    public String toString() {
        return ":" + String.join(",", methodNames);
    }

    private static final class MultipleMethodsFilter extends Filter {

        private final Description methodsListDescription;
        private final Class<?> testClass;
        private final String[] methodNames;

        private MultipleMethodsFilter(Class<?> testClass, String[] methodNames) {
            if (testClass == null) {
                throw new IllegalArgumentException("testClass is <null>");
            }
            if (methodNames == null) {
                throw new IllegalArgumentException("methodNames is <null>");
            }
            methodsListDescription = Description.createSuiteDescription(testClass);
            for (String methodName : methodNames) {
                methodsListDescription.addChild(
                        Description.createTestDescription(testClass, methodName));
            }
            this.testClass = testClass;
            this.methodNames = methodNames;
        }

        @Override
        public boolean shouldRun(Description description) {
            if (methodNames.length == 0) {
                return false;
            }
            if (description.isTest()) {
                return methodsListDescription.getChildren().stream()
                    .anyMatch(Predicate.isEqual(description));
            }
            return description.getChildren().stream().anyMatch(this::shouldRun);
        }

        @Override
        public String describe() {
            StringBuilder buf = new StringBuilder(40);
            if (methodNames.length == 0) {
                buf.append("No methods");
            } else {
                buf.append(methodNames.length == 1 ? "Method " : "Methods ");
                buf.append(String.join(",", methodNames));
            }
            buf.append('(').append(testClass.getName()).append(')');
            return buf.toString();
        }

    }

}
