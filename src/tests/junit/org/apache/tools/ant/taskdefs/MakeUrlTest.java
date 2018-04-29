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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertThat;

public class MakeUrlTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/makeurl.xml");
    }

    @Test
    public void testEmpty() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("No property defined");
        buildRule.executeTarget("testEmpty");
    }

    @Test
    public void testNoProperty() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("No property defined");
        buildRule.executeTarget("testNoProperty");
    }

    @Test
    public void testNoFile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("No files defined");
        buildRule.executeTarget("testNoFile");
    }

    @Test
    public void testValidation() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("A source file is missing");
        buildRule.executeTarget("testValidation");
    }

    @Test
    public void testWorks() {
        buildRule.executeTarget("testWorks");
        assertThat(buildRule.getProject().getProperty("testWorks"),
                both(containsString("file:")).and(containsString("/foo")));
    }

    @Test
    public void testIllegalChars() {
        buildRule.executeTarget("testIllegalChars");
        assertThat(buildRule.getProject().getProperty("testIllegalChars"),
                both(containsString("file:")).and(containsString("fo%20o%25")));
    }

    /**
     * test that we can round trip by opening a url that exists
     *
     * @throws IOException if something goes wrong
     */
    @Test
    public void testRoundTrip() throws IOException {
        buildRule.executeTarget("testRoundTrip");
        String property = buildRule.getProject().getProperty("testRoundTrip");
        assertThat(property, containsString("file:"));
        URL url = new URL(property);
        InputStream instream = url.openStream();
        instream.close();
    }

    @Test
    public void testIllegalCombinations() {
        buildRule.executeTarget("testIllegalCombinations");
        assertThat(buildRule.getProject().getProperty("testIllegalCombinations"),
                both(containsString("/foo")).and(containsString(".xml")));
    }

    @Test
    public void testFileset() {
        buildRule.executeTarget("testFileset");
        assertThat(buildRule.getProject().getProperty("testFileset"),
                both(containsString(".xml ")).and(endsWith(".xml")));
    }

    @Test
    public void testFilesetSeparator() {
        buildRule.executeTarget("testFilesetSeparator");
        assertThat(buildRule.getProject().getProperty("testFilesetSeparator"),
                both(containsString(".xml\",\"")).and(endsWith(".xml")));
    }

    @Test
    public void testPath() {
        buildRule.executeTarget("testPath");
        assertThat(buildRule.getProject().getProperty("testPath"), containsString("makeurl.xml"));
    }

}
