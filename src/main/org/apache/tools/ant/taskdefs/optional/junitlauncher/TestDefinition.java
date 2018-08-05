package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the configuration details of a test that needs to be launched by the {@link JUnitLauncherTask}
 */
abstract class TestDefinition {

    protected String ifProperty;
    protected String unlessProperty;
    protected Boolean haltOnFailure;
    protected String failureProperty;
    protected String outputDir;
    protected String includeEngines;
    protected String excludeEngines;
    protected ForkDefinition forkDefinition;

    protected List<ListenerDefinition> listeners = new ArrayList<>();

    String getIfProperty() {
        return ifProperty;
    }

    public void setIf(final String ifProperty) {
        this.ifProperty = ifProperty;
    }

    String getUnlessProperty() {
        return unlessProperty;
    }

    public void setUnless(final String unlessProperty) {
        this.unlessProperty = unlessProperty;
    }

    boolean isHaltOnFailure() {
        return this.haltOnFailure != null && this.haltOnFailure;
    }

    Boolean getHaltOnFailure() {
        return this.haltOnFailure;
    }

    public void setHaltOnFailure(final boolean haltonfailure) {
        this.haltOnFailure = haltonfailure;
    }

    String getFailureProperty() {
        return failureProperty;
    }

    public void setFailureProperty(final String failureProperty) {
        this.failureProperty = failureProperty;
    }

    public void addConfiguredListener(final ListenerDefinition listener) {
        this.listeners.add(listener);
    }

    List<ListenerDefinition> getListeners() {
        return Collections.unmodifiableList(this.listeners);
    }

    public void setOutputDir(final String dir) {
        this.outputDir = dir;
    }

    String getOutputDir() {
        return this.outputDir;
    }

    public ForkDefinition createFork() {
        if (this.forkDefinition != null) {
            throw new BuildException("Test definition cannot have more than one fork elements");
        }
        this.forkDefinition = new ForkDefinition();
        return this.forkDefinition;
    }

    ForkDefinition getForkDefinition() {
        return this.forkDefinition;
    }

    abstract List<TestRequest> createTestRequests();

    protected boolean shouldRun(final Project project) {
        final PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(project);
        return propertyHelper.testIfCondition(this.ifProperty) && propertyHelper.testUnlessCondition(this.unlessProperty);
    }

    String[] getIncludeEngines() {
        return includeEngines == null ? new String[0] : split(this.includeEngines, ",");
    }

    public void setIncludeEngines(final String includeEngines) {
        this.includeEngines = includeEngines;
    }

    String[] getExcludeEngines() {
        return excludeEngines == null ? new String[0] : split(this.excludeEngines, ",");
    }

    public void setExcludeEngines(final String excludeEngines) {
        this.excludeEngines = excludeEngines;
    }

    private static String[] split(final String value, final String delimiter) {
        if (value == null) {
            return new String[0];
        }
        final List<String> parts = new ArrayList<>();
        for (final String part : value.split(delimiter)) {
            if (part.trim().isEmpty()) {
                // skip it
                continue;
            }
            parts.add(part);
        }
        return parts.toArray(new String[parts.size()]);
    }

    protected abstract void toForkedRepresentation(JUnitLauncherTask task, XMLStreamWriter writer) throws XMLStreamException;

}
