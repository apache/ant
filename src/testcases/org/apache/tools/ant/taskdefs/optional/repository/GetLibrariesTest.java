/*
 * Copyright  2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.repository;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.taskdefs.repository.AssertDownloaded;

/**
 * test the test libraries stuff.
 * skip all the tests if we are offline
 */
public class GetLibrariesTest extends BuildFileTest {
    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";


    public GetLibrariesTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject(TASKDEFS_DIR + "getlibraries.xml");
    }


    protected boolean offline() {
        return "true".equals(System.getProperty("offline"));
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testEmpty() {
        expectBuildException("testEmpty",GetLibraries.ERROR_NO_DEST_DIR);
    }

    public void testEmpty2() {
        expectBuildException("testEmpty2", GetLibraries.ERROR_NO_REPOSITORY);
    }

    public void testEmpty3() {
        expectBuildException("testEmpty3", GetLibraries.ERROR_NO_LIBRARIES);
    }

    public void testNoRepo() {
        expectBuildException("testNoRepo", GetLibraries.ERROR_NO_REPOSITORY);
    }

    public void testUnknownReference() {
        expectBuildException("testUnknownReference", "Reference unknown not found.");
    }

    /**
     * refs are  broken
     * */
    public void testFunctionalInline() {
        execIfOnline("testFunctionalInline");
    }
    
    public void testMavenInline() {
        String targetName = "testMavenInline";
        execIfOnline(targetName);
    }

    /**
     * exec a target, but only if we are online
     * @param targetName
     */
    private void execIfOnline(String targetName) {
        if (offline()) {
            return;
        }
        executeTarget(targetName);
    }

    public void testTwoRepositories() {
        expectBuildException("testTwoRepositories",
                GetLibraries.ERROR_ONE_REPOSITORY_ONLY);
    }

    public void testMavenInlineBadURL() {
        expectExceptionIfOnline("testMavenInlineBadURL",
                "testMavenInlineBadURL",
                GetLibraries.ERROR_INCOMPLETE_RETRIEVAL);
    }

    /**
     * exec a target if we are online; expect an eception
     * @param target
     * @param cause caue of the faule
     * @param message
     */
    private void expectExceptionIfOnline(String target, String cause,String message) {
        if (offline()) {
            return;
        }
        expectBuildExceptionContaining(target,cause,
                message);
    }

    public void testRenaming() {
        execIfOnline("testRenaming");

    }

    public void testOverwrite() {
        execIfOnline("testOverwrite");
    }

    public void testIf() {
        execIfOnline("testIf");
    }

    public void testUnless() {
        execIfOnline("testUnless");
    }

    public void testPathID() {
        execIfOnline("testPathID");
    }

    public void testSecurity() {
        execIfOnline("testSecurity");
    }

    public void testSchedule() {
        execIfOnline("testSchedule");
    }

    public void testForceEnabled() {
        execIfOnline("testForceEnabled");
    }

    public void testForceDisabled() {
        execIfOnline("testForceDisabled");
    }

    public void testAbsentFiles() {
        execIfOnline("testAbsentFiles");
    }

    public void testAbsentFilesTwice() {
        execIfOnline("testAbsentFilesTwice");
    }

    public void testNoUpdate() {
        expectExceptionIfOnline("testNoUpdate",
                "update disabled; dest file missing",
                GetLibraries.ERROR_INCOMPLETE_RETRIEVAL);
    }

    public void testTimestamp() {
        execIfOnline("testTimestamp");
    }

    public void testAssertDownloadedCountSet() {
        expectExceptionIfOnline("testAssertDownloadedCountSet",
                "No count in assertdownloaded",
                AssertDownloaded.ERROR_NO_COUNT);
    }

    public void testAssertDownloadedCountTested() {
        expectExceptionIfOnline("testAssertDownloadedCountTested",
                "Wrong count in assertdownloaded",
                AssertDownloaded.ERROR_DOWNLOAD_FAILURE);
    }
}
