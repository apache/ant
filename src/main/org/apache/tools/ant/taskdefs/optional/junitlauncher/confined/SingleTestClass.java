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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_CLASS_NAME;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_EXCLUDE_ENGINES;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_HALT_ON_FAILURE;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_INCLUDE_ENGINES;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_METHODS;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_OUTPUT_DIRECTORY;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ELM_TEST;

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

    public String[] getMethods() {
        if (!hasMethodsSpecified()) {
            return null;
        }
        return this.testMethods.toArray(new String[0]);
    }

    @Override
    protected void toForkedRepresentation(final JUnitLauncherTask task, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(LD_XML_ELM_TEST);
        writer.writeAttribute(LD_XML_ATTR_CLASS_NAME, testClass);
        if (testMethods != null) {
            final StringBuilder sb = new StringBuilder();
            for (final String method : testMethods) {
                if (sb.length() != 0) {
                    sb.append(",");
                }
                sb.append(method);
            }
            writer.writeAttribute(LD_XML_ATTR_METHODS, sb.toString());
        }
        if (haltOnFailure != null) {
            writer.writeAttribute(LD_XML_ATTR_HALT_ON_FAILURE, haltOnFailure.toString());
        }
        if (outputDir != null) {
            writer.writeAttribute(LD_XML_ATTR_OUTPUT_DIRECTORY, outputDir);
        }
        if (includeEngines != null) {
            writer.writeAttribute(LD_XML_ATTR_INCLUDE_ENGINES, includeEngines);
        }
        if (excludeEngines != null) {
            writer.writeAttribute(LD_XML_ATTR_EXCLUDE_ENGINES, excludeEngines);
        }
        // listeners for this test
        if (listeners != null) {
            for (final ListenerDefinition listenerDef : getListeners()) {
                if (!listenerDef.shouldUse(task.getProject())) {
                    // not applicable
                    continue;
                }
                listenerDef.toForkedRepresentation(writer);
            }
        }
        writer.writeEndElement();
    }

    public static TestDefinition fromForkedRepresentation(final XMLStreamReader reader) throws XMLStreamException {
        reader.require(XMLStreamConstants.START_ELEMENT, null, LD_XML_ELM_TEST);
        final SingleTestClass testDefinition = new SingleTestClass();
        final String testClassName = requireAttributeValue(reader, LD_XML_ATTR_CLASS_NAME);
        testDefinition.setName(testClassName);
        final String methodNames = reader.getAttributeValue(null, LD_XML_ATTR_METHODS);
        if (methodNames != null) {
            testDefinition.setMethods(methodNames);
        }
        final String halt = reader.getAttributeValue(null, LD_XML_ATTR_HALT_ON_FAILURE);
        if (halt != null) {
            testDefinition.setHaltOnFailure(Boolean.parseBoolean(halt));
        }
        final String outDir = reader.getAttributeValue(null, LD_XML_ATTR_OUTPUT_DIRECTORY);
        if (outDir != null) {
            testDefinition.setOutputDir(outDir);
        }
        final String includeEngs = reader.getAttributeValue(null, LD_XML_ATTR_INCLUDE_ENGINES);
        if (includeEngs != null) {
            testDefinition.setIncludeEngines(includeEngs);
        }
        final String excludeEngs = reader.getAttributeValue(null, LD_XML_ATTR_EXCLUDE_ENGINES);
        if (excludeEngs != null) {
            testDefinition.setExcludeEngines(excludeEngs);
        }
        while (reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            reader.require(XMLStreamConstants.START_ELEMENT, null, Constants.LD_XML_ELM_LISTENER);
            testDefinition.addConfiguredListener(ListenerDefinition.fromForkedRepresentation(reader));
        }
        return testDefinition;
    }

    private static String requireAttributeValue(final XMLStreamReader reader, final String attrName) throws XMLStreamException {
        final String val = reader.getAttributeValue(null, attrName);
        if (val != null) {
            return val;
        }
        throw new XMLStreamException("Attribute " + attrName + " is missing at " + reader.getLocation());
    }
}
