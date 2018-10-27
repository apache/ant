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

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class MakeUrlTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/makeurl.xml");
    }

    @Test
    public void testEmpty() {
        try {
			buildRule.executeTarget("testEmpty");
			fail("BuildException expected: missing property");
		} catch (BuildException ex) {
			assertContains("property", ex.getMessage());
		}
    }

    @Test
    public void testNoProperty() {
        try {
			buildRule.executeTarget("testNoProperty");
			fail("BuildException expected: missing property");
		} catch (BuildException ex) {
			assertContains("property", ex.getMessage());
		}
    }

    @Test
    public void testNoFile() {
        try {
			buildRule.executeTarget("testNoFile");
			fail("BuildException expected: missing file");
		} catch (BuildException ex) {
			assertContains("file", ex.getMessage());
		}
    }

    @Test
    public void testValidation() {
        try {
			buildRule.executeTarget("testValidation");
			fail("BuildException expected: " + MakeUrl.ERROR_MISSING_FILE);
		} catch (BuildException ex) {
			assertContains("file", ex.getMessage());
		}
    }

    @Test
    public void testWorks() {
        buildRule.executeTarget("testWorks");
        assertPropertyContains("testWorks", "file:");
        assertPropertyContains("testWorks", "/foo");
    }

    @Test
    public void testIllegalChars() {
        buildRule.executeTarget("testIllegalChars");
        assertPropertyContains("testIllegalChars", "file:");
        assertPropertyContains("testIllegalChars", "fo%20o%25");
    }

    /**
     * test that we can round trip by opening a url that exists
     *
     * @throws IOException
     */
    @Test
    public void testRoundTrip() throws IOException {
        buildRule.executeTarget("testRoundTrip");
        assertPropertyContains("testRoundTrip", "file:");
        String property = getProperty("testRoundTrip");
        URL url = new URL(property);
        InputStream instream = url.openStream();
        instream.close();
    }

    @Test
    public void testIllegalCombinations() {
        buildRule.executeTarget("testIllegalCombinations");
        assertPropertyContains("testIllegalCombinations", "/foo");
        assertPropertyContains("testIllegalCombinations", ".xml");
    }

    @Test
    public void testFileset() {
        buildRule.executeTarget("testFileset");
        assertPropertyContains("testFileset", ".xml ");
        assertPropertyEndsWith("testFileset", ".xml");
    }

    @Test
    public void testFilesetSeparator() {
        buildRule.executeTarget("testFilesetSeparator");
        assertPropertyContains("testFilesetSeparator", ".xml\",\"");
        assertPropertyEndsWith("testFilesetSeparator", ".xml");
    }

    @Test
    public void testPath() {
        buildRule.executeTarget("testPath");
        assertPropertyContains("testPath", "makeurl.xml");
    }

    /**
     * assert that a property ends with
     *
     * @param property
     * @param ending
     */
    private void assertPropertyEndsWith(String property, String ending) {
        String result = getProperty(property);
        String substring = result.substring(result.length() - ending.length());
        assertEquals(ending, substring);
    }

    /**
     * assert that a property contains a string
     *
     * @param property name of property to look for
     * @param contains what to search for in the string
     */
    protected void assertPropertyContains(String property, String contains) {
        String result = getProperty(property);

        assertTrue("expected " + contains + " in " + result,
                result != null && result.indexOf(contains) >= 0);
    }

    /**
     * get a property from the project
     *
     * @param property
     * @return
     */
    protected String getProperty(String property) {
        return buildRule.getProject().getProperty(property);
    }
}
