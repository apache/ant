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
package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import static org.example.junitlauncher.Tracker.wasTestRun;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.JUnitLauncherTask;
import org.apache.tools.ant.util.LoaderUtils;
import org.example.junitlauncher.jupiter.JupiterSampleTest;
import org.example.junitlauncher.jupiter.JupiterTagSampleTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the {@link JUnitLauncherTask}
 */
public class JUnitLauncherTaskWithTagTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    /**
     * The JUnit setup method.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/junitlauncherwithtag.xml");
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
        final Path tracker1 = setupTrackerProperty(target+"1");
                
        final Path tracker2 = setupTrackerProperty(target+"2");
        
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
