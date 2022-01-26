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
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class LegacyXmlResultFormatterTest {

    private static final String KEY = "key";
    private static final String ORIG = "<\u0000&>foo";
    private static final String ENCODED = "&lt;&amp;#0;&amp;&gt;foo";
    private static final String CDATA_START = "<![CDATA[";
    private static final String CDATA_END = "]]>";

    private final LegacyXmlResultFormatter f = new LegacyXmlResultFormatter();

    @Test
    public void encodesAttributesProperly() throws Exception {
        final String result = this.runTest(true, "org.example.junitlauncher.jupiter.JupiterSampleTest");
        assertThat(result, containsString("=\"" + ENCODED + "\""));
    }

    @Test
    public void encodesSysOutProperly() throws Exception {
        f.sysOutAvailable(ORIG.getBytes(StandardCharsets.UTF_8));
        final String result = this.runTest(false, "org.example.junitlauncher.jupiter.JupiterSampleTest");
        assertThat(result, containsString(ENCODED));
    }

    @Test
    public void testEncodesCDataProperly() {
        final String result = assertDoesNotThrow(() -> this.runTest(false, "org.example.junitlauncher.jupiter.JupiterCDataTest"));
        assertThat(result, containsString("]]" + CDATA_END + CDATA_START + ">"));
        // Just in case someone decides fixing writeCData is worth breaking other projects.
        assertThat(result, not(containsString("]]]]" + CDATA_END + CDATA_START + ">" + CDATA_START + ">")));
    }

    /**
     * Create a {@link TestPlan} to pass to the formatter
     * @param withProperties {@code true} if we want to set {@link #KEY} to {@link #ORIG}.
     * @param testClass The class to test
     */
    private void startTest(final boolean withProperties, final String testClass) {
        final Launcher launcher = LauncherFactory.create(LauncherConfig.builder().addTestExecutionListeners(this.f).build());
        LauncherDiscoveryRequestBuilder request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(testClass));
        if (withProperties) {
            request.configurationParameter(KEY, ORIG);
        }
        final TestPlan testPlan = launcher.discover(request.build());
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
        launcher.execute(testPlan, f);
    }

    private String runTest(final boolean withProperties, final String testClass) throws IOException {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            f.setDestination(bos);
            this.startTest(withProperties, testClass);
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
