/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001,2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
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
