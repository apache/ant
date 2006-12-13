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

import org.apache.tools.ant.BuildFileTest;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;


public class MakeUrlTest extends BuildFileTest {

    public MakeUrlTest(String s) {
        super(s);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/makeurl.xml");
    }

    public void testEmpty() {
        expectBuildExceptionContaining("testEmpty", "missing property", "property");
    }

    public void testNoProperty() {
        expectBuildExceptionContaining("testNoProperty", "missing property", "property");
    }

    public void testNoFile() {
        expectBuildExceptionContaining("testNoFile", "missing file", "file");
    }

    public void testValidation() {
        expectBuildExceptionContaining("testValidation", MakeUrl.ERROR_MISSING_FILE, "file");
    }

    public void testWorks() {
        executeTarget("testWorks");
        assertPropertyContains("testWorks", "file:");
        assertPropertyContains("testWorks", "/foo");
    }

    public void testIllegalChars() {
        executeTarget("testIllegalChars");
        assertPropertyContains("testIllegalChars", "file:");
        assertPropertyContains("testIllegalChars", "fo%20o%25");
    }

    /**
     * test that we can round trip by opening a url that exists
     *
     * @throws IOException
     */
    public void testRoundTrip() throws IOException {
        executeTarget("testRoundTrip");
        assertPropertyContains("testRoundTrip", "file:");
        String property = getProperty("testRoundTrip");
        URL url = new URL(property);
        InputStream instream = url.openStream();
        instream.close();
    }

    public void testIllegalCombinations() {
        executeTarget("testIllegalCombinations");
        assertPropertyContains("testIllegalCombinations", "/foo");
        assertPropertyContains("testIllegalCombinations", ".xml");
    }

    public void testFileset() {
        executeTarget("testFileset");
        assertPropertyContains("testFileset", ".xml ");
        String result = getProperty("testFileset");
        assertPropertyEndsWith("testFileset", ".xml");
    }

    public void testFilesetSeparator() {
        executeTarget("testFilesetSeparator");
        assertPropertyContains("testFilesetSeparator", ".xml\",\"");
        assertPropertyEndsWith("testFilesetSeparator", ".xml");
    }

    public void testPath() {
        executeTarget("testPath");
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
        return project.getProperty(property);
    }
}
