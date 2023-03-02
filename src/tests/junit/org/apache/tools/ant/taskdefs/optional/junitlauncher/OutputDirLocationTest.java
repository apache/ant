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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.tools.ant.BuildFileRule;
import org.example.junitlauncher.jupiter.PassingTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.example.junitlauncher.Tracker.verifySuccess;
import static org.example.junitlauncher.Tracker.wasTestRun;

/**
 * Tests that the outputDir property used by the junitlauncher task's elements resolve against
 * the basedir of the project.
 */
public class OutputDirLocationTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/junitlauncher-outputdir.xml");
    }

    /**
     * basedir of the project is a different directory than the one which contains the project build
     * file. A target which uses junitlauncher task which runs in-vm tests and uses a outputDir,
     * is launched. The test verifies that the outputDir resolves against the basedir of the project
     *
     * @see "Bugzilla issue 66504"
     */
    @Test
    public void testJunitLauncherReportDir() throws Exception {
        final String targetName = "test-report-dir";
        final Path trackerFile = setupTrackerProperty(targetName);
        buildRule.executeTarget(targetName);
        // make sure the right test(s) were run
        Assert.assertTrue("PassingTest test was expected to be run",
                wasTestRun(trackerFile, PassingTest.class.getName()));
        Assert.assertTrue("PassingTest#testSucceeds was expected to succeed",
                verifySuccess(trackerFile, PassingTest.class.getName(), "testSucceeds"));
    }

    /**
     * basedir of the project is a different directory than the one which contains the project build
     * file. A target which uses junitlauncher task which runs forked tests and uses a outputDir,
     * is launched. The test verifies that the outputDir resolves against the basedir of the project.
     *
     * @see "Bugzilla issue 66504"
     */
    @Test
    public void testForkedJunitLauncherReportDir() throws Exception {
        final String targetName = "test-report-dir-fork";
        final Path trackerFile = setupTrackerProperty(targetName);
        buildRule.executeTarget(targetName);
        // make sure the right test(s) were run
        Assert.assertTrue("PassingTest test was expected to be run",
                wasTestRun(trackerFile, PassingTest.class.getName()));
        Assert.assertTrue("PassingTest#testSucceeds was expected to succeed",
                verifySuccess(trackerFile, PassingTest.class.getName(), "testSucceeds"));
    }

    private Path setupTrackerProperty(final String targetName) {
        final String filename = targetName + "-tracker.txt";
        buildRule.getProject().setProperty(targetName + ".tracker", filename);
        final String outputDir = buildRule.getProject().getProperty("report-dir");
        final File resolvedOutputDir = buildRule.getProject().resolveFile(outputDir);
        return Paths.get(resolvedOutputDir.toString(), filename);
    }
}
