package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.Task;
import org.junit.platform.launcher.TestExecutionListener;

import java.io.Closeable;
import java.io.OutputStream;

/**
 * A {@link TestExecutionListener} which lets implementing classes format and write out
 * the test execution results.
 */
public interface TestResultFormatter extends TestExecutionListener, Closeable {

    /**
     * This method will be invoked by the <code>junitlauncher</code> and will be passed the
     * {@link OutputStream} to a file, to which the formatted result is expected to be written
     * to.
     * <p>
     * This method will be called once, early on, during the initialization of this
     * {@link TestResultFormatter}, typically before the test execution itself has started.
     * </p>
     *
     * @param os The output stream to which to write out the result
     */
    void setDestination(OutputStream os);

    /**
     * This method will be invoked by the <code>junitlauncher</code> and will be passed the
     * {@link Task} under which this result formatter is configured. This allows the
     * {@link TestResultFormatter} to have access to any additional contextual information
     * to use in the test reports.
     *
     * @param task The {@link Task} in which this {@link TestResultFormatter} is configured
     */
    default void setExecutingTask(Task task) {
    }

    /**
     * This method will be invoked by the <code>junitlauncher</code>, <strong>regularly/multiple times</strong>,
     * as and when any content is generated on the standard output stream during the test execution.
     * This method will be only be called if the <code>sendSysOut</code> attribute of the <code>listener</code>,
     * to which this {@link TestResultFormatter} is configured for, is enabled
     *
     * @param data The content generated on standard output stream
     */
    default void sysOutAvailable(byte[] data) {
    }

    /**
     * This method will be invoked by the <code>junitlauncher</code>, <strong>regularly/multiple times</strong>,
     * as and when any content is generated on the standard error stream during the test execution.
     * This method will be only be called if the <code>sendSysErr</code> attribute of the <code>listener</code>,
     * to which this {@link TestResultFormatter} is configured for, is enabled
     *
     * @param data The content generated on standard error stream
     */
    default void sysErrAvailable(byte[] data) {
    }

}
