package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.KeepAliveOutputStream;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * An Ant {@link Task} responsible for launching the JUnit platform for running tests.
 * This requires a minimum of JUnit 5, since that's the version in which the JUnit platform launcher
 * APIs were introduced.
 * <p>
 * This task in itself doesn't run the JUnit tests, instead the sole responsibility of
 * this task is to setup the JUnit platform launcher, build requests, launch those requests and then parse the
 * result of the execution to present in a way that's been configured on this Ant task.
 * </p>
 * <p>
 * Furthermore, this task allows users control over which classes to select for passing on to the JUnit 5
 * platform for test execution. It however, is solely the JUnit 5 platform, backed by test engines that
 * decide and execute the tests.
 *
 * @see <a href="https://junit.org/junit5/">JUnit 5 documentation</a> for more details
 * on how JUnit manages the platform and the test engines.
 */
public class JUnitLauncherTask extends Task {

    private Path classPath;
    private boolean haltOnFailure;
    private String failureProperty;
    private boolean printSummary;
    private final List<TestDefinition> tests = new ArrayList<>();
    private final List<ListenerDefinition> listeners = new ArrayList<>();

    public JUnitLauncherTask() {
    }

    @Override
    public void execute() throws BuildException {
        final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            final ClassLoader executionCL = createClassLoaderForTestExecution();
            Thread.currentThread().setContextClassLoader(executionCL);
            final Launcher launcher = LauncherFactory.create();
            final List<TestRequest> requests = buildTestRequests();
            for (final TestRequest testRequest : requests) {
                try {
                    final TestDefinition test = testRequest.getOwner();
                    final LauncherDiscoveryRequest request = testRequest.getDiscoveryRequest().build();
                    final List<TestExecutionListener> testExecutionListeners = new ArrayList<>();
                    // a listener that we always put at the front of list of listeners
                    // for this request.
                    final Listener firstListener = new Listener();
                    // we always enroll the summary generating listener, to the request, so that we
                    // get to use some of the details of the summary for our further decision making
                    testExecutionListeners.add(firstListener);
                    testExecutionListeners.addAll(getListeners(testRequest, executionCL));
                    final PrintStream originalSysOut = System.out;
                    final PrintStream originalSysErr = System.err;
                    try {
                        firstListener.switchedSysOutHandle = trySwitchSysOutErr(testRequest, StreamType.SYS_OUT);
                        firstListener.switchedSysErrHandle = trySwitchSysOutErr(testRequest, StreamType.SYS_ERR);
                        launcher.execute(request, testExecutionListeners.toArray(new TestExecutionListener[testExecutionListeners.size()]));
                    } finally {
                        // switch back sysout/syserr to the original
                        try {
                            System.setOut(originalSysOut);
                        } catch (Exception e) {
                            // ignore
                        }
                        try {
                            System.setErr(originalSysErr);
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    handleTestExecutionCompletion(test, firstListener.getSummary());
                } finally {
                    try {
                        testRequest.close();
                    } catch (Exception e) {
                        // log and move on
                        log("Failed to cleanly close test request", e, Project.MSG_DEBUG);
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
        }
    }

    /**
     * Adds the {@link Path} to the classpath which will be used for execution of the tests
     *
     * @param path The classpath
     */
    public void addConfiguredClassPath(final Path path) {
        if (this.classPath == null) {
            // create a "wrapper" path which can hold on to multiple
            // paths that get passed to this method (if at all the task in the build is
            // configured with multiple classpaht elements)
            this.classPath = new Path(getProject());
        }
        this.classPath.add(path);
    }

    /**
     * Adds a {@link SingleTestClass} that will be passed on to the underlying JUnit platform
     * for possible execution of the test
     *
     * @param test The test
     */
    public void addConfiguredTest(final SingleTestClass test) {
        this.preConfigure(test);
        this.tests.add(test);
    }

    /**
     * Adds {@link TestClasses} that will be passed on to the underlying JUnit platform for
     * possible execution of the tests
     *
     * @param testClasses The test classes
     */
    public void addConfiguredTestClasses(final TestClasses testClasses) {
        this.preConfigure(testClasses);
        this.tests.add(testClasses);
    }

    /**
     * Adds a {@link ListenerDefinition listener} which will be enrolled for listening to test
     * execution events
     *
     * @param listener The listener
     */
    public void addConfiguredListener(final ListenerDefinition listener) {
        this.listeners.add(listener);
    }

    public void setHaltonfailure(final boolean haltonfailure) {
        this.haltOnFailure = haltonfailure;
    }

    public void setFailureProperty(final String failureProperty) {
        this.failureProperty = failureProperty;
    }

    public void setPrintSummary(final boolean printSummary) {
        this.printSummary = printSummary;
    }

    private void preConfigure(final TestDefinition test) {
        if (test.getHaltOnFailure() == null) {
            test.setHaltOnFailure(this.haltOnFailure);
        }
        if (test.getFailureProperty() == null) {
            test.setFailureProperty(this.failureProperty);
        }
    }

    private List<TestRequest> buildTestRequests() {
        if (this.tests.isEmpty()) {
            return Collections.emptyList();
        }
        final List<TestRequest> requests = new ArrayList<>();
        for (final TestDefinition test : this.tests) {
            final List<TestRequest> testRequests = test.createTestRequests(this);
            if (testRequests == null || testRequests.isEmpty()) {
                continue;
            }
            requests.addAll(testRequests);
        }
        return requests;
    }

    private List<TestExecutionListener> getListeners(final TestRequest testRequest, final ClassLoader classLoader) {
        final TestDefinition test = testRequest.getOwner();
        final List<ListenerDefinition> applicableListenerElements = test.getListeners().isEmpty() ? this.listeners : test.getListeners();
        final List<TestExecutionListener> listeners = new ArrayList<>();
        final Project project = getProject();
        for (final ListenerDefinition applicableListener : applicableListenerElements) {
            if (!applicableListener.shouldUse(project)) {
                log("Excluding listener " + applicableListener.getClassName() + " since it's not applicable" +
                        " in the context of project " + project, Project.MSG_DEBUG);
                continue;
            }
            final TestExecutionListener listener = requireTestExecutionListener(applicableListener, classLoader);
            if (listener instanceof TestResultFormatter) {
                // setup/configure the result formatter
                setupResultFormatter(testRequest, applicableListener, (TestResultFormatter) listener);
            }
            listeners.add(listener);
        }
        return listeners;
    }

    private void setupResultFormatter(final TestRequest testRequest, final ListenerDefinition formatterDefinition,
                                      final TestResultFormatter resultFormatter) {

        testRequest.closeUponCompletion(resultFormatter);
        // set the execution context
        resultFormatter.setContext(new InVMExecution());
        // set the destination output stream for writing out the formatted result
        final TestDefinition test = testRequest.getOwner();
        final java.nio.file.Path outputDir = test.getOutputDir() != null ? Paths.get(test.getOutputDir()) : getProject().getBaseDir().toPath();
        final String filename = formatterDefinition.requireResultFile(test);
        final java.nio.file.Path resultOutputFile = Paths.get(outputDir.toString(), filename);
        try {
            final OutputStream resultOutputStream = Files.newOutputStream(resultOutputFile);
            // enroll the output stream to be closed when the execution of the TestRequest completes
            testRequest.closeUponCompletion(resultOutputStream);
            resultFormatter.setDestination(new KeepAliveOutputStream(resultOutputStream));
        } catch (IOException e) {
            throw new BuildException(e);
        }
        // check if system.out/system.err content needs to be passed on to the listener
        if (formatterDefinition.shouldSendSysOut()) {
            testRequest.addSysOutInterest(resultFormatter);
        }
        if (formatterDefinition.shouldSendSysErr()) {
            testRequest.addSysErrInterest(resultFormatter);
        }
    }

    private TestExecutionListener requireTestExecutionListener(final ListenerDefinition listener, final ClassLoader classLoader) {
        final String className = listener.getClassName();
        if (className == null || className.trim().isEmpty()) {
            throw new BuildException("classname attribute value is missing on listener element");
        }
        final Class<?> klass;
        try {
            klass = Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new BuildException("Failed to load listener class " + className, e);
        }
        if (!TestExecutionListener.class.isAssignableFrom(klass)) {
            throw new BuildException("Listener class " + className + " is not of type " + TestExecutionListener.class.getName());
        }
        try {
            return TestExecutionListener.class.cast(klass.newInstance());
        } catch (Exception e) {
            throw new BuildException("Failed to create an instance of listener " + className, e);
        }
    }

    private void handleTestExecutionCompletion(final TestDefinition test, final TestExecutionSummary summary) {
        if (printSummary) {
            // print the summary to System.out
            summary.printTo(new PrintWriter(System.out, true));
        }
        final boolean hasTestFailures = summary.getTestsFailedCount() != 0;
        try {
            if (hasTestFailures && test.getFailureProperty() != null) {
                // if there are test failures and the test is configured to set a property in case
                // of failure, then set the property to true
                getProject().setNewProperty(test.getFailureProperty(), "true");
            }
        } finally {
            if (hasTestFailures && test.isHaltOnFailure()) {
                // if the test is configured to halt on test failures, throw a build error
                final String errorMessage;
                if (test instanceof NamedTest) {
                    errorMessage = "Test " + ((NamedTest) test).getName() + " has " + summary.getTestsFailedCount() + " failure(s)";
                } else {
                    errorMessage = "Some test(s) have failure(s)";
                }
                throw new BuildException(errorMessage);
            }
        }
    }

    private ClassLoader createClassLoaderForTestExecution() {
        if (this.classPath == null) {
            return this.getClass().getClassLoader();
        }
        return new AntClassLoader(this.getClass().getClassLoader(), getProject(), this.classPath, true);
    }

    @SuppressWarnings("resource")
    private Optional<SwitchedStreamHandle> trySwitchSysOutErr(final TestRequest testRequest, final StreamType streamType) {
        switch (streamType) {
            case SYS_OUT: {
                if (!testRequest.interestedInSysOut()) {
                    return Optional.empty();
                }
                break;
            }
            case SYS_ERR: {
                if (!testRequest.interestedInSysErr()) {
                    return Optional.empty();
                }
                break;
            }
            default: {
                // unknown, but no need to error out, just be lenient
                // and return back
                return Optional.empty();
            }
        }
        final PipedOutputStream pipedOutputStream = new PipedOutputStream();
        final PipedInputStream pipedInputStream;
        try {
            pipedInputStream = new PipedInputStream(pipedOutputStream);
        } catch (IOException ioe) {
            // log and return
            return Optional.empty();
        }
        final PrintStream printStream = new PrintStream(pipedOutputStream, true);
        final SysOutErrStreamReader streamer;
        switch (streamType) {
            case SYS_OUT: {
                System.setOut(new PrintStream(printStream));
                streamer = new SysOutErrStreamReader(this, pipedInputStream,
                        StreamType.SYS_OUT, testRequest.getSysOutInterests());
                final Thread sysOutStreamer = new Thread(streamer);
                sysOutStreamer.setDaemon(true);
                sysOutStreamer.setName("junitlauncher-sysout-stream-reader");
                sysOutStreamer.setUncaughtExceptionHandler((t, e) -> this.log("Failed in sysout streaming", e, Project.MSG_INFO));
                sysOutStreamer.start();
                break;
            }
            case SYS_ERR: {
                System.setErr(new PrintStream(printStream));
                streamer = new SysOutErrStreamReader(this, pipedInputStream,
                        StreamType.SYS_ERR, testRequest.getSysErrInterests());
                final Thread sysErrStreamer = new Thread(streamer);
                sysErrStreamer.setDaemon(true);
                sysErrStreamer.setName("junitlauncher-syserr-stream-reader");
                sysErrStreamer.setUncaughtExceptionHandler((t, e) -> this.log("Failed in syserr streaming", e, Project.MSG_INFO));
                sysErrStreamer.start();
                break;
            }
            default: {
                return Optional.empty();
            }
        }
        return Optional.of(new SwitchedStreamHandle(pipedOutputStream, streamer));
    }

    private enum StreamType {
        SYS_OUT,
        SYS_ERR
    }

    private static final class SysOutErrStreamReader implements Runnable {
        private static final byte[] EMPTY = new byte[0];

        private final JUnitLauncherTask task;
        private final InputStream sourceStream;
        private final StreamType streamType;
        private final Collection<TestResultFormatter> resultFormatters;
        private volatile SysOutErrContentDeliverer contentDeliverer;

        SysOutErrStreamReader(final JUnitLauncherTask task, final InputStream source, final StreamType streamType, final Collection<TestResultFormatter> resultFormatters) {
            this.task = task;
            this.sourceStream = source;
            this.streamType = streamType;
            this.resultFormatters = resultFormatters;
        }

        @Override
        public void run() {
            final SysOutErrContentDeliverer streamContentDeliver = new SysOutErrContentDeliverer(this.streamType, this.resultFormatters);
            final Thread deliveryThread = new Thread(streamContentDeliver);
            deliveryThread.setName("junitlauncher-" + (this.streamType == StreamType.SYS_OUT ? "sysout" : "syserr") + "-stream-deliverer");
            deliveryThread.setDaemon(true);
            deliveryThread.start();
            this.contentDeliverer = streamContentDeliver;
            int numRead = -1;
            final byte[] data = new byte[1024];
            try {
                while ((numRead = this.sourceStream.read(data)) != -1) {
                    final byte[] copy = Arrays.copyOf(data, numRead);
                    streamContentDeliver.availableData.offer(copy);
                }
            } catch (IOException e) {
                task.log("Failed while streaming " + (this.streamType == StreamType.SYS_OUT ? "sysout" : "syserr") + " data",
                        e, Project.MSG_INFO);
            } finally {
                streamContentDeliver.stop = true;
                // just "wakeup" the delivery thread, to take into account
                // those race conditions, where that other thread didn't yet
                // notice that it was asked to stop and has now gone into a
                // X amount of wait, waiting for any new data
                streamContentDeliver.availableData.offer(EMPTY);
            }
        }
    }

    private static final class SysOutErrContentDeliverer implements Runnable {
        private volatile boolean stop;
        private final Collection<TestResultFormatter> resultFormatters;
        private final StreamType streamType;
        private final BlockingQueue<byte[]> availableData = new LinkedBlockingQueue<>();
        private final CountDownLatch completionLatch = new CountDownLatch(1);

        SysOutErrContentDeliverer(final StreamType streamType, final Collection<TestResultFormatter> resultFormatters) {
            this.streamType = streamType;
            this.resultFormatters = resultFormatters;
        }

        @Override
        public void run() {
            try {
                while (!this.stop) {
                    final byte[] streamData;
                    try {
                        streamData = this.availableData.poll(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (streamData != null) {
                        deliver(streamData);
                    }
                }
                // drain it
                final List<byte[]> remaining = new ArrayList<>();
                this.availableData.drainTo(remaining);
                if (!remaining.isEmpty()) {
                    for (final byte[] data : remaining) {
                        deliver(data);
                    }
                }
            } finally {
                this.completionLatch.countDown();
            }
        }

        private void deliver(final byte[] data) {
            if (data == null || data.length == 0) {
                return;
            }
            for (final TestResultFormatter resultFormatter : this.resultFormatters) {
                // send it to the formatter
                switch (streamType) {
                    case SYS_OUT: {
                        resultFormatter.sysOutAvailable(data);
                        break;
                    }
                    case SYS_ERR: {
                        resultFormatter.sysErrAvailable(data);
                        break;
                    }
                }
            }
        }
    }

    private final class SwitchedStreamHandle {
        private final PipedOutputStream outputStream;
        private final SysOutErrStreamReader streamReader;

        SwitchedStreamHandle(final PipedOutputStream outputStream, final SysOutErrStreamReader streamReader) {
            this.streamReader = streamReader;
            this.outputStream = outputStream;
        }
    }

    private final class Listener extends SummaryGeneratingListener {
        private Optional<SwitchedStreamHandle> switchedSysOutHandle;
        private Optional<SwitchedStreamHandle> switchedSysErrHandle;

        @Override
        public void testPlanExecutionFinished(final TestPlan testPlan) {
            super.testPlanExecutionFinished(testPlan);
            // now that the test plan execution is finished, close the switched sysout/syserr output streams
            // and wait for the sysout and syserr content delivery, to result formatters, to finish
            if (this.switchedSysOutHandle.isPresent()) {
                final SwitchedStreamHandle sysOut = this.switchedSysOutHandle.get();
                try {
                    closeAndWait(sysOut);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (this.switchedSysErrHandle.isPresent()) {
                final SwitchedStreamHandle sysErr = this.switchedSysErrHandle.get();
                try {
                    closeAndWait(sysErr);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private void closeAndWait(final SwitchedStreamHandle handle) throws InterruptedException {
            FileUtils.close(handle.outputStream);
            if (handle.streamReader.contentDeliverer == null) {
                return;
            }
            // wait for a few seconds
            handle.streamReader.contentDeliverer.completionLatch.await(2, TimeUnit.SECONDS);
        }
    }

    private final class InVMExecution implements TestExecutionContext {

        private final Properties props;

        InVMExecution() {
            this.props = new Properties();
            this.props.putAll(JUnitLauncherTask.this.getProject().getProperties());
        }

        @Override
        public Properties getProperties() {
            return this.props;
        }

        @Override
        public Optional<Project> getProject() {
            return Optional.of(JUnitLauncherTask.this.getProject());
        }
    }
}
