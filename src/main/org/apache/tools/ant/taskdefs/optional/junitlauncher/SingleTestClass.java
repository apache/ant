package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.Project;
import org.junit.Test;
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

    SingleTestClass() {

    }

    public void setName(final String test) {
        if (test == null || test.trim().isEmpty()) {
            throw new IllegalArgumentException("Test name cannot be null or empty string");
        }
        this.testClass = test;
    }

    @Test
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
            final String[] methods = this.getMethods();
            for (final String method : methods) {
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
