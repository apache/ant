/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.Project;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Represents the single {@code test} (class) that's configured to be launched by the {@link JUnitLauncherTask}
 */
public class SingleTestClass extends TestDefinition implements NamedTest {

    private String testClass;
    private Set<String> testMethods;

    public SingleTestClass() {

    }

    public void setName(final String test) {
        if (test == null || test.trim().isEmpty()) {
            throw new IllegalArgumentException("Test name cannot be null or empty string");
        }
        this.testClass = test;
    }

    public String getName() {
        return this.testClass;
    }

    public void setMethods(final String methods) {
        // parse the comma separated set of methods
        if (methods == null || methods.trim().isEmpty()) {
            this.testMethods = Collections.emptySet();
            return;
        }
        final StringTokenizer tokenizer = new StringTokenizer(methods, ",");
        if (!tokenizer.hasMoreTokens()) {
            this.testMethods = Collections.emptySet();
            return;
        }
        // maintain specified ordering
        this.testMethods = new LinkedHashSet<>();
        while (tokenizer.hasMoreTokens()) {
            final String method = tokenizer.nextToken().trim();
            if (method.isEmpty()) {
                continue;
            }
            this.testMethods.add(method);
        }
    }

    boolean hasMethodsSpecified() {
        return this.testMethods != null && !this.testMethods.isEmpty();
    }

    String[] getMethods() {
        if (!hasMethodsSpecified()) {
            return null;
        }
        return this.testMethods.toArray(new String[this.testMethods.size()]);
    }

    @Override
    List<TestRequest> createTestRequests(final JUnitLauncherTask launcherTask) {
        final Project project = launcherTask.getProject();
        if (!shouldRun(project)) {
            launcherTask.log("Excluding test " + this.testClass + " since it's considered not to run " +
                    "in context of project " + project, Project.MSG_DEBUG);
            return Collections.emptyList();
        }
        final LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();
        if (!this.hasMethodsSpecified()) {
            requestBuilder.selectors(DiscoverySelectors.selectClass(this.testClass));
        } else {
            // add specific methods
            for (final String method : this.getMethods()) {
                requestBuilder.selectors(DiscoverySelectors.selectMethod(this.testClass, method));
            }
        }
        // add any engine filters
        final String[] enginesToInclude = this.getIncludeEngines();
        if (enginesToInclude != null && enginesToInclude.length > 0) {
            requestBuilder.filters(EngineFilter.includeEngines(enginesToInclude));
        }
        final String[] enginesToExclude = this.getExcludeEngines();
        if (enginesToExclude != null && enginesToExclude.length > 0) {
            requestBuilder.filters(EngineFilter.excludeEngines(enginesToExclude));
        }
        return Collections.singletonList(new TestRequest(this, requestBuilder));
    }
}
