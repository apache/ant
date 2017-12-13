package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Holds together the necessary details about a request that will be launched by the {@link JUnitLauncherTask}
 */
final class TestRequest implements AutoCloseable {
    private final TestDefinition ownerTest;
    private final LauncherDiscoveryRequestBuilder discoveryRequest;
    private final List<Closeable> closables = new ArrayList<>();
    private final List<TestResultFormatter> interestedInSysOut = new ArrayList<>();
    private final List<TestResultFormatter> interestedInSysErr = new ArrayList<>();


    TestRequest(final TestDefinition ownerTest, final LauncherDiscoveryRequestBuilder discoveryRequest) {
        this.ownerTest = ownerTest;
        this.discoveryRequest = discoveryRequest;
    }

    TestDefinition getOwner() {
        return ownerTest;
    }

    LauncherDiscoveryRequestBuilder getDiscoveryRequest() {
        return discoveryRequest;
    }

    void closeUponCompletion(final Closeable closeable) {
        if (closeable == null) {
            return;
        }
        this.closables.add(closeable);
    }

    void addSysOutInterest(final TestResultFormatter out) {
        this.interestedInSysOut.add(out);
    }

    boolean interestedInSysOut() {
        return !this.interestedInSysOut.isEmpty();
    }

    Collection<TestResultFormatter> getSysOutInterests() {
        return Collections.unmodifiableList(this.interestedInSysOut);
    }

    void addSysErrInterest(final TestResultFormatter err) {
        this.interestedInSysErr.add(err);
    }

    boolean interestedInSysErr() {
        return !this.interestedInSysErr.isEmpty();
    }

    Collection<TestResultFormatter> getSysErrInterests() {
        return Collections.unmodifiableList(this.interestedInSysErr);
    }

    public void close() throws Exception {
        if (this.closables.isEmpty()) {
            return;
        }
        for (final Closeable closeable : closables) {
            closeable.close();
        }
    }
}
