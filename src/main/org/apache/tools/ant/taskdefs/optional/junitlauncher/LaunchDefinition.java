package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import java.util.List;

/**
 * Defines the necessary context for launching the JUnit platform for running
 * tests.
 */
public interface LaunchDefinition {

    /**
     * Returns the {@link TestDefinition tests} that have to be launched
     *
     * @return
     */
    List<TestDefinition> getTests();

    /**
     * Returns the default {@link ListenerDefinition listeners} that will be used
     * for the tests, if the {@link #getTests() tests} themselves don't specify any
     *
     * @return
     */
    List<ListenerDefinition> getListeners();

    /**
     * Returns true if a summary needs to be printed out after the execution of the
     * tests. False otherwise.
     *
     * @return
     */
    boolean isPrintSummary();

    /**
     * Returns true if any remaining tests launch need to be stopped if any test execution
     * failed. False otherwise.
     *
     * @return
     */
    boolean isHaltOnFailure();

    /**
     * Returns the {@link ClassLoader} that has to be used for launching and execution of the
     * tests
     *
     * @return
     */
    ClassLoader getClassLoader();

    /**
     * Returns the {@link TestExecutionContext} that will be passed to {@link TestResultFormatter#setContext(TestExecutionContext)
     * result formatters} which are applicable during the execution of the tests.
     *
     * @return
     */
    TestExecutionContext getTestExecutionContext();
}
