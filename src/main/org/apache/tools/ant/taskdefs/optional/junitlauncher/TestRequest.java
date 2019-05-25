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

import org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.JUnitLauncherTask;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.TestDefinition;
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
    private String name;

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

    void setName(final String name) {
        this.name = name;
    }

    String getName() {
        return this.name;
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
