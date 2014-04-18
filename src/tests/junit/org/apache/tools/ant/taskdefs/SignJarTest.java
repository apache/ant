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

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testcase for the Signjar task
 *
 */
public class SignJarTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/signjar.xml");
    }

    /**
     * check for being offline
     * @return true if the system property "offline" is "true"
     */
    private boolean isOffline() {
        return Boolean.getBoolean("offline");
    }

    @Test
    public void testSigFile() {
        buildRule.executeTarget("sigfile");
        SignJarChild sj = new SignJarChild();
        sj.setAlias("testonly");
        sj.setKeystore("testkeystore");
        sj.setStorepass("apacheant");
        File jar = new File(buildRule.getProject().getProperty("test.jar"));
        sj.setJar(jar);
        assertFalse("mustn't find signature without sigfile attribute",
                    sj.isSigned());
        sj.setSigfile("TEST");
        assertTrue("must find signature with sigfile attribute",
                   sj.isSigned());
    }

    @Test
    public void testInvalidChars() {
        buildRule.executeTarget("invalidchars");
        SignJarChild sj = new SignJarChild();
        sj.setAlias("test@nly");
        sj.setKeystore("testkeystore");
        sj.setStorepass("apacheant");
        File jar = new File(buildRule.getProject().getProperty("test.jar"));
        sj.setJar(jar);
        assertTrue(sj.isSigned());
    }

    /**
     * subclass in order to get access to protected isSigned method if
     * tests and task come from different classloaders.
     */
    private static class SignJarChild extends SignJar {
        public boolean isSigned() {
            return isSigned(jar);
        }
    }

    @Test
    public void testURLKeystoreFile() {
       buildRule.executeTarget("urlKeystoreFile");
    }

    @Test
    public void testURLKeystoreHTTP() {
        Assume.assumeFalse("Test is set offline", isOffline());
        buildRule.executeTarget("urlKeystoreHTTP");
    }

    @Test
    public void testTsaLocalhost() {
        Assume.assumeTrue("Only runs on Java 1.5+", JavaEnvUtils.getJavaVersionNumber()>=15);
        try {
            buildRule.executeTarget("testTsaLocalhost");
            fail("Should have thrown exception - no TSA at localhost:0");
        } catch(BuildException ex) {
            assertEquals("jarsigner returned: 1", ex.getMessage());
        }
    }

    /**
     * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=50081">bug 50081</a>
     */
    @Test
    public void testSignUnnormalizedJar() throws Exception {
        buildRule.executeTarget("jar");
        File testJar = new File(buildRule.getProject().getProperty("test.jar"));
        File testJarParent = testJar.getParentFile();
        File f = new File(testJarParent,
                          "../" + testJarParent.getName() + "/"
                          + testJar.getName());
        assertFalse(testJar.equals(f));
        assertEquals(testJar.getCanonicalPath(), f.getCanonicalPath());
        SignJar s = new SignJar();
        s.setProject(buildRule.getProject());
        s.setJar(f);
        s.setAlias("testonly");
        s.setStorepass("apacheant");
        s.setKeystore("testkeystore");
        s.execute();
    }
}
