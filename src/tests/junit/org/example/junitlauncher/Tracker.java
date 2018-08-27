/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.example.junitlauncher;

import org.apache.tools.ant.taskdefs.optional.junitlauncher.TestExecutionContext;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.TestResultFormatter;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Utility listener used in {@link org.apache.tools.ant.taskdefs.optional.junitlauncher.JUnitLauncherTaskTest}
 * to verify that tests launched through junitlauncher task are executed as expected
 */
public class Tracker implements TestResultFormatter {

    private static final String PREFIX_TEST_CLASS_STARTED = "started:test-class:";
    private static final String PREFIX_TEST_METHOD_STARTED = "started:test-method:";
    private static final String PREFIX_TEST_CLASS_SKIPPED = "skipped:test-class:";
    private static final String PREFIX_TEST_METHOD_SKIPPED = "skipped:test-method:";

    private PrintWriter writer;
    private TestExecutionContext context;

    @Override
    public void setDestination(final OutputStream os) {
        this.writer = new PrintWriter(os, true);
    }

    @Override
    public void setContext(final TestExecutionContext context) {
        this.context = context;
    }

    @Override
    public void close() throws IOException {
        this.writer.flush();
    }

    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        testIdentifier.getSource().ifPresent((s) -> {
            if (s instanceof MethodSource) {
                writer.println(PREFIX_TEST_METHOD_STARTED + ((MethodSource) s).getClassName()
                        + "#" + ((MethodSource) s).getMethodName());
                return;
            }
            if (s instanceof ClassSource) {
                writer.println(PREFIX_TEST_CLASS_STARTED + ((ClassSource) s).getClassName());
                return;
            }

        });
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier, final TestExecutionResult testExecutionResult) {
        testIdentifier.getSource().ifPresent((s) -> {
            if (s instanceof MethodSource) {
                writer.println(testExecutionResult.getStatus().name() + ":test-method:" + ((MethodSource) s).getClassName()
                        + "#" + ((MethodSource) s).getMethodName());
                return;
            }
            if (s instanceof ClassSource) {
                writer.println(testExecutionResult.getStatus().name() + ":test-class:" + ((ClassSource) s).getClassName());
                return;
            }

        });
    }

    @Override
    public void executionSkipped(final TestIdentifier testIdentifier, final String reason) {
        testIdentifier.getSource().ifPresent((s) -> {
            if (s instanceof MethodSource) {
                writer.println(PREFIX_TEST_METHOD_SKIPPED + ((MethodSource) s).getClassName() + "#"
                        + ((MethodSource) s).getMethodName());
                return;
            }
            if (s instanceof ClassSource) {
                writer.println(PREFIX_TEST_CLASS_SKIPPED + ((ClassSource) s).getClassName());
                return;
            }

        });
    }

    public static boolean wasTestRun(final Path trackerFile, final String className) throws IOException {
        final List<String> lines = readTrackerFile(trackerFile);
        return lines.contains(PREFIX_TEST_CLASS_STARTED + className);
    }

    public static boolean wasTestRun(final Path trackerFile, final String className, final String methodName) throws IOException {
        final List<String> lines = readTrackerFile(trackerFile);
        return lines.contains(PREFIX_TEST_METHOD_STARTED + className + "#" + methodName);
    }

    public static boolean verifyFailed(final Path trackerFile, final String className, final String methodName) throws IOException {
        final List<String> lines = readTrackerFile(trackerFile);
        return lines.contains(TestExecutionResult.Status.FAILED + ":test-method:" + className + "#" + methodName);
    }

    public static boolean verifySuccess(final Path trackerFile, final String className, final String methodName) throws IOException {
        final List<String> lines = readTrackerFile(trackerFile);
        return lines.contains(TestExecutionResult.Status.SUCCESSFUL + ":test-method:" + className + "#" + methodName);
    }

    public static boolean verifySkipped(final Path trackerFile, final String className, final String methodName) throws IOException {
        final List<String> lines = readTrackerFile(trackerFile);
        return lines.contains(PREFIX_TEST_METHOD_SKIPPED + className + "#" + methodName);
    }

    private static List<String> readTrackerFile(final Path trackerFile) throws IOException {
        if (!Files.isRegularFile(trackerFile)) {
            throw new RuntimeException(trackerFile + " is either missing or not a file");
        }
        return Files.readAllLines(trackerFile);
    }
}
