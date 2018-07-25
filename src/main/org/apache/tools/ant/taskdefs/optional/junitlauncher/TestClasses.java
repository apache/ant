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

import static org.apache.tools.ant.taskdefs.optional.junitlauncher.Constants.LD_XML_ELM_TEST_CLASSES;

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

    @Override
    List<TestRequest> createTestRequests() {
        final List<SingleTestClass> tests = this.getTests();
        if (tests.isEmpty()) {
            return Collections.emptyList();
        }
        final List<TestRequest> requests = new ArrayList<>();
        for (final SingleTestClass test : tests) {
            requests.addAll(test.createTestRequests());
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

    @Override
    protected void toForkedRepresentation(final JUnitLauncherTask task, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(LD_XML_ELM_TEST_CLASSES);
        // write out as multiple SingleTestClass representations
        for (final SingleTestClass singleTestClass : getTests()) {
            singleTestClass.toForkedRepresentation(task, writer);
        }
        writer.writeEndElement();
    }

    static List<TestDefinition> fromForkedRepresentation(final XMLStreamReader reader) throws XMLStreamException {
        reader.require(XMLStreamConstants.START_ELEMENT, null, LD_XML_ELM_TEST_CLASSES);
        final List<TestDefinition> testDefinitions = new ArrayList<>();
        // read out as multiple SingleTestClass representations
        while (reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            reader.require(XMLStreamConstants.START_ELEMENT, null, Constants.LD_XML_ELM_TEST);
            testDefinitions.add(SingleTestClass.fromForkedRepresentation(reader));
        }
        reader.require(XMLStreamConstants.END_ELEMENT, null, LD_XML_ELM_TEST_CLASSES);
        return testDefinitions;
    }
}
