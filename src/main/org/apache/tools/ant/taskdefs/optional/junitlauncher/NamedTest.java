package org.apache.tools.ant.taskdefs.optional.junitlauncher;

/**
 * A test that has a name associated with it
 */
public interface NamedTest {

    /**
     * Returns the name of the test
     *
     * @return
     */
    String getName();
}
