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
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Testcase for the Signjar task
 *
 */
public class SignJarTest extends BuildFileTest {

    public static final String EXPANDED_MANIFEST
        = "src/etc/testcases/taskdefs/manifests/META-INF/MANIFEST.MF";


    public SignJarTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/signjar.xml");
    }

    public void tearDown() {
        executeTarget("clean");
    }

    /**
     * check for being offline
     * @return true if the system property "offline" is "true"
     */
    private boolean isOffline() {
        return Boolean.getBoolean("offline");
    }

    public void testSigFile() {
        executeTarget("sigfile");
        SignJarChild sj = new SignJarChild();
        sj.setAlias("testonly");
        sj.setKeystore("testkeystore");
        sj.setStorepass("apacheant");
        File jar = new File(getProject().getProperty("test.jar"));
        sj.setJar(jar);
        assertFalse("mustn't find signature without sigfile attribute",
                    sj.isSigned());
        sj.setSigfile("TEST");
        assertTrue("must find signature with sigfile attribute",
                   sj.isSigned());
    }

    public void testInvalidChars() {
        executeTarget("invalidchars");
        SignJarChild sj = new SignJarChild();
        sj.setAlias("test@nly");
        sj.setKeystore("testkeystore");
        sj.setStorepass("apacheant");
        File jar = new File(getProject().getProperty("test.jar"));
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

    public void testURLKeystoreFile() {
        executeTarget("urlKeystoreFile");
    }

    public void testURLKeystoreHTTP() {
        if(!isOffline()) {
            executeTarget("urlKeystoreHTTP");
        }
    }

    public void testTsaLocalhost() {
        //only test on java1.5+
        if(JavaEnvUtils.getJavaVersionNumber()>=15) {
            expectBuildException("testTsaLocalhost",
                "no TSA at localhost:0");
            assertLogContaining("java.net.ConnectException");
        }
    }

    /**
     * @see https://issues.apache.org/bugzilla/show_bug.cgi?id=50081
     */
    public void testSignUnnormalizedJar() throws Exception {
        executeTarget("jar");
        File testJar = new File(getProject().getProperty("test.jar"));
        File testJarParent = testJar.getParentFile();
        File f = new File(testJarParent,
                          "../" + testJarParent.getName() + "/"
                          + testJar.getName());
        assertFalse(testJar.equals(f));
        assertEquals(testJar.getCanonicalPath(), f.getCanonicalPath());
        SignJar s = new SignJar();
        s.setProject(getProject());
        s.setJar(f);
        s.setAlias("testonly");
        s.setStorepass("apacheant");
        s.setKeystore("testkeystore");
        s.execute();
    }
}
