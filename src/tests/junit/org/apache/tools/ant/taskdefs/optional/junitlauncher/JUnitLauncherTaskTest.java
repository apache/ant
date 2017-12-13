package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Tests the {@link JUnitLauncherTask}
 */
public class JUnitLauncherTaskTest {

    private Project project;

    /**
     * The JUnit setup method.
     */
    @Before
    public void setUp() {
        File antFile = new File(System.getProperty("root"), "src/etc/testcases/taskdefs/optional/junitlauncher.xml");
        this.project = new Project();
        this.project.init();
        ProjectHelper.configureProject(project, antFile);
        project.addBuildListener(new BuildListener() {
            @Override
            public void buildStarted(final BuildEvent event) {

            }

            @Override
            public void buildFinished(final BuildEvent event) {

            }

            @Override
            public void targetStarted(final BuildEvent event) {

            }

            @Override
            public void targetFinished(final BuildEvent event) {

            }

            @Override
            public void taskStarted(final BuildEvent event) {

            }

            @Override
            public void taskFinished(final BuildEvent event) {

            }

            @Override
            public void messageLogged(final BuildEvent event) {
                if (event.getPriority() <= Project.MSG_INFO) {
                    System.out.println(event.getMessage());
                }
            }
        });
    }

    /**
     * Tests that when a test, that's configured with {@code haltOnFailure=true}, stops the build, when the
     * test fails
     */
    @Test
    public void testFailureStopsBuild() {
        try {
            project.executeTarget("test-failure-stops-build");
            Assert.fail("Test execution failure was expected to stop the build but didn't");
        } catch (BuildException be) {
            // expected
        }
    }

    /**
     * Tests that when a test, that's isn't configured with {@code haltOnFailure=true}, continues the
     * build even when there are test failures
     */
    @Test
    public void testFailureContinuesBuild() {
        project.executeTarget("test-failure-continues-build");
    }


    /**
     * Tests the execution of test that's expected to succeed
     */
    @Test
    public void testSuccessfulTests() {
        project.executeTarget("test-success");
    }

    /**
     * Tests execution of a test which is configured to execute only a particular set of test methods
     */
    @Test
    public void testSpecificMethodTest() {
        project.executeTarget("test-one-specific-method");
        project.executeTarget("test-multiple-specific-methods");
    }

    /**
     * Tests the execution of more than one {@code &lt;test&gt;} elements in the {@code &lt;junitlauncher&gt;} task
     */
    @Test
    public void testMultipleIndividualTests() {
        project.executeTarget("test-multiple-individual");
    }

    /**
     * Tests execution of tests, that have been configured using the {@code &lt;testclasses&gt;} nested element
     * of the {@code &lt;junitlauncher&gt;} task
     */
    @Test
    public void testTestClasses() {
        project.executeTarget("test-batch");
    }
}
