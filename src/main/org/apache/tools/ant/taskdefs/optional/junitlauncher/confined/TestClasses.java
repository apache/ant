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

import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Resources;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_CLASS_NAME;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_EXCLUDE_ENGINES;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_HALT_ON_FAILURE;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_INCLUDE_ENGINES;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_OUTPUT_DIRECTORY;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ELM_TEST;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ELM_TEST_CLASSES;

/**
 * Represents a {@code testclasses} that's configured to be launched by the {@link JUnitLauncherTask}
 */
public class TestClasses extends TestDefinition {

    private final Resources resources = new Resources();

    public TestClasses() {

    }

    public void add(final ResourceCollection resourceCollection) {
        this.resources.add(resourceCollection);
    }

    public List<String> getTestClassNames() {
        if (this.resources.isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> tests = new ArrayList<>();
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
            tests.add(className.replace(File.separatorChar, '.').replace('/', '.').replace('\\', '.'));
        }
        return tests;
    }

    @Override
    protected void toForkedRepresentation(final JUnitLauncherTask task, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(LD_XML_ELM_TEST_CLASSES);
        // write out each test class
        for (final String test : getTestClassNames()) {
            writer.writeStartElement(LD_XML_ELM_TEST);
            writer.writeAttribute(LD_XML_ATTR_CLASS_NAME, test);
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
        writer.writeEndElement();
    }

    public static List<TestDefinition> fromForkedRepresentation(final XMLStreamReader reader) throws XMLStreamException {
        reader.require(XMLStreamConstants.START_ELEMENT, null, LD_XML_ELM_TEST_CLASSES);
        final List<TestDefinition> testDefinitions = new ArrayList<>();
        // read out as multiple SingleTestClass representations
        while (reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            final SingleTestClass testDefinition = new SingleTestClass();
            reader.require(XMLStreamConstants.START_ELEMENT, null, Constants.LD_XML_ELM_TEST);
            final String testClassName = requireAttributeValue(reader, LD_XML_ATTR_CLASS_NAME);
            testDefinition.setName(testClassName);
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
            reader.require(XMLStreamConstants.END_ELEMENT, null, Constants.LD_XML_ELM_TEST);
            testDefinitions.add(testDefinition);
        }
        reader.require(XMLStreamConstants.END_ELEMENT, null, LD_XML_ELM_TEST_CLASSES);
        return Collections.unmodifiableList(testDefinitions);
    }

    private static String requireAttributeValue(final XMLStreamReader reader, final String attrName) throws XMLStreamException {
        final String val = reader.getAttributeValue(null, attrName);
        if (val != null) {
            return val;
        }
        throw new XMLStreamException("Attribute " + attrName + " is missing at " + reader.getLocation());
    }
}
