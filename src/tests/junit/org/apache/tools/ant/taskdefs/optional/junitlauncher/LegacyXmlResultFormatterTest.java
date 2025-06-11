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

import org.apache.tools.ant.Project;
import org.junit.Test;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class LegacyXmlResultFormatterTest {

    private static final String KEY = "key";
    private static final String ORIG = "<\u0000&>foo";
    private static final String ENCODED = "&lt;&amp;#0;&amp;&gt;foo";

    private final LegacyXmlResultFormatter f = new LegacyXmlResultFormatter();

    @Test
    public void encodesAttributesProperly() throws Exception {
        final TestPlan plan = startTest(true);
        final String result = finishTest(plan);
        assertThat(result, containsString("=\"" + ENCODED + "\""));
    }

    @Test
    public void encodesSysOutProperly() throws Exception {
        final TestPlan plan = startTest(false);
        f.sysOutAvailable(ORIG.getBytes(StandardCharsets.UTF_8));
        final String result = finishTest(plan);
        assertThat(result, containsString(ENCODED));
    }

    @Test
    public void properlyReportsFailures() throws Exception {
        properlyReportsFailuresAndErrors(new AssertionError("failed", null), true);
    }

    @Test
    public void properlyReportsErrors() throws Exception {
        properlyReportsFailuresAndErrors(new NullPointerException("failed"), false);
    }

    private void properlyReportsFailuresAndErrors(Throwable errorOrFailure,
                                                  boolean shouldBeFailure)
        throws Exception {

        final TestPlan plan = startTest(false);
        final TestDescriptor testDescriptor = new DummyTestDescriptor("failure");
        final TestIdentifier test = TestIdentifier.from(testDescriptor);
        f.executionStarted(test);
        final TestExecutionResult testResult = TestExecutionResult.failed(errorOrFailure);
        f.executionFinished(test, testResult);
        final String result = finishTest(plan);

        final int expectedFailureCount = shouldBeFailure ? 1 : 0;
        final int expectedErrorCount = shouldBeFailure ? 0 : 1;

        final StreamSource source = new StreamSource() {
                @Override
                public Reader getReader() {
                    return new StringReader(result);
                }
            };
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final InputSource is = SAXSource.sourceToInputSource(source);
        final DocumentBuilder b = dbf.newDocumentBuilder();
        final Document doc = b.parse(is);
        final Element suite = doc.getDocumentElement();
        assertThat(suite.getTagName(), equalTo("testsuite"));
        assertThat(suite.getAttribute("failures"), equalTo(expectedFailureCount + ""));
        assertThat(suite.getAttribute("errors"), equalTo(expectedErrorCount + ""));
        final NodeList testCases = suite.getElementsByTagName("testcase");
        assertThat(testCases.getLength(), equalTo(1));
        final Node testCase = testCases.item(0);
        assertThat(testCase, instanceOf(Element.class));
        final Element testCaseElement = (Element) testCase;
        NodeList failureElements = testCaseElement.getElementsByTagName("failure");
        assertThat(failureElements.getLength(), equalTo(expectedFailureCount));
        NodeList errorElements = testCaseElement.getElementsByTagName("error");
        assertThat(errorElements.getLength(), equalTo(expectedErrorCount));
    }

    private TestPlan startTest(final boolean withProperties) {
        f.setContext(new TestExecutionContext() {
            @Override
            public Properties getProperties() {
                final Properties p = new Properties();
                if (withProperties) {
                    p.setProperty(KEY, ORIG);
                }
                return p;
            }

            @Override
            public Optional<Project> getProject() {
                return Optional.empty();
            }
        });
        final ConfigurationParameters dummyParams = new ConfigurationParameters() {
            @Override
            public Optional<String> get(String key) {
                return Optional.empty();
            }

            @Override
            public Optional<Boolean> getBoolean(String key) {
                return Optional.empty();
            }

            @Override
            public <T> Optional<T> get(String key, Function<String, T> transformer) {
                return Optional.empty();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public Set<String> keySet() {
                return Collections.emptySet();
            }
        };
        final TestPlan testPlan = TestPlan.from(Collections.emptySet(), dummyParams);
        f.testPlanExecutionStarted(testPlan);
        return testPlan;
    }

    private String finishTest(final TestPlan testPlan) throws IOException {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            f.setDestination(bos);
            f.testPlanExecutionFinished(testPlan);
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static class DummyTestDescriptor extends AbstractTestDescriptor {
        private DummyTestDescriptor(String displayName) {
            super(UniqueId.forEngine("testengine"), displayName);
        }

        @Override
        public TestDescriptor.Type getType() {
            return TestDescriptor.Type.TEST;
        }
    }
}
