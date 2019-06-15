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
package org.apache.tools.ant.util;

import java.io.File;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.junit.Test;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * TestCase for JavaEnvUtils.
 *
 */
public class JavaEnvUtilsTest {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    @Test
    public void testGetExecutableNetware() {
        assumeTrue("Test only runs on netware", Os.isName("netware"));
        assertEquals("java", JavaEnvUtils.getJreExecutable("java"));
        assertEquals("javac", JavaEnvUtils.getJdkExecutable("javac"));
        assertEquals("foo", JavaEnvUtils.getJreExecutable("foo"));
        assertEquals("foo", JavaEnvUtils.getJdkExecutable("foo"));
    }

    @Test
    public void testGetExecutableWindows() {
        assumeTrue("Test only runs on windows", Os.isFamily("windows"));
        String javaHome = FILE_UTILS.normalize(JavaEnvUtils.getJavaHome())
            .getAbsolutePath();

        String j = JavaEnvUtils.getJreExecutable("java");
        assertThat(j, endsWith(".exe"));
        assertTrue(j + " is absolute", (new File(j)).isAbsolute());
        try {
            assertThat(j + " is normalized and in the JRE dir", j, startsWith(javaHome));
        } catch (AssertionError e) {
            // java.home is bogus
            assertEquals("java.exe", j);
        }

        j = JavaEnvUtils.getJdkExecutable("javac");
        assertThat(j, endsWith(".exe"));

        try {
            assertTrue(j + " is absolute", (new File(j)).isAbsolute());
            String javaHomeParent = FILE_UTILS.normalize(javaHome + "/..").getAbsolutePath();
            assertThat(j + " is normalized and in the JDK dir", j, startsWith(javaHomeParent));
            if (JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9)) {
                assertThat(j + " is normalized and not in the JRE dir", j, startsWith(javaHome));
            } else {
                assertThat(j + " is normalized and not in the JRE dir", j, not(startsWith(javaHome)));
            }
        } catch (AssertionError e) {
            // java.home is bogus
            assertEquals("javac.exe", j);
        }

        assertEquals("foo.exe", JavaEnvUtils.getJreExecutable("foo"));
        assertEquals("foo.exe", JavaEnvUtils.getJdkExecutable("foo"));
    }

    @Test
    public void testGetExecutableMostPlatforms() {
        assumeFalse("Test only runs on non Netware and non Windows systems",
                Os.isName("netware") || Os.isFamily("windows"));
        String javaHome = FILE_UTILS.normalize(JavaEnvUtils.getJavaHome()).getAbsolutePath();

        // could still be OS/2
        String extension = Os.isFamily("dos") ? ".exe" : "";

        String j = JavaEnvUtils.getJreExecutable("java");
        if (!extension.isEmpty()) {
            assertThat(j, endsWith(extension));
        }
        assertTrue(j + " is absolute", (new File(j)).isAbsolute());
        assertThat(j + " is normalized and in the JRE dir", j, startsWith(javaHome));

        j = JavaEnvUtils.getJdkExecutable("javac");
        if (!extension.isEmpty()) {
            assertThat(j, endsWith(extension));
        }
        assertTrue(j + " is absolute", (new File(j)).isAbsolute());

        String javaHomeParent = FILE_UTILS.normalize(javaHome + "/..").getAbsolutePath();
        assertThat(j + " is normalized and in the JDK dir", j, startsWith(javaHomeParent));

        if (JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9)) {
            assertThat(j + " is normalized and in the JRE dir", j, startsWith(javaHome));
        } else {
            assertThat(j + " is normalized and not in the JRE dir", j, not(startsWith(javaHome)));
        }

        assertEquals("foo" + extension, JavaEnvUtils.getJreExecutable("foo"));
        assertEquals("foo" + extension, JavaEnvUtils.getJdkExecutable("foo"));
    }

    @Test
    public void testIsAtLeastJavaVersion() {
        assertTrue("Current java version is not at least the current java version...",
                JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.getJavaVersion()));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void isJavaVersionSupportsBothVersionsOfJava9() {
        assumeTrue(JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_9));
        assertTrue("JAVA_1_9 is not considered equal to JAVA_9",
                JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_9));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void java10IsDetectedProperly() {
        assumeTrue("10".equals(System.getProperty("java.specification.version")));
        assertEquals("10", JavaEnvUtils.getJavaVersion());
        assertEquals(100, JavaEnvUtils.getJavaVersionNumber());
        assertEquals(new DeweyDecimal("10"), JavaEnvUtils.getParsedJavaVersion());
        assertTrue(JavaEnvUtils.isJavaVersion("10"));
        assertTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9));
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void java11IsDetectedProperly() {
        assumeTrue("11".equals(System.getProperty("java.specification.version")));
        assertEquals("11", JavaEnvUtils.getJavaVersion());
        assertEquals(110, JavaEnvUtils.getJavaVersionNumber());
        assertEquals(new DeweyDecimal("11"), JavaEnvUtils.getParsedJavaVersion());
        assertTrue(JavaEnvUtils.isJavaVersion("11"));
        assertTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9));
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void java12IsDetectedProperly() {
        assumeTrue("12".equals(System.getProperty("java.specification.version")));
        assertEquals("12", JavaEnvUtils.getJavaVersion());
        assertEquals(120, JavaEnvUtils.getJavaVersionNumber());
        assertEquals(new DeweyDecimal("12"), JavaEnvUtils.getParsedJavaVersion());
        assertTrue(JavaEnvUtils.isJavaVersion("12"));
        assertTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9));
    }


}
