package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.Project;

import java.util.Optional;
import java.util.Properties;

/**
 * A {@link TestExecutionContext} represents the execution context for a test
 * that has been launched by the {@link JUnitLauncherTask} and provides any necessary
 * contextual information about such tests.
 */
public interface TestExecutionContext {

    /**
     * @return Returns the properties that were used for the execution of the test
     */
    Properties getProperties();


    /**
     * @return Returns the {@link Project} in whose context the test is being executed.
     * The {@code Project} is sometimes not available, like in the case where
     * the test is being run in a forked mode, in such cases this method returns
     * {@link Optional#empty() an empty value}
     */
    Optional<Project> getProject();
}
