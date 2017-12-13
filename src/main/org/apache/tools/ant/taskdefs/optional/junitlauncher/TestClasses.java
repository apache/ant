package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Resources;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a {@code testclasses} that's configured to be launched by the {@link JUnitLauncherTask}
 */
public class TestClasses extends TestDefinition {

    private final Resources resources = new Resources();


    public void add(final ResourceCollection resourceCollection) {
        this.resources.add(resourceCollection);
    }

    @Override
    List<TestRequest> createTestRequests(final JUnitLauncherTask launcherTask) {
        final List<SingleTestClass> tests = this.getTests();
        if (tests.isEmpty()) {
            return Collections.emptyList();
        }
        final List<TestRequest> requests = new ArrayList<>();
        for (final SingleTestClass test : tests) {
            requests.addAll(test.createTestRequests(launcherTask));
        }
        return requests;
    }

    private List<SingleTestClass> getTests() {
        if (this.resources.isEmpty()) {
            return Collections.emptyList();
        }
        final List<SingleTestClass> tests = new ArrayList<>();
        for (final Resource resource : resources) {
            if (!resource.isExists()) {
                continue;
            }
            final String name = resource.getName();
            // we only consider .class files
            if (!name.endsWith(".class")) {
                continue;
            }
            final String className = name.substring(0, name.lastIndexOf('.'));
            final BatchSourcedSingleTest test = new BatchSourcedSingleTest(className.replace(File.separatorChar, '.').replace('/', '.').replace('\\', '.'));
            tests.add(test);
        }
        return tests;
    }

    /**
     * A {@link BatchSourcedSingleTest} is similar to a {@link SingleTestClass} except that
     * some of the characteristics of the test (like whether to halt on failure) are borrowed
     * from the {@link TestClasses batch} to which this test belongs to
     */
    private final class BatchSourcedSingleTest extends SingleTestClass {

        private BatchSourcedSingleTest(final String testClassName) {
            this.setName(testClassName);
        }

        @Override
        String getIfProperty() {
            return TestClasses.this.getIfProperty();
        }

        @Override
        String getUnlessProperty() {
            return TestClasses.this.getUnlessProperty();
        }

        @Override
        boolean isHaltOnFailure() {
            return TestClasses.this.isHaltOnFailure();
        }

        @Override
        String getFailureProperty() {
            return TestClasses.this.getFailureProperty();
        }

        @Override
        List<ListenerDefinition> getListeners() {
            return TestClasses.this.getListeners();
        }

        @Override
        String getOutputDir() {
            return TestClasses.this.getOutputDir();
        }

        @Override
        String[] getIncludeEngines() {
            return TestClasses.this.getIncludeEngines();
        }

        @Override
        String[] getExcludeEngines() {
            return TestClasses.this.getExcludeEngines();
        }
    }
}
