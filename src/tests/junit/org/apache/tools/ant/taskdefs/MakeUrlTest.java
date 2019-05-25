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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class MakeUrlTest {

    @RunWith(Parameterized.class)
    public static class InvalidArgumentTest {

        @Rule
        public final BuildFileRule buildRule = new BuildFileRule();

        @Rule
        public ExpectedException thrown = ExpectedException.none();

        @Before
        public void setUp() {
            buildRule.configureProject("src/etc/testcases/taskdefs/makeurl.xml");
        }

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object[]> targets() {
            return Arrays.asList(new Object[][]{
                    {"testEmpty", "No property defined"},
                    {"testNoProperty", "No property defined"},
                    {"testNoFile", "No files defined"},
                    {"testValidation", "A source file is missing"}
            });
        }

        @Parameterized.Parameter
        public String targetName;

        @Parameterized.Parameter(1)
        public String message;

        @Test
        public void test() {
            thrown.expect(BuildException.class);
            thrown.expectMessage(message);
            buildRule.executeTarget(targetName);
        }
    }

    @RunWith(Parameterized.class)
    public static class ValidArgumentTest {

        @Rule
        public final BuildFileRule buildRule = new BuildFileRule();

        @Before
        public void setUp() {
            buildRule.configureProject("src/etc/testcases/taskdefs/makeurl.xml");
        }

        @Parameterized.Parameters(name = "{0}")
        public static Collection<Object[]> targets() {
            return Arrays.asList(new Object[][]{
                    {"testWorks", both(containsString("file:")).and(containsString("/foo"))},
                    {"testIllegalChars", both(containsString("file:")).and(containsString("fo%20o%25"))},
                    {"testRoundTrip", containsString("file:")},
                    {"testIllegalCombinations", both(containsString("/foo")).and(containsString(".xml"))},
                    {"testFileset", both(containsString(".xml ")).and(endsWith(".xml"))},
                    {"testFilesetSeparator", both(containsString(".xml\",\"")).and(endsWith(".xml"))},
                    {"testPath", containsString("makeurl.xml")}
            });
        }

        @Parameterized.Parameter
        public String targetName;

        @Parameterized.Parameter(1)
        public Matcher<String> matcher;

        @Test
        public void test() throws IOException {
            buildRule.executeTarget(targetName);
            String property = buildRule.getProject().getProperty(targetName);
            assertNotNull("property not set", property);
            assertThat(property, matcher);

            if (targetName.equals("testRoundTrip")) {
                // test that we can round trip by opening a url that exists
                URL url = new URL(property);
                InputStream instream = url.openStream();
                instream.close();
            }
        }
    }

}
