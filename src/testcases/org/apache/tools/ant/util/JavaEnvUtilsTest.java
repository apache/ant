/*
 * Copyright  2002-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.util;

import java.io.File;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.tools.ant.taskdefs.condition.Os;

/**
 * TestCase for JavaEnvUtils.
 *
 */
public class JavaEnvUtilsTest extends TestCase {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    public JavaEnvUtilsTest(String s) {
        super(s);
    }

    public void testGetExecutableNetware() {
        if (Os.isName("netware")) {
            assertEquals("java", JavaEnvUtils.getJreExecutable("java"));
            assertEquals("javac", JavaEnvUtils.getJdkExecutable("javac"));
            assertEquals("foo", JavaEnvUtils.getJreExecutable("foo"));
            assertEquals("foo", JavaEnvUtils.getJdkExecutable("foo"));
        }
    }

    public void testGetExecutableWindows() {
        if (Os.isFamily("windows")) {
            String javaHome =
                FILE_UTILS.normalize(System.getProperty("java.home"))
                .getAbsolutePath();

            String j = JavaEnvUtils.getJreExecutable("java");
            assertTrue(j.endsWith(".exe"));
            assertTrue(j+" is absolute", (new File(j)).isAbsolute());
            try {
                assertTrue(j+" is normalized and in the JRE dir",
                           j.startsWith(javaHome));
            } catch (AssertionFailedError e) {
                // java.home is bogus
                assertEquals("java.exe", j);
            }

            j = JavaEnvUtils.getJdkExecutable("javac");
            assertTrue(j.endsWith(".exe"));
            try {
                assertTrue(j+" is absolute", (new File(j)).isAbsolute());
                String javaHomeParent =
                    FILE_UTILS.normalize(javaHome+"/..").getAbsolutePath();
                assertTrue(j+" is normalized and in the JDK dir",
                           j.startsWith(javaHomeParent));

                if (JavaEnvUtils.getJavaVersion() == JavaEnvUtils.JAVA_1_0 ||
                    JavaEnvUtils.getJavaVersion() == JavaEnvUtils.JAVA_1_1) {
                    assertTrue(j+" is normalized and in the JRE dir",
                               j.startsWith(javaHome));
                } else {
                    assertTrue(j+" is normalized and not in the JRE dir",
                               !j.startsWith(javaHome));
                }

            } catch (AssertionFailedError e) {
                // java.home is bogus
                assertEquals("javac.exe", j);
            }

            assertEquals("foo.exe", JavaEnvUtils.getJreExecutable("foo"));
            assertEquals("foo.exe", JavaEnvUtils.getJdkExecutable("foo"));
        }
    }

    public void testGetExecutableMostPlatforms() {
        if (!Os.isName("netware") && !Os.isFamily("windows")) {
            String javaHome =
                FILE_UTILS.normalize(System.getProperty("java.home"))
                .getAbsolutePath();

            // could still be OS/2
            String extension = Os.isFamily("dos") ? ".exe" : "";

            String j = JavaEnvUtils.getJreExecutable("java");
            if (!extension.equals("")) {
                assertTrue(j.endsWith(extension));
            }
            assertTrue(j+" is absolute", (new File(j)).isAbsolute());
            assertTrue(j+" is normalized and in the JRE dir",
                       j.startsWith(javaHome));

            j = JavaEnvUtils.getJdkExecutable("javac");
            if (!extension.equals("")) {
                assertTrue(j.endsWith(extension));
            }
            assertTrue(j+" is absolute", (new File(j)).isAbsolute());

            String javaHomeParent =
                FILE_UTILS.normalize(javaHome+"/..").getAbsolutePath();
            assertTrue(j+" is normalized and in the JDK dir",
                       j.startsWith(javaHomeParent));

            if (JavaEnvUtils.getJavaVersion() == JavaEnvUtils.JAVA_1_0 ||
                JavaEnvUtils.getJavaVersion() == JavaEnvUtils.JAVA_1_1 ||
                Os.isFamily("mac")) {
                assertTrue(j+" is normalized and in the JRE dir",
                           j.startsWith(javaHome));
            } else {
                assertTrue(j+" is normalized and not in the JRE dir",
                           !j.startsWith(javaHome));
            }

            assertEquals("foo"+extension,
                         JavaEnvUtils.getJreExecutable("foo"));
            assertEquals("foo"+extension,
                         JavaEnvUtils.getJdkExecutable("foo"));
        }

    }

}
