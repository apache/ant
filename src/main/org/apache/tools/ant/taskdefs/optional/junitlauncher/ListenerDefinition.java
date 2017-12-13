package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Represents the {@code &lt;listener&gt;} element within the {@code &lt;junitlauncher&gt;}
 * task
 */
public class ListenerDefinition {

    private static final String LEGACY_PLAIN = "legacy-plain";
    private static final String LEGACY_BRIEF = "legacy-brief";
    private static final String LEGACY_XML = "legacy-xml";

    private String ifProperty;
    private String unlessProperty;
    private String className;
    private String resultFile;
    private boolean sendSysOut;
    private boolean sendSysErr;

    private String defaultResultFileSuffix = "txt";

    public ListenerDefinition() {

    }

    public void setClassName(final String className) {
        this.className = className;
    }

    String getClassName() {
        return this.className;
    }

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

    public void setType(final ListenerType type) {
        switch (type.getValue()) {
            case LEGACY_PLAIN: {
                this.setClassName("org.apache.tools.ant.taskdefs.optional.junitlauncher.LegacyPlainResultFormatter");
                this.defaultResultFileSuffix = "txt";
                break;
            }
            case LEGACY_BRIEF: {
                this.setClassName("org.apache.tools.ant.taskdefs.optional.junitlauncher.LegacyBriefResultFormatter");
                this.defaultResultFileSuffix = "txt";
                break;
            }
            case LEGACY_XML: {
                this.setClassName("org.apache.tools.ant.taskdefs.optional.junitlauncher.LegacyXmlResultFormatter");
                this.defaultResultFileSuffix = "xml";
                break;
            }
        }
    }

    public void setResultFile(final String filename) {
        this.resultFile = filename;
    }

    String requireResultFile(final TestDefinition test) {
        if (this.resultFile != null) {
            return this.resultFile;
        }
        final StringBuilder sb = new StringBuilder("TEST-");
        if (test instanceof NamedTest) {
            sb.append(((NamedTest) test).getName());
        } else {
            sb.append("unknown");
        }
        sb.append(".").append(this.defaultResultFileSuffix);
        return sb.toString();
    }

    public void setSendSysOut(final boolean sendSysOut) {
        this.sendSysOut = sendSysOut;
    }

    boolean shouldSendSysOut() {
        return this.sendSysOut;
    }

    public void setSendSysErr(final boolean sendSysErr) {
        this.sendSysErr = sendSysErr;
    }

    boolean shouldSendSysErr() {
        return this.sendSysErr;
    }

    protected boolean shouldUse(final Project project) {
        final PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(project);
        return propertyHelper.testIfCondition(this.ifProperty) && propertyHelper.testUnlessCondition(this.unlessProperty);
    }

    public static class ListenerType extends EnumeratedAttribute {

        @Override
        public String[] getValues() {
            return new String[]{LEGACY_PLAIN, LEGACY_BRIEF, LEGACY_XML};
        }
    }

}
