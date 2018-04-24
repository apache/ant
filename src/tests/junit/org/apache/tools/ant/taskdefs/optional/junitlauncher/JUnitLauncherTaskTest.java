package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
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
        buildRule.getProject().addBuildListener(new BuildListener() {
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
    @Test(expected = BuildException.class)
    public void testFailureStopsBuild() {
        buildRule.executeTarget("test-failure-stops-build");
    }

    /**
     * Tests that when a test, that's isn't configured with {@code haltOnFailure=true}, continues the
     * build even when there are test failures
     */
    @Test
    public void testFailureContinuesBuild() {
        buildRule.executeTarget("test-failure-continues-build");
    }

    /**
     * Tests the execution of test that's expected to succeed
     */
    @Test
    public void testSuccessfulTests() {
        buildRule.executeTarget("test-success");
    }

    /**
     * Tests execution of a test which is configured to execute only a particular set of test methods
     */
    @Test
    public void testSpecificMethodTest() {
        buildRule.executeTarget("test-one-specific-method");
        buildRule.executeTarget("test-multiple-specific-methods");
    }

    /**
     * Tests the execution of more than one {@code &lt;test&gt;} elements in the {@code &lt;junitlauncher&gt;} task
     */
    @Test
    public void testMultipleIndividualTests() {
        buildRule.executeTarget("test-multiple-individual");
    }

    /**
     * Tests execution of tests, that have been configured using the {@code &lt;testclasses&gt;} nested element
     * of the {@code &lt;junitlauncher&gt;} task
     */
    @Test
    public void testTestClasses() {
        buildRule.executeTarget("test-batch");
    }
}
