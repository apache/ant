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
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static org.hamcrest.Matchers.containsString;
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
        final TestPlan testPlan = TestPlan.from(Collections.emptySet());
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
}
