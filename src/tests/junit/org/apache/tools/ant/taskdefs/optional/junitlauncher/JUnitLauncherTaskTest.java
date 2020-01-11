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

import static org.example.junitlauncher.Tracker.verifyFailed;
import static org.example.junitlauncher.Tracker.verifySetupFailed;
import static org.example.junitlauncher.Tracker.verifySkipped;
import static org.example.junitlauncher.Tracker.verifySuccess;
import static org.example.junitlauncher.Tracker.wasTestRun;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.JUnitLauncherTask;
import org.apache.tools.ant.util.LoaderUtils;
import org.example.junitlauncher.jupiter.JupiterSampleTest;
import org.example.junitlauncher.jupiter.JupiterSampleTestFailingBeforeAll;
import org.example.junitlauncher.jupiter.JupiterTagSampleTest;
import org.example.junitlauncher.vintage.AlwaysFailingJUnit4Test;
import org.example.junitlauncher.vintage.ForkedTest;
import org.example.junitlauncher.vintage.JUnit4SampleTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the {@link JUnitLauncherTask}
 */
public class JUnitLauncherTaskTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    /**
     * The JUnit setup method.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/junitlauncher.xml");
    }

    /**
     * Tests that when a test, that's configured with {@code haltOnFailure=true}, stops the build, when the
     * test fails
     */
    @Test
    public void testFailureStopsBuild() throws Exception {
        final String targetName = "test-failure-stops-build";
        final Path trackerFile = setupTrackerProperty(targetName);
        try {
            buildRule.executeTarget(targetName);
            Assert.fail(targetName + " was expected to fail");
        } catch (BuildException e) {
            // expected, but do further tests to make sure the build failed for expected reason
            if (!verifyFailed(trackerFile, AlwaysFailingJUnit4Test.class.getName(),
                    "testWillFail")) {
                // throw back the original cause
                throw e;
            }
        }
    }
    
    /**
     * Tests that when a test, that's isn't configured with {@code haltOnFailure=true}, continues the
     * build even when there are test failures
     */
    @Test
    public void testFailureContinuesBuild() throws Exception {
        final String targetName = "test-failure-continues-build";
        final Path trackerFile = setupTrackerProperty(targetName);
        buildRule.executeTarget(targetName);
        // make sure the test that was expected to be run (and fail), did indeed fail
        Assert.assertTrue("AlwaysFailingJUnit4Test#testWillFail was expected to run", wasTestRun(trackerFile,
                AlwaysFailingJUnit4Test.class.getName(), "testWillFail"));
        Assert.assertTrue("AlwaysFailingJUnit4Test#testWillFail was expected to fail", verifyFailed(trackerFile,
                AlwaysFailingJUnit4Test.class.getName(), "testWillFail"));
    }

    /**
     * Tests the execution of test that's expected to succeed
     */
    @Test
    public void testSuccessfulTests() throws Exception {
        final String targetName = "test-success";
        final Path trackerFile = setupTrackerProperty(targetName);
        buildRule.executeTarget(targetName);
        // make sure the right test(s) were run
        Assert.assertTrue("JUnit4SampleTest test was expected to be run", wasTestRun(trackerFile, JUnit4SampleTest.class.getName()));
        Assert.assertTrue("JUnit4SampleTest#testFoo was expected to succeed", verifySuccess(trackerFile,
                JUnit4SampleTest.class.getName(), "testFoo"));
    }

    /**
     * Tests execution of a test which is configured to execute only a particular set of test methods
     */
    @Test
    public void testSpecificMethodTest() throws Exception {
        final String targetSpecificMethod = "test-one-specific-method";
        final Path tracker1 = setupTrackerProperty(targetSpecificMethod);
        buildRule.executeTarget(targetSpecificMethod);
        // verify only that specific method was run
        Assert.assertTrue("testBar was expected to be run", wasTestRun(tracker1, JUnit4SampleTest.class.getName(),
                "testBar"));
        Assert.assertFalse("testFoo wasn't expected to be run", wasTestRun(tracker1, JUnit4SampleTest.class.getName(),
                "testFoo"));


        final String targetMultipleMethods = "test-multiple-specific-methods";
        final Path tracker2 = setupTrackerProperty(targetMultipleMethods);
        buildRule.executeTarget(targetMultipleMethods);
        Assert.assertTrue("testFooBar was expected to be run", wasTestRun(tracker2, JUnit4SampleTest.class.getName(),
                "testFooBar"));
        Assert.assertTrue("testFoo was expected to be run", wasTestRun(tracker2, JUnit4SampleTest.class.getName(),
                "testFoo"));
        Assert.assertFalse("testBar wasn't expected to be run", wasTestRun(tracker2, JUnit4SampleTest.class.getName(),
                "testBar"));
    }

    /**
     * Tests the execution of more than one {@code &lt;test&gt;} elements in the {@code &lt;junitlauncher&gt;} task
     */
    @Test
    public void testMultipleIndividualTests() throws Exception {
        final String targetName = "test-multiple-individual";
        final Path trackerFile1 = setupTrackerProperty(targetName + "-1");
        final Path trackerFile2 = setupTrackerProperty(targetName + "-2");
        buildRule.executeTarget(targetName);

        Assert.assertTrue("AlwaysFailingJUnit4Test#testWillFail was expected to be run", wasTestRun(trackerFile1,
                AlwaysFailingJUnit4Test.class.getName(), "testWillFail"));
        Assert.assertTrue("JUnit4SampleTest#testFoo was expected to be run", wasTestRun(trackerFile2,
                JUnit4SampleTest.class.getName(), "testFoo"));
    }

    /**
     * Tests execution of tests, that have been configured using the {@code &lt;testclasses&gt;} nested element
     * of the {@code &lt;junitlauncher&gt;} task
     */
    @Test
    public void testTestClasses() throws Exception {
        final String targetName = "test-batch";
        final Path trackerFile = setupTrackerProperty(targetName);
        buildRule.executeTarget(targetName);

        Assert.assertTrue("JUnit4SampleTest#testFoo was expected to succeed", verifySuccess(trackerFile,
                JUnit4SampleTest.class.getName(), "testFoo"));
        Assert.assertTrue("AlwaysFailingJUnit4Test#testWillFail was expected to fail", verifyFailed(trackerFile,
                AlwaysFailingJUnit4Test.class.getName(), "testWillFail"));
        Assert.assertTrue("JupiterSampleTest#testSucceeds was expected to succeed", verifySuccess(trackerFile,
                JupiterSampleTest.class.getName(), "testSucceeds"));
        Assert.assertTrue("JupiterSampleTest#testFails was expected to succeed", verifyFailed(trackerFile,
                JupiterSampleTest.class.getName(), "testFails"));
        Assert.assertTrue("JupiterSampleTest#testSkipped was expected to be skipped", verifySkipped(trackerFile,
                JupiterSampleTest.class.getName(), "testSkipped"));
        Assert.assertFalse("ForkedTest wasn't expected to be run", wasTestRun(trackerFile, ForkedTest.class.getName()));

        verifyLegacyXMLFile("TEST-org.example.junitlauncher.jupiter.JupiterSampleTestFailingBeforeAll.xml", "<failure message=\"Intentional failure\" type=\"java.lang.RuntimeException\">");
        verifyLegacyXMLFile("TEST-org.example.junitlauncher.jupiter.JupiterSampleTestFailingStatic.xml", "Caused by: java.lang.RuntimeException: Intentional exception from static init block");
    }

    private void verifyLegacyXMLFile(final String fileName, final String expectedContentExtract) throws IOException {
        final String outputDir = buildRule.getProject().getProperty("output.dir");
        final Path xmlFile = Paths.get(outputDir, fileName);

        Assert.assertTrue("XML file doesn't exist: " + xmlFile, Files.exists(xmlFile));
        final String content = new String(Files.readAllBytes(xmlFile), StandardCharsets.UTF_8);
        Assert.assertTrue(fileName + " doesn't contain " + expectedContentExtract, content.contains(expectedContentExtract));
    }

    /**
     * Tests the execution of a forked test
     */
    @Test
    public void testBasicFork() throws Exception {
        final String targetName = "test-basic-fork";
        final Path trackerFile = setupTrackerProperty(targetName);
        // setup a dummy and incorrect value of a sysproperty that's used in the test
        // being forked
        System.setProperty(ForkedTest.SYS_PROP_ONE, "dummy");
        buildRule.executeTarget(targetName);
        // verify that our JVM's sysprop value didn't get changed
        Assert.assertEquals("System property " + ForkedTest.SYS_PROP_ONE + " was unexpected updated",
                "dummy", System.getProperty(ForkedTest.SYS_PROP_ONE));

        Assert.assertTrue("ForkedTest#testSysProp was expected to succeed", verifySuccess(trackerFile,
                ForkedTest.class.getName(), "testSysProp"));
    }

    /**
     * Tests that in a forked mode execution of tests, when the {@code includeJUnitPlatformLibraries} attribute
     * is set to false, then the execution of such tests fails with a classloading error for the JUnit platform
     * classes
     *
     * @throws Exception
     */
    @Test
    public void testExcludeJUnitPlatformLibs() throws Exception {
        final String targetName = "test-junit-platform-lib-excluded";
        try {
            buildRule.executeTarget(targetName);
            Assert.fail(targetName + " was expected to fail since JUnit platform libraries " +
                    "weren't included in the classpath of the forked JVM");
        } catch (BuildException be) {
            // expect a ClassNotFoundException for a JUnit platform class
            final String cnfeMessage = ClassNotFoundException.class.getName() + ": org.junit.platform";
            if (!buildRule.getFullLog().contains(cnfeMessage)) {
                throw be;
            }
        }
        final String exclusionLogMsg = "Excluding JUnit platform libraries";
        Assert.assertTrue("JUnit platform libraries weren't excluded from classpath", buildRule.getFullLog().contains(exclusionLogMsg));
    }

    /**
     * Tests that in a forked mode execution of tests, when the {@code includeAntRuntimeLibraries} attribute
     * is set to false, then the execution of such tests fails with a classloading error for the Ant runtime
     * classes
     *
     * @throws Exception
     */
    @Test
    public void testExcludeAntRuntimeLibs() throws Exception {
        final String targetName = "test-junit-ant-runtime-lib-excluded";
        try {
            buildRule.executeTarget(targetName);
            Assert.fail(targetName + " was expected to fail since Ant runtime libraries " +
                    "weren't included in the classpath of the forked JVM");
        } catch (BuildException be) {
            // expect a Error due to missing main class (which is part of Ant runtime libraries
            // that we excluded)
            final String missingMainClass = "Could not find or load main class " + StandaloneLauncher.class.getName();
            if (!buildRule.getFullLog().contains(missingMainClass)) {
                throw be;
            }
        }
        final String exclusionLogMsg = "Excluding Ant runtime libraries";
        Assert.assertTrue("Ant runtime libraries weren't excluded from classpath", buildRule.getFullLog().contains(exclusionLogMsg));
    }


    /**
     * Tests that in a forked mode execution, with {@code includeJUnitPlatformLibraries} attribute set to false
     * and with the test classpath explicitly including JUnit platform library jars, the tests are executed successfully
     *
     * @throws Exception
     */
    @Test
    public void testJUnitPlatformLibsCustomLocation() throws Exception {
        final String targetName = "test-junit-platform-lib-custom-location";
        final Path trackerFile = setupTrackerProperty(targetName);
        buildRule.executeTarget(targetName);
        final String exclusionLogMsg = "Excluding JUnit platform libraries";
        Assert.assertTrue("JUnit platform libraries weren't excluded from classpath", buildRule.getFullLog().contains(exclusionLogMsg));
        Assert.assertTrue("JupiterSampleTest#testSucceeds was expected to succeed", verifySuccess(trackerFile,
                JupiterSampleTest.class.getName(), "testSucceeds"));
        Assert.assertTrue("JupiterSampleTest#testFails was expected to fail", verifyFailed(trackerFile,
                JupiterSampleTest.class.getName(), "testFails"));
    }

    /**
     * Tests that in a forked mode execution, with {@code includeAntRuntimeLibraries} attribute set to false
     * and with the test classpath explicitly including Ant runtime library jars, the tests are executed successfully
     *
     * @throws Exception
     */
    @Test
    public void testAntRuntimeLibsCustomLocation() throws Exception {
        final String targetName = "test-ant-runtime-lib-custom-location";
        final Path trackerFile = setupTrackerProperty(targetName);
        setupRuntimeClassesProperty();
        // run the target
        buildRule.executeTarget(targetName);
        final String exclusionLogMsg = "Excluding Ant runtime libraries";
        Assert.assertTrue("Ant runtime libraries weren't excluded from classpath", buildRule.getFullLog().contains(exclusionLogMsg));
        Assert.assertTrue("JupiterSampleTest#testSucceeds was expected to succeed", verifySuccess(trackerFile,
                JupiterSampleTest.class.getName(), "testSucceeds"));
        Assert.assertTrue("JupiterSampleTest#testFails was expected to fail", verifyFailed(trackerFile,
                JupiterSampleTest.class.getName(), "testFails"));
        Assert.assertTrue("AlwaysFailingJUnit4Test#testWillFail was expected to fail", verifyFailed(trackerFile,
                AlwaysFailingJUnit4Test.class.getName(), "testWillFail"));
        Assert.assertTrue("ForkedTest#testSysProp was expected to succeed", verifySuccess(trackerFile,
                ForkedTest.class.getName(), "testSysProp"));


    }

    /**
     * Tests that in a forked mode execution, with {@code includeAntRuntimeLibraries} and {@code includeJUnitPlatformLibraries}
     * attributes set to false and with the test classpath explicitly including Ant runtime and JUnit platform library jars,
     * the tests are executed successfully
     *
     * @throws Exception
     */
    @Test
    public void testAntAndJUnitPlatformLibsCustomLocation() throws Exception {
        final String targetName = "test-ant-and-junit-platform-lib-custom-location";
        final Path trackerFile = setupTrackerProperty(targetName);
        setupRuntimeClassesProperty();
        // run the target
        buildRule.executeTarget(targetName);

        Assert.assertTrue("Ant runtime libraries weren't excluded from classpath",
                buildRule.getFullLog().contains("Excluding Ant runtime libraries"));
        Assert.assertTrue("JUnit platform libraries weren't excluded from classpath",
                buildRule.getFullLog().contains("Excluding JUnit platform libraries"));

        Assert.assertTrue("JUnit4SampleTest#testBar was expected to pass", verifySuccess(trackerFile,
                JUnit4SampleTest.class.getName(), "testBar"));
    }

    /**
     * Tests execution of a test which is configured to execute only methods with a special tag
     */
    @Test
    public void testMethodWithIncludeTag() throws Exception {
        final String target = "test-method-with-include-tag";
        final Path tracker2 = setupTrackerProperty(target);
        buildRule.executeTarget(target);
        // verify only that specific method was run
        Assert.assertTrue("testMethodIncludeTagisExecuted was expected to be run", wasTestRun(tracker2, JupiterSampleTest.class.getName(),
                "testMethodIncludeTagisExecuted"));
        Assert.assertFalse("testMethodIncludeTagisNotExecuted was expected NOT to be run", wasTestRun(tracker2, JupiterSampleTest.class.getName(),
                "testMethodIncludeTagisNotExecuted"));
    }

    /**
     * Tests execution of a test which is configured to execute only methods without special tags
     */
    @Test
    public void testMethodWithExcludeTag() throws Exception {
        final String target = "test-method-with-exclude-tag";
        final Path tracker2 = setupTrackerProperty(target);
        buildRule.executeTarget(target);
        // verify only that specific method was run
        Assert.assertTrue("testMethodIncludeTagisExecuted was expected to be run", wasTestRun(tracker2, JupiterSampleTest.class.getName(),
                "testMethodIncludeTagisExecuted"));
        Assert.assertFalse("testMethodIncludeTagisNotExecuted was expected NOT to be run", wasTestRun(tracker2, JupiterSampleTest.class.getName(),
                "testMethodIncludeTagisNotExecuted"));
    }

    /**
     * Tests execution of a test which is configured to execute only methods with special tags, two classes specified
     */
    @Test
    public void testMethodWithTag2Classes() throws Exception {
        final String target = "test-method-with-tag-2-classes";
        final Path tracker1 = setupTrackerProperty(target + "1");

        final Path tracker2 = setupTrackerProperty(target + "2");

        buildRule.executeTarget(target);
        // verify only that specific method was run
        Assert.assertTrue("testMethodIncludeTagisExecuted was expected to be run", wasTestRun(tracker1, JupiterSampleTest.class.getName(),
                "testMethodIncludeTagisExecuted"));
        Assert.assertFalse("testMethodIncludeTagisNotExecuted was expected NOT to be run", wasTestRun(tracker1, JupiterSampleTest.class.getName(),
                "testMethodIncludeTagisNotExecuted"));
        Assert.assertTrue("testMethodIncludeTagisExecutedTagSampleTest was expected to be run", wasTestRun(tracker2, JupiterTagSampleTest.class.getName(),
                "testMethodIncludeTagisExecutedTagSampleTest"));
        Assert.assertFalse("testMethodIncludeTagisNotExecutedTagSampleTest was expected NOT to be run", wasTestRun(tracker2, JupiterTagSampleTest.class.getName(),
                "testMethodIncludeTagisNotExecutedTagSampleTest"));
        Assert.assertFalse("testMethodIncludeTagisNotExecutedTagSampleTest2 was expected NOT to be run", wasTestRun(tracker2, JupiterTagSampleTest.class.getName(),
                "testMethodIncludeTagisNotExecutedTagSampleTest2"));
    }
    

    /**
     * Tests that failure at with beforeall stops the build 
     */
    @Test
    public void testBeforeAllFailureStopsBuild() throws Exception {
        final String targetName = "test-beforeall-failure-stops-build";
        final Path trackerFile = setupTrackerProperty(targetName);
        try {
            buildRule.executeTarget(targetName);
            Assert.fail(targetName + " was expected to fail");
        } catch (BuildException e) {
            // expected, but do further tests to make sure the build failed for expected reason
            if (!verifySetupFailed(trackerFile, JupiterSampleTestFailingBeforeAll.class.getName())) {
                // throw back the original cause
                throw e;
            }
        }
    }
    
    /**
     * Tests that when a test, that's isn't configured with {@code haltOnFailure=true}, continues the
     * build even when there are test failures
     */
    @Test
    public void testBeforeAllFailureContinuesBuild() throws Exception {
        final String targetName = "test-beforeall-failure-continues-build";
        final Path trackerFile = setupTrackerProperty(targetName);
        buildRule.executeTarget(targetName);
        Assert.assertTrue("Expected @BeforeAll failure to lead to failing testcase", verifySetupFailed(trackerFile, JupiterSampleTestFailingBeforeAll.class.getName()));
    }


    /**
     * Tests execution of a test which is configured to execute only methods with special tags, two classes specified
     */
    @Test
    public void testMethodWithTagFileSet() throws Exception {
        final String target = "test-method-with-tag-fileset";
        final Path tracker = setupTrackerProperty(target);

        buildRule.executeTarget(target);
        // verify only that specific method was run
        Assert.assertTrue("testMethodIncludeTagisExecuted was expected to be run", wasTestRun(tracker, JupiterSampleTest.class.getName(),
                "testMethodIncludeTagisExecuted"));
        Assert.assertFalse("testMethodIncludeTagisNotExecuted was expected NOT to be run", wasTestRun(tracker, JupiterSampleTest.class.getName(),
                "testMethodIncludeTagisNotExecuted"));
        Assert.assertTrue("testMethodIncludeTagisExecutedTagSampleTest was expected to be run", wasTestRun(tracker, JupiterTagSampleTest.class.getName(),
                "testMethodIncludeTagisExecutedTagSampleTest"));
        Assert.assertFalse("testMethodIncludeTagisNotExecutedTagSampleTest was expected NOT to be run", wasTestRun(tracker, JupiterTagSampleTest.class.getName(),
                "testMethodIncludeTagisNotExecutedTagSampleTest"));
        Assert.assertFalse("testMethodIncludeTagisNotExecutedTagSampleTest2 was expected NOT to be run", wasTestRun(tracker, JupiterTagSampleTest.class.getName(),
                "testMethodIncludeTagisNotExecutedTagSampleTest2"));
    }

    /**
     * Tests execution of a test which is configured to execute only methods with special tags, two classes specified
     */
    @Test
    public void testMethodWithTagFileSetFork() throws Exception {
        final String target = "test-method-with-tag-fileset-fork";
        final Path tracker = setupTrackerProperty(target);

        buildRule.executeTarget(target);

        Assert.assertTrue("testMethodIncludeTagisExecuted was expected to be run", wasTestRun(tracker, JupiterSampleTest.class.getName(),
                "testMethodIncludeTagisExecuted"));
        Assert.assertFalse("testMethodIncludeTagisNotExecuted was expected NOT to be run", wasTestRun(tracker, JupiterSampleTest.class.getName(),
                "testMethodIncludeTagisNotExecuted"));
        Assert.assertTrue("testMethodIncludeTagisExecutedTagSampleTest was expected to be run", wasTestRun(tracker, JupiterTagSampleTest.class.getName(),
                "testMethodIncludeTagisExecutedTagSampleTest"));
        Assert.assertFalse("testMethodIncludeTagisNotExecutedTagSampleTest was expected NOT to be run", wasTestRun(tracker, JupiterTagSampleTest.class.getName(),
                "testMethodIncludeTagisNotExecutedTagSampleTest"));
        Assert.assertFalse("testMethodIncludeTagisNotExecutedTagSampleTest2 was expected NOT to be run", wasTestRun(tracker, JupiterTagSampleTest.class.getName(),
                "testMethodIncludeTagisNotExecutedTagSampleTest2"));

        // Do it in the test, cause otherwise the file will be too big
        Files.deleteIfExists(tracker);
    }


    /**
     * Tests that the forked test works fine when the {@code testclasses} element is used
     * as a sibling of a {@code listener} element
     */
    @Test
    public void testBz63958() throws Exception {
        final String targetName = "bz-63958";
        final Path trackerFile = setupTrackerProperty(targetName);
        buildRule.executeTarget(targetName);

        Assert.assertTrue("ForkedTest#testSysProp was expected to succeed", verifySuccess(trackerFile,
                ForkedTest.class.getName(), "testSysProp"));
    }

    private Path setupTrackerProperty(final String targetName) {
        final String filename = targetName + "-tracker.txt";
        buildRule.getProject().setProperty(targetName + ".tracker", filename);
        final String outputDir = buildRule.getProject().getProperty("output.dir");
        return Paths.get(outputDir, filename);
    }

    private void setupRuntimeClassesProperty() {
        // setup a property that points to the locations of Ant runtime classes.
        // this path will then be used in target to create a duplicate copied
        // classes and then will be used as a custom location for Ant runtime libraries
        final String projectResourceName = LoaderUtils.classNameToResource(Project.class.getName());
        final File antClassesPath = LoaderUtils.getResourceSource(Project.class.getClassLoader(), projectResourceName);
        buildRule.getProject().setProperty("ant.runtime.classes.original.path", antClassesPath.getAbsolutePath());
    }
}
