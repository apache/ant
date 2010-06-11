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
package org.apache.tools.ant.launch;

import junit.framework.TestCase;

import java.io.File;

import org.apache.tools.ant.taskdefs.condition.Os;

/** Test the locator in the ant-launch JAR */
public class LocatorTest extends TestCase {
    private boolean windows;
    private boolean unix;
    private static final String LAUNCHER_JAR = "//morzine/slo/Java/Apache/ant/lib/ant-launcher.jar";
    private static final String SHARED_JAR_URI = "jar:file:"+ LAUNCHER_JAR +"!/org/apache/tools/ant/launch/Launcher.class";

    /**
     * No-arg constructor to enable serialization. This method is not intended to be used by mere mortals without calling
     * setName().
     */
    public LocatorTest() {
    }

    /** Constructs a test case with the given name.
     * @param name
     */
    public LocatorTest(String name) {
        super(name);
    }

    /**
     * Sets up the fixture, for example, open a network connection.
     * This method is called before a test is executed.
     */
    protected void setUp() throws Exception {
        super.setUp();
        windows = Os.isFamily(Os.FAMILY_DOS);
        unix = Os.isFamily(Os.FAMILY_UNIX);
    }

    /**
     * expect a uri to resolve to strings on different platforms
     * @param uri uri to parse
     * @param expectedUnix unix string (or null to skip that test)
     * @param expectedDos DOS string (or null to skip that test)
     * @return the resolved string
     */
    private String resolveTo(String uri, String expectedUnix, String expectedDos) {
        String result = Locator.fromURI(uri);
        assertResolved(uri, expectedUnix, result, unix);
        assertResolved(uri, expectedDos, result, windows);
        return result;
    }

    /**
     * Assert something resolved
     * @param uri original URI
     * @param expectedResult what we expected
     * @param result what we got
     * @param enabled is the test enabled?
     */
    private void assertResolved(String uri, String expectedResult, String result, boolean enabled) {
        if (enabled && expectedResult != null && expectedResult.length() > 0) {
            assertEquals("Expected " + uri + " to resolve to \n" + expectedResult + "\n but got\n"
                    + result + "\n", expectedResult, result);
        }
    }

    /**
     * This asserts that we can round trip the path to a URI and back again
     * @param path filename with no directory separators
     * @return the trailing filename
     */
    private String assertResolves(String path) {
        String asuri = new File(path).toURI().toASCIIString();
        String fullpath = System.getProperty("user.dir") + File.separator + path;
        String result = resolveTo(asuri, fullpath, fullpath);
        return result.substring(result.lastIndexOf(File.separatorChar) + 1);
    }


    /**
     * this isnt really a valid URI, except maybe in IE
     * @throws Exception
     */
    public void testNetworkURI() throws Exception {
        resolveTo("file:\\\\PC03\\jclasses\\lib\\ant-1.7.0.jar", ""
                + "\\\\PC03\\jclasses\\lib\\ant-1.7.0.jar",
                "\\\\PC03\\jclasses\\lib\\ant-1.7.0.jar");
    }

    /**
     * This is not being tested as we don't appear to generate paths like this in the launcher
     * @throws Exception
     */
    public void NotestTripleForwardSlashNetworkURI() throws Exception {
        resolveTo("file:///PC03/jclasses/lib/ant-1.7.0.jar",
                "///PC03/jclasses/lib/ant-1.7.0.jar",
                "\\\\PC03\\jclasses\\lib\\ant-1.7.0.jar");
    }

    public void testUnixNetworkPath() throws Exception {
        resolveTo("file://cluster/home/ant/lib",
                "//cluster/home/ant/lib",
                "\\\\cluster\\home\\ant\\lib");
    }

    public void testUnixPath() throws Exception {
        resolveTo("file:/home/ant/lib", "/home/ant/lib", null);
    }

    public void testSpacedURI() throws Exception {
        resolveTo("file:C:\\Program Files\\Ant\\lib",
                "C:\\Program Files\\Ant\\lib",
                "C:\\Program Files\\Ant\\lib");
    }

    /**
     * Bug 42275; Ant failing to run off a remote share
     * @throws Throwable if desired
     */
    public void testAntOnRemoteShare() throws Throwable {
        String resolved=Locator.fromJarURI(SHARED_JAR_URI);
        assertResolved(SHARED_JAR_URI, LAUNCHER_JAR, resolved, unix);
        assertResolved(SHARED_JAR_URI, LAUNCHER_JAR.replace('/', '\\'),
                       resolved, windows);
    }

    /**
     * Bug 42275; Ant failing to run off a remote share
     *
     * @throws Throwable if desired
     */
    public void testFileFromRemoteShare() throws Throwable {
        String resolved = Locator.fromJarURI(SHARED_JAR_URI);
        File f = new File(resolved);
        String path = f.getAbsolutePath();
        if (windows) {
            assertEquals(0, path.indexOf("\\\\"));
        }
    }

    public void testHttpURI() throws Exception {
        String url = "http://ant.apache.org";
        try {
            Locator.fromURI(url);
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertTrue(message, message.indexOf(Locator.ERROR_NOT_FILE_URI) >= 0);
            assertTrue(message, message.indexOf(url) >= 0);
        }
    }

    public void testInternationalURI() throws Exception {
        String result = assertResolves("L\u00f6wenbrau.aus.M\u00fcnchen");
        char umlauted = result.charAt(1);
        assertEquals("expected 0xf6 (\u00f6), but got " + Integer.toHexString(umlauted) + " '"
                + umlauted + "'", 0xf6, umlauted);
    }

    public void testOddLowAsciiURI() throws Exception {
        assertResolves("hash# and percent%");
    }

}
