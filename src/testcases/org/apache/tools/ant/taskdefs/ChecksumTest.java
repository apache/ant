/* 
 * Copyright  2001,2003-2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

import java.io.IOException;
import java.io.File;

/**
 * @author Stefan Bodewig
 * @author Aslak Hellesoy
 * @version $Revision$
 */
public class ChecksumTest extends BuildFileTest {

    public ChecksumTest(String name) {
        super(name);
    }

    public void setUp() { 
        configureProject("src/etc/testcases/taskdefs/checksum.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testCreateMd5() throws IOException {
        FileUtils fileUtils = FileUtils.newFileUtils();
        executeTarget("createMd5");
        assertTrue(fileUtils.contentEquals(project.resolveFile("expected/asf-logo.gif.md5"),
                                           project.resolveFile("../asf-logo.gif.MD5")));
    }

    public void testSetProperty() {
        executeTarget("setProperty");
        assertEquals("0541d3df42520911f268abc730f3afe0",
                     project.getProperty("logo.MD5"));
        assertTrue(!project.resolveFile("../asf-logo.gif.MD5").exists());
    }

    public void testVerifyTotal() {
        executeTarget("verifyTotal");
        assertEquals("ef8f1477fcc9bf93832c1a74f629c626",
                     project.getProperty("total"));
    }

    public void testVerifyChecksumdir() {
        executeTarget("verifyChecksumdir");
        assertEquals("ef8f1477fcc9bf93832c1a74f629c626",
                     project.getProperty("total"));
        File shouldExist = project.resolveFile("checksum/checksums/foo/zap/Eenie.MD5");
        File shouldNotExist = project.resolveFile("checksum/foo/zap/Eenie.MD5");
        assertTrue( "Checksums should be written to " + shouldExist.getAbsolutePath(), shouldExist.exists());
        assertTrue( "Checksums should not be written to " + shouldNotExist.getAbsolutePath(), !shouldNotExist.exists());
    }

    public void testVerifyAsTask() {
        testVerify("verifyAsTask");
        assertNotNull(project.getProperty("no.logo.MD5"));
        assertEquals("false", project.getProperty("no.logo.MD5"));
    }

    public void testVerifyAsCondition() {
        testVerify("verifyAsCondition");
        assertNull(project.getProperty("no.logo.MD5"));
    }

    public void testVerifyFromProperty() {
        assertNull(getProject().getProperty("verify"));
        expectPropertySet("verifyFromProperty", "verify", "true");
    }

    public void testVerifyChecksumdirNoTotal() {
        executeTarget("verifyChecksumdirNoTotal");
    }
    private void testVerify(String target) {
        assertNull(project.getProperty("logo.MD5"));
        assertNull(project.getProperty("no.logo.MD5"));
        executeTarget(target);
        assertNotNull(project.getProperty("logo.MD5"));
        assertEquals("true", project.getProperty("logo.MD5"));
    }

}
