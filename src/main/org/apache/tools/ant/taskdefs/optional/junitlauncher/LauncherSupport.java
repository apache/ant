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


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.LaunchDefinition;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.ListenerDefinition;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.NamedTest;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.SingleTestClass;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.TestClasses;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.TestDefinition;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.KeepAliveOutputStream;
import org.apache.tools.ant.util.LeadPipeInputStream;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for doing the real work involved in launching the JUnit platform
 * and passing it the relevant tests that need to be executed by the JUnit platform.
 * <p>
 * This class relies on a {@link LaunchDefinition} for setting up the launch of the
 * JUnit platform.
 * <p>
 * The {@code LauncherSupport} isn't concerned with whether or not
 * it's being executed in the same JVM as the build in which the {@code junitlauncher}
 * was triggered or if it's running as part of a forked JVM. Instead it just relies
 * on the {@code LaunchDefinition} to do whatever decisions need to be done before and
 * after launching the tests.
 * <p>
 * This class is not thread-safe and isn't expected to be used for launching from
 * multiple different threads simultaneously.
 * <p>This class is an internal implementation detail of the Ant project and although
 * it's a public class, it isn't meant to be used outside of this project. This class
 * can be changed, across releases, without any backward compatible guarantees and hence
 * shouldn't be used or relied upon outside of this project.
 */
public class LauncherSupport {

    private final LaunchDefinition launchDefinition;
    private final TestExecutionContext testExecutionContext;

    private boolean testsFailed;

    /**
     * Create a {@link LauncherSupport} for the passed {@link LaunchDefinition}
     *
     * @param definition           The launch definition which will be used for launching the tests
     * @param testExecutionContext The {@link TestExecutionContext} to use for the tests
     */
    public LauncherSupport(final LaunchDefinition definition, final TestExecutionContext testExecutionContext) {
        if (definition == null) {
            throw new IllegalArgumentException("Launch definition cannot be null");
        }
        if (testExecutionContext == null) {
            throw new IllegalArgumentException("Test execution context cannot be null");
        }
        this.launchDefinition = definition;
        this.testExecutionContext = testExecutionContext;
    }

    /**
     * Launches the tests defined in the {@link LaunchDefinition}
     *
     * @throws BuildException If any tests failed and the launch definition was configured to throw
     *                        an exception, or if any other exception occurred before or after launching
     *                        the tests
     */
    public void launch() throws BuildException {
        final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.launchDefinition.getClassLoader());
            final Launcher launcher = LauncherFactory.create();
            final List<TestRequest> requests = buildTestRequests();
            for (final TestRequest testRequest : requests) {
                try {
                    final TestDefinition test = testRequest.getOwner();
                    final LauncherDiscoveryRequest request = testRequest.getDiscoveryRequest().build();
                    final List<TestExecutionListener> testExecutionListeners = new ArrayList<>();
                    // a listener that we always put at the front of list of listeners
                    // for this request.
                    final Listener firstListener = new Listener(System.out);
                    // we always enroll the summary generating listener, to the request, so that we
                    // get to use some of the details of the summary for our further decision making
                    testExecutionListeners.add(firstListener);
                    testExecutionListeners.addAll(getListeners(testRequest, this.launchDefinition.getClassLoader()));
                    final PrintStream originalSysOut = System.out;
                    final PrintStream originalSysErr = System.err;
                    try {
                        firstListener.switchedSysOutHandle = trySwitchSysOutErr(testRequest, StreamType.SYS_OUT, originalSysErr);
                        firstListener.switchedSysErrHandle = trySwitchSysOutErr(testRequest, StreamType.SYS_ERR, originalSysErr);
                        launcher.execute(request, testExecutionListeners.toArray(new TestExecutionListener[0]));
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
                        // close the streams that we had used to redirect System.out/System.err
                        try {
                            firstListener.switchedSysOutHandle.ifPresent((h) -> {
                                try {
                                    h.close();
                                } catch (Exception e) {
                                    // ignore
                                }
                            });
                        } catch (Exception e) {
                            // ignore
                        }
                        try {
                            firstListener.switchedSysErrHandle.ifPresent((h) -> {
                                try {
                                    h.close();
                                } catch (Exception e) {
                                    // ignore
                                }
                            });
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
     * Returns true if there were any test failures, when this {@link LauncherSupport} was used
     * to {@link #launch()} tests. False otherwise.
     *
     * @return
     */
    boolean hasTestFailures() {
        return this.testsFailed;
    }

    private List<TestRequest> buildTestRequests() {
        final List<TestDefinition> tests = this.launchDefinition.getTests();
        if (tests.isEmpty()) {
            return Collections.emptyList();
        }
        final List<TestRequest> requests = new ArrayList<>();
        for (final TestDefinition test : tests) {
            final List<TestRequest> testRequests;
            if (test instanceof SingleTestClass || test instanceof TestClasses) {
                testRequests = createTestRequests(test);
            } else {
                throw new BuildException("Unexpected test definition type " + test.getClass().getName());
            }
            if (testRequests == null || testRequests.isEmpty()) {
                continue;
            }
            requests.addAll(testRequests);
        }
        return requests;
    }

    private List<TestExecutionListener> getListeners(final TestRequest testRequest, final ClassLoader classLoader) {
        final TestDefinition test = testRequest.getOwner();
        final List<ListenerDefinition> applicableListenerElements = test.getListeners().isEmpty()
                ? this.launchDefinition.getListeners() : test.getListeners();
        final List<TestExecutionListener> listeners = new ArrayList<>();
        final Optional<Project> project = this.testExecutionContext.getProject();
        for (final ListenerDefinition applicableListener : applicableListenerElements) {
            if (project.isPresent() && !applicableListener.shouldUse(project.get())) {
                log("Excluding listener " + applicableListener.getClassName() + " since it's not applicable" +
                        " in the context of project", null, Project.MSG_DEBUG);
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
        resultFormatter.setContext(this.testExecutionContext);
        resultFormatter.setUseLegacyReportingName(formatterDefinition.isUseLegacyReportingName());
        // set the destination output stream for writing out the formatted result
        final java.nio.file.Path resultOutputFile = getListenerOutputFile(testRequest, formatterDefinition);
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

    private Path getListenerOutputFile(final TestRequest testRequest, final ListenerDefinition listener) {
        final TestDefinition test = testRequest.getOwner();
        final String filename;
        if (listener.getResultFile() != null) {
            filename = listener.getResultFile();
        } else {
            // compute a file name
            final StringBuilder sb = new StringBuilder("TEST-");
            sb.append(testRequest.getName() == null ? "unknown" : testRequest.getName());
            sb.append(".");
            final String suffix;
            if ("org.apache.tools.ant.taskdefs.optional.junitlauncher.LegacyXmlResultFormatter".equals(listener.getClassName())) {
                suffix = "xml";
            } else {
                suffix = "txt";
            }
            sb.append(suffix);
            filename = sb.toString();
        }
        if (listener.getOutputDir() != null) {
            // use the output dir defined on the listener
            return Paths.get(listener.getOutputDir(), filename);
        }
        // check on the enclosing test definition, in context of which this listener is being run
        if (test.getOutputDir() != null) {
            return Paths.get(test.getOutputDir(), filename);
        }
        // neither listener nor the test define a output dir, so use basedir of the project
        final TestExecutionContext testExecutionContext = this.testExecutionContext;
        final String baseDir = testExecutionContext.getProperties().getProperty(MagicNames.PROJECT_BASEDIR);
        return Paths.get(baseDir, filename);
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
            return (TestExecutionListener) klass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new BuildException("Failed to create an instance of listener " + className, e);
        }
    }

    private void handleTestExecutionCompletion(final TestDefinition test, final TestExecutionSummary summary) {
        final boolean hasTestFailures = summary.getTotalFailureCount() != 0;
        if (hasTestFailures) {
            // keep track of the test failure(s) for the entire launched instance
            this.testsFailed = true;
        }
        try {
            if (hasTestFailures && test.getFailureProperty() != null) {
                // if there are test failures and the test is configured to set a property in case
                // of failure, then set the property to true
                final TestExecutionContext testExecutionContext = this.testExecutionContext;
                if (testExecutionContext.getProject().isPresent()) {
                    final Project project = testExecutionContext.getProject().get();
                    project.setNewProperty(test.getFailureProperty(), "true");
                }
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

    private Optional<SwitchedStreamHandle> trySwitchSysOutErr(final TestRequest testRequest, final StreamType streamType,
                                                              final PrintStream originalSysErr) {
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
            pipedInputStream = new LeadPipeInputStream(pipedOutputStream);
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
                        StreamType.SYS_OUT, testRequest.getSysOutInterests(), originalSysErr);
                final Thread sysOutStreamer = new Thread(streamer);
                sysOutStreamer.setDaemon(true);
                sysOutStreamer.setName("junitlauncher-sysout-stream-reader");
                sysOutStreamer.setUncaughtExceptionHandler((t, e) -> {
                    // skip the logging redirection infrastructure of junitlauncher task (which is what has
                    // failed here) and instead directly write out the error to the original System.err
                    originalSysErr.println("Failed in sysout streaming");
                    e.printStackTrace(originalSysErr);
                });
                sysOutStreamer.start();
                break;
            }
            case SYS_ERR: {
                System.setErr(new PrintStream(printStream));
                streamer = new SysOutErrStreamReader(this, pipedInputStream,
                        StreamType.SYS_ERR, testRequest.getSysErrInterests(), originalSysErr);
                final Thread sysErrStreamer = new Thread(streamer);
                sysErrStreamer.setDaemon(true);
                sysErrStreamer.setName("junitlauncher-syserr-stream-reader");
                sysErrStreamer.setUncaughtExceptionHandler((t, e) -> {
                    // skip the logging redirection infrastructure of junitlauncher task (which is what has
                    // failed here) and instead directly write out the error to the original System.err
                    originalSysErr.println("Failed in syserr streaming");
                    e.printStackTrace(originalSysErr);
                });
                sysErrStreamer.start();
                break;
            }
            default: {
                return Optional.empty();
            }
        }
        return Optional.of(new SwitchedStreamHandle(pipedOutputStream, streamer));
    }

    private void log(final String message, final Throwable t, final int level) {
        final TestExecutionContext testExecutionContext = this.testExecutionContext;
        if (testExecutionContext.getProject().isPresent()) {
            testExecutionContext.getProject().get().log(message, t, level);
            return;
        }
        if (t == null) {
            System.out.println(message);
        } else {
            System.err.println(message);
            t.printStackTrace();
        }
    }


    private List<TestRequest> createTestRequests(final TestDefinition test) {
        // create TestRequest(s) and add necessary selectors, filters to it

        if (test instanceof SingleTestClass) {
            final SingleTestClass singleTestClass = (SingleTestClass) test;
            final LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();
            final TestRequest request = new TestRequest(test, requestBuilder);
            request.setName(singleTestClass.getName());
            final String[] methods = singleTestClass.getMethods();
            if (methods == null) {
                requestBuilder.selectors(DiscoverySelectors.selectClass(singleTestClass.getName()));
            } else {
                // add specific methods
                for (final String method : methods) {
                    requestBuilder.selectors(DiscoverySelectors.selectMethod(singleTestClass.getName(), method));
                }
            }
            addFilters(request);
            return Collections.singletonList(request);
        }

        if (test instanceof TestClasses) {
            final List<String> testClasses = ((TestClasses) test).getTestClassNames();
            if (testClasses.isEmpty()) {
                return Collections.emptyList();
            }
            final List<TestRequest> requests = new ArrayList<>();
            for (final String testClass : testClasses) {
                final LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();
                final TestRequest request = new TestRequest(test, requestBuilder);
                request.setName(testClass);
                requestBuilder.selectors(DiscoverySelectors.selectClass(testClass));
                addFilters(request);

                requests.add(request);
            }
            return requests;
        }
        return Collections.emptyList();
    }

    /**
     * Add necessary {@link Filter JUnit filters} to the {@code testRequest}
     *
     * @param testRequest The test request
     */
    private void addFilters(final TestRequest testRequest) {
        final LauncherDiscoveryRequestBuilder requestBuilder = testRequest.getDiscoveryRequest();
        // add any engine filters
        final String[] enginesToInclude = testRequest.getOwner().getIncludeEngines();
        if (enginesToInclude != null && enginesToInclude.length > 0) {
            requestBuilder.filters(EngineFilter.includeEngines(enginesToInclude));
        }
        final String[] enginesToExclude = testRequest.getOwner().getExcludeEngines();
        if (enginesToExclude != null && enginesToExclude.length > 0) {
            requestBuilder.filters(EngineFilter.excludeEngines(enginesToExclude));
        }
        // add any tag filters
        if (this.launchDefinition.getIncludeTags().size() > 0) {
            requestBuilder.filters(TagFilter.includeTags(this.launchDefinition.getIncludeTags()));
        }
        if (this.launchDefinition.getExcludeTags().size() > 0) {
            requestBuilder.filters(TagFilter.excludeTags(this.launchDefinition.getExcludeTags()));
        }
    }

    private enum StreamType {
        SYS_OUT,
        SYS_ERR
    }

    // Implementation note: Logging from this class is prohibited since it can lead
    // to deadlocks (see bz-64733 for details)
    private static final class SysOutErrStreamReader implements Runnable {
        private static final byte[] EMPTY = new byte[0];

        private final LauncherSupport launchManager;
        private final PrintStream originalSysErr;
        private final InputStream sourceStream;
        private final StreamType streamType;
        private final Collection<TestResultFormatter> resultFormatters;
        private volatile SysOutErrContentDeliverer contentDeliverer;

        SysOutErrStreamReader(final LauncherSupport launchManager, final InputStream source,
                              final StreamType streamType, final Collection<TestResultFormatter> resultFormatters,
                              final PrintStream originalSysErr) {
            this.launchManager = launchManager;
            this.sourceStream = source;
            this.streamType = streamType;
            this.resultFormatters = resultFormatters;
            this.originalSysErr = originalSysErr;
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
                // let the UncaughtExceptionHandler of this thread deal with this exception
                throw new UncheckedIOException(e);
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

    private final class SwitchedStreamHandle implements AutoCloseable {
        private final PipedOutputStream outputStream;
        private final SysOutErrStreamReader streamReader;

        SwitchedStreamHandle(final PipedOutputStream outputStream, final SysOutErrStreamReader streamReader) {
            this.streamReader = streamReader;
            this.outputStream = outputStream;
        }

        @Override
        public void close() throws Exception {
            outputStream.close();
            streamReader.sourceStream.close();
        }
    }

    private final class Listener extends SummaryGeneratingListener {
        private final PrintStream originalSysOut;

        private Optional<SwitchedStreamHandle> switchedSysOutHandle;
        private Optional<SwitchedStreamHandle> switchedSysErrHandle;

        private Listener(final PrintStream originalSysOut) {
            this.originalSysOut = originalSysOut;
        }

        @Override
        public void executionStarted(final TestIdentifier testIdentifier) {
            super.executionStarted(testIdentifier);
            AbstractJUnitResultFormatter.isTestClass(testIdentifier).ifPresent(testClass ->
                    this.originalSysOut.println("Running " + testClass.getClassName()));
        }


        private static final double ONE_SECOND = 1000.0;
        // We use this only in the testPlanExecutionFinished method, which
        // as per the JUnit5 platform semantics won't be called concurrently
        // by multiple threads (https://github.com/junit-team/junit5/issues/2539#issuecomment-766325555).
        // So it's safe to use this without any additional thread safety access controls.
        private NumberFormat timeFormatter = NumberFormat.getInstance();

        @Override
        public void testPlanExecutionFinished(final TestPlan testPlan) {
            super.testPlanExecutionFinished(testPlan);
            if (!testPlan.containsTests()) {
                // we print the summary only if any tests are present
                return;
            }
            if (launchDefinition.isPrintSummary()) {
                final TestExecutionSummary summary = this.getSummary();
                // Keep the summary as close to as the old junit task summary
                // tests run, failed, skipped, duration
                final StringBuilder sb = new StringBuilder("Tests run: ");
                sb.append(summary.getTestsStartedCount());
                sb.append(", Failures: ");
                sb.append(summary.getTestsFailedCount());
                sb.append(", Aborted: ");
                sb.append(summary.getTestsAbortedCount());
                sb.append(", Skipped: ");
                sb.append(summary.getTestsSkippedCount());
                sb.append(", Time elapsed: ");
                final long elapsedMs = summary.getTimeFinished() - summary.getTimeStarted();
                sb.append(timeFormatter.format(elapsedMs / ONE_SECOND));
                sb.append(" sec");
                this.originalSysOut.println(sb.toString());
            }
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

}
