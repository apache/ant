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
package org.apache.tools.ant.taskdefs.optional.junitlauncher.confined;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.types.EnumeratedAttribute;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_CLASS_NAME;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_LISTENER_RESULT_FILE;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_LISTENER_USE_LEGACY_REPORTING_NAME;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_OUTPUT_DIRECTORY;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_SEND_SYS_ERR;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_SEND_SYS_OUT;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ELM_LISTENER;

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
    private String outputDir;
    private boolean useLegacyReportingName = true;

    public ListenerDefinition() {

    }

    public void setClassName(final String className) {
        this.className = className;
    }

    public String getClassName() {
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
                break;
            }
            case LEGACY_BRIEF: {
                this.setClassName("org.apache.tools.ant.taskdefs.optional.junitlauncher.LegacyBriefResultFormatter");
                break;
            }
            case LEGACY_XML: {
                this.setClassName("org.apache.tools.ant.taskdefs.optional.junitlauncher.LegacyXmlResultFormatter");
                break;
            }
        }
    }

    public void setResultFile(final String filename) {
        this.resultFile = filename;
    }

    public String getResultFile() {
        return this.resultFile;
    }

    public void setSendSysOut(final boolean sendSysOut) {
        this.sendSysOut = sendSysOut;
    }

    public boolean shouldSendSysOut() {
        return this.sendSysOut;
    }

    public void setSendSysErr(final boolean sendSysErr) {
        this.sendSysErr = sendSysErr;
    }

    public boolean shouldSendSysErr() {
        return this.sendSysErr;
    }

    /**
     * Sets the output directory for this listener
     *
     * @param dir Path to the output directory
     * @since Ant 1.10.6
     */
    public void setOutputDir(final String dir) {
        this.outputDir = dir;
    }

    public String getOutputDir() {
        return this.outputDir;
    }

    /**
     *
     * @return Returns {@code true} if legacy reporting name (JUnit 4 style) is to be used.
     *         Else returns {@code false}.
     * @since Ant 1.10.10
     */
    public boolean isUseLegacyReportingName() {
        return useLegacyReportingName;
    }

    /**
     * Set the test identifier reporting style
     * @param useLegacyReportingName {@code true} if legacy reporting name (JUnit 4 style) is to
     *                               be used. Else {@code false}.
     * @since Ant 1.10.10
     */
    public void setUseLegacyReportingName(final boolean useLegacyReportingName) {
        this.useLegacyReportingName = useLegacyReportingName;
    }

    public boolean shouldUse(final Project project) {
        final PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(project);
        return propertyHelper.testIfCondition(this.ifProperty) && propertyHelper.testUnlessCondition(this.unlessProperty);
    }

    /**
     * defines available listener types.
     */
    public static class ListenerType extends EnumeratedAttribute {

        /** {@inheritDoc}. */
        @Override
        public String[] getValues() {
            return new String[]{LEGACY_PLAIN, LEGACY_BRIEF, LEGACY_XML};
        }
    }

    void toForkedRepresentation(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(LD_XML_ELM_LISTENER);
        writer.writeAttribute(LD_XML_ATTR_CLASS_NAME, this.className);
        writer.writeAttribute(LD_XML_ATTR_SEND_SYS_ERR, Boolean.toString(this.sendSysErr));
        writer.writeAttribute(LD_XML_ATTR_SEND_SYS_OUT, Boolean.toString(this.sendSysOut));
        writer.writeAttribute(LD_XML_ATTR_LISTENER_USE_LEGACY_REPORTING_NAME, Boolean.toString(this.useLegacyReportingName));
        if (this.outputDir != null) {
            writer.writeAttribute(LD_XML_ATTR_OUTPUT_DIRECTORY, this.outputDir);
        }
        if (this.resultFile != null) {
            writer.writeAttribute(LD_XML_ATTR_LISTENER_RESULT_FILE, this.resultFile);
        }
        writer.writeEndElement();
    }

    public static ListenerDefinition fromForkedRepresentation(final XMLStreamReader reader) throws XMLStreamException {
        reader.require(XMLStreamConstants.START_ELEMENT, null, LD_XML_ELM_LISTENER);
        final ListenerDefinition listenerDef = new ListenerDefinition();
        final String className = requireAttributeValue(reader, LD_XML_ATTR_CLASS_NAME);
        listenerDef.setClassName(className);
        final String sendSysErr = reader.getAttributeValue(null, LD_XML_ATTR_SEND_SYS_ERR);
        if (sendSysErr != null) {
            listenerDef.setSendSysErr(Boolean.parseBoolean(sendSysErr));
        }
        final String sendSysOut = reader.getAttributeValue(null, LD_XML_ATTR_SEND_SYS_OUT);
        if (sendSysOut != null) {
            listenerDef.setSendSysOut(Boolean.parseBoolean(sendSysOut));
        }
        final String outputDir = reader.getAttributeValue(null, LD_XML_ATTR_OUTPUT_DIRECTORY);
        if (outputDir != null) {
            listenerDef.setOutputDir(outputDir);
        }
        final String resultFile = reader.getAttributeValue(null, LD_XML_ATTR_LISTENER_RESULT_FILE);
        if (resultFile != null) {
            listenerDef.setResultFile(resultFile);
        }
        final String useLegacyReportingName = reader.getAttributeValue(null,
                LD_XML_ATTR_LISTENER_USE_LEGACY_REPORTING_NAME);
        if (useLegacyReportingName != null) {
            listenerDef.setUseLegacyReportingName(Boolean.parseBoolean(useLegacyReportingName));
        }
        reader.nextTag();
        reader.require(XMLStreamConstants.END_ELEMENT, null, LD_XML_ELM_LISTENER);
        return listenerDef;
    }

    private static String requireAttributeValue(final XMLStreamReader reader, final String attrName) throws XMLStreamException {
        final String val = reader.getAttributeValue(null, attrName);
        if (val != null) {
            return val;
        }
        throw new XMLStreamException("Attribute " + attrName + " is missing at " + reader.getLocation());
    }

}
