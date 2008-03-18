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
     * @return true iff the system property "offline" is "true"
     */
    private boolean isOffline() {
        return Boolean.getBoolean("offline");
    }
    public void testBasicSigning() {
        executeTarget("basic");
    }

    public void testSigFile() {
        executeTarget("sigfile");
    }

    public void testMaxMemory() {
        executeTarget("maxmemory");
    }

    public void testURLKeystoreFile() {
        executeTarget("urlKeystoreFile");
    }

    public void testURLKeystoreHTTP() {
        if(!isOffline()) {
            executeTarget("urlKeystoreHTTP");
        }
    }

    public void testPreserveLastModified() {
        executeTarget("preserveLastModified");
    }

    public void testFileset() {
        executeTarget("testFileset");
    }

    public void testFilesetAndJar() {
        executeTarget("testFilesetAndJar");
    }

    public void testFilesetAndSignedJar() {
        expectBuildExceptionContaining("testFilesetAndSignedJar",
                "incompatible attributes",
                SignJar.ERROR_SIGNEDJAR_AND_PATHS);
    }

    public void testPath() {
        executeTarget("testPath");
    }

    public void testPathAndJar() {
        executeTarget("testPathAndJar");
    }

    public void testPathAndSignedJar() {
        expectBuildExceptionContaining("testPathAndSignedJar",
                "incompatible attributes",
                SignJar.ERROR_SIGNEDJAR_AND_PATHS);
    }

    public void testSignedJar() {
        executeTarget("testSignedJar");
    }

    public void testDestDir() {
        executeTarget("testDestDir");
    }

    public void testDestDirAndSignedJar() {
        expectBuildExceptionContaining("testFilesetAndSignedJar",
                "incompatible attributes",
                SignJar.ERROR_SIGNEDJAR_AND_PATHS);
    }

    public void testDestDirAndSignedJar2() {
        expectBuildExceptionContaining("testPathAndSignedJar",
                "incompatible attributes",
                SignJar.ERROR_SIGNEDJAR_AND_PATHS);
    }

    public void testDestDirFileset() {
        executeTarget("testDestDirFileset");
    }

    public void testMapperFileset() {
        executeTarget("testMapperFileset");
    }

    public void testDestDirPath() {
        executeTarget("testDestDirPath");
    }

    public void testMapperPath() {
        executeTarget("testMapperPath");
    }

    public void testMapperNoDest() {
        expectBuildExceptionContaining("testMapperNoDest",
                "two mappers",
                SignJar.ERROR_MAPPER_WITHOUT_DEST);
    }

    public void testTwoMappers() {
        expectBuildExceptionContaining("testTwoMappers",
                "two mappers",
                SignJar.ERROR_TOO_MANY_MAPPERS);
    }

    public void testNoAlias() {
        expectBuildExceptionContaining("testNoAlias",
                "no alias",
                SignJar.ERROR_NO_ALIAS);
    }

    public void testNoFiles() {
        expectBuildExceptionContaining("testNoFiles",
                "no files",
                SignJar.ERROR_NO_SOURCE);
    }

    public void testNoStorePass() {
        expectBuildExceptionContaining("testNoStorePass",
                "no password",
                SignJar.ERROR_NO_STOREPASS);
    }

    public void testTsaLocalhost() {
        //only test on java1.5+
        if(JavaEnvUtils.getJavaVersionNumber()>=15) {
            expectBuildException("testTsaLocalhost",
                "no TSA at localhost:0");
            assertLogContaining("java.net.ConnectException");
        }
    }

    public void testSysProperty() {
        executeTarget("testSysProperty");
    }

    public void testVerifyJar() {
        executeTarget("testVerifyJar");
    }

    public void testVerifyNoArgs() {
        expectBuildExceptionContaining("testVerifyNoArgs",
                "no args",
                AbstractJarSignerTask.ERROR_NO_SOURCE);
    }

    public void testVerifyJarUnsigned() {
        expectBuildExceptionContaining("testVerifyJarUnsigned",
                "unsigned JAR file",
                VerifyJar.ERROR_NO_VERIFY);
    }

    public void NotestVerifyJarNotInKeystore() {
        expectBuildExceptionContaining("testVerifyJarNotInKeystore",
                "signature not in keystore",
                VerifyJar.ERROR_NO_VERIFY);
    }

    public void testVerifyFileset() {
        executeTarget("testVerifyFileset");
    }

    public void testVerifyPath() {
        executeTarget("testVerifyPath");
    }

}
