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
package org.apache.tools.ant.launch;

import java.io.File;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/** Test the locator in the ant-launch JAR */
public class LocatorTest {
    private boolean windows;
    private boolean unix;
    private static final String LAUNCHER_JAR = "//morzine/slo/Java/Apache/ant/lib/ant-launcher.jar";
    private static final String SHARED_JAR_URI = "jar:file:" + LAUNCHER_JAR
            + "!/org/apache/tools/ant/launch/Launcher.class";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
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
        if (enabled && expectedResult != null && !expectedResult.isEmpty()) {
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
     * this isn't really a valid URI, except maybe in IE
     */
    @Test
    public void testNetworkURI() {
        resolveTo("file:\\\\PC03\\jclasses\\lib\\ant-1.7.0.jar", ""
                + "\\\\PC03\\jclasses\\lib\\ant-1.7.0.jar",
                "\\\\PC03\\jclasses\\lib\\ant-1.7.0.jar");
    }

    @Ignore("We don't appear to generate paths like this in the launcher")
    @Test
    public void testTripleForwardSlashNetworkURI() {
        resolveTo("file:///PC03/jclasses/lib/ant-1.7.0.jar",
                "///PC03/jclasses/lib/ant-1.7.0.jar",
                "\\\\PC03\\jclasses\\lib\\ant-1.7.0.jar");
    }

    @Test
    public void testUnixNetworkPath() {
        resolveTo("file://cluster/home/ant/lib",
                "//cluster/home/ant/lib",
                "\\\\cluster\\home\\ant\\lib");
    }

    @Test
    public void testUnixPath() {
        resolveTo("file:/home/ant/lib", "/home/ant/lib", null);
    }

    @Test
    public void testSpacedURI() {
        resolveTo("file:C:\\Program Files\\Ant\\lib",
                "C:\\Program Files\\Ant\\lib",
                "C:\\Program Files\\Ant\\lib");
    }

    /**
     * Bug 42275; Ant failing to run off a remote share
     */
    @Test
    public void testAntOnRemoteShare() {
        String resolved = Locator.fromJarURI(SHARED_JAR_URI);
        assertResolved(SHARED_JAR_URI, LAUNCHER_JAR, resolved, unix);
        assertResolved(SHARED_JAR_URI, LAUNCHER_JAR.replace('/', '\\'),
                       resolved, windows);
    }

    /**
     * Bug 42275; Ant failing to run off a remote share
     */
    @Test
    public void testFileFromRemoteShare() {
        assumeTrue("not Windows", windows);
        String resolved = Locator.fromJarURI(SHARED_JAR_URI);
        File f = new File(resolved);
        String path = f.getAbsolutePath();
        assertEquals(0, path.indexOf("\\\\"));
    }

    @Test
    public void testHttpsURI() {
        String url = "https://ant.apache.org";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Locator.ERROR_NOT_FILE_URI + url);
        Locator.fromURI(url);
    }

    @Test
    public void testInternationalURI() throws Exception {
        String result = assertResolves("L\u00f6wenbrau.aus.M\u00fcnchen");
        char umlauted = result.charAt(1);
        assertEquals("expected 0xf6 (\u00f6), but got " + Integer.toHexString(umlauted) + " '"
                + umlauted + "'", 0xf6, umlauted);
        assertEquals("file:/tmp/a%C3%A7a%C3%AD%20berry", Locator.encodeURI("file:/tmp/a\u00E7a\u00ED berry"));
        assertEquals("file:/tmp/a\u00E7a\u00ED berry", Locator.decodeUri("file:/tmp/a%C3%A7a%C3%AD%20berry"));
        assertEquals("file:/tmp/a\u00E7a\u00ED berry", Locator.decodeUri("file:/tmp/a\u00E7a\u00ED%20berry")); // #50543
        assertEquals("file:/tmp/hezky \u010Desky", Locator.decodeUri("file:/tmp/hezky%20\u010Desky")); // non-ISO-8859-1 variant
    }

    @Test
    public void testOddLowAsciiURI() {
        assertResolves("hash# and percent%");
    }

}
