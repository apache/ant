/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import org.apache.tools.ant.BuildFileTest;

/**
 * @author Erik Meade <emeade@geekfarm.org>
 */
public class JarTest extends BuildFileTest {

    private static String tempJar = "tmp.jar";
    private static String tempDir = "jartmp/";
    private Reader r1, r2;

    public JarTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/jar.xml");
    }

    public void tearDown() {
        if (r1 != null) {
            try {
                r1.close();
            } catch (IOException e) {
            }
        }
        if (r2 != null) {
            try {
                r2.close();
            } catch (IOException e) {
            }
        }
        
        executeTarget("cleanup");
    }

    public void test1() {
        expectBuildException("test1", "required argument not specified");
    }

    public void test2() {
        expectBuildException("test2", "manifest file does not exist");
    }

    public void test3() {
        expectBuildException("test3", "Unrecognized whenempty attribute: format C: /y");
    }

    public void test4() {
        executeTarget("test4");
        File jarFile = new File(getProjectDir(), tempJar);
        assertTrue(jarFile.exists());
    }

    public void testNoRecreateWithoutUpdate() {
        testNoRecreate("test4");
    }

    public void testNoRecreateWithUpdate() {
        testNoRecreate("testNoRecreateWithUpdate");
    }

    private void testNoRecreate(String secondTarget) {
        executeTarget("test4");
        File jarFile = new File(getProjectDir(), tempJar);
        long jarModifiedDate = jarFile.lastModified();
        try {
            Thread.currentThread().sleep(2500);
        } catch (InterruptedException e) {
        } // end of try-catch
        executeTarget(secondTarget);
        assertEquals("jar has not been recreated in " + secondTarget,
                     jarModifiedDate, jarFile.lastModified());
    }

    public void testRecreateWithoutUpdateAdditionalFiles() {
        testRecreate("test4", "testRecreateWithoutUpdateAdditionalFiles");
    }

    public void testRecreateWithUpdateAdditionalFiles() {
        testRecreate("test4", "testRecreateWithUpdateAdditionalFiles");
    }

    public void testRecreateWithoutUpdateNewerFile() {
        testRecreate("testRecreateNewerFileSetup",
                     "testRecreateWithoutUpdateNewerFile");
    }

    public void testRecreateWithUpdateNewerFile() {
        testRecreate("testRecreateNewerFileSetup",
                     "testRecreateWithUpdateNewerFile");
    }

    private void testRecreate(String firstTarget, String secondTarget) {
        executeTarget(firstTarget);
        try {
            Thread.currentThread().sleep(2500);
        } catch (InterruptedException e) {
        } // end of try-catch
        File jarFile = new File(getProjectDir(), tempJar);
        long jarModifiedDate = jarFile.lastModified();
        executeTarget(secondTarget);
        jarFile = new File(getProjectDir(), tempJar);
        assertTrue("jar has been recreated in " + secondTarget,
                   jarModifiedDate < jarFile.lastModified());
    }

    public void testManifestStaysIntact() 
        throws IOException, ManifestException {
        executeTarget("testManifestStaysIntact");

        r1 = new FileReader(getProject()
                            .resolveFile(tempDir + "manifest"));
        r2 = new FileReader(getProject()
                            .resolveFile(tempDir + "META-INF/MANIFEST.MF"));
        Manifest mf1 = new Manifest(r1);
        Manifest mf2 = new Manifest(r2);
        assertEquals(mf1, mf2);
    }

    public void testNoRecreateBasedirExcludesWithUpdate() {
        testNoRecreate("testNoRecreateBasedirExcludesWithUpdate");
    }

    public void testNoRecreateBasedirExcludesWithoutUpdate() {
        testNoRecreate("testNoRecreateBasedirExcludesWithoutUpdate");
    }

    public void testNoRecreateZipfilesetExcludesWithUpdate() {
        testNoRecreate("testNoRecreateZipfilesetExcludesWithUpdate");
    }

    public void testNoRecreateZipfilesetExcludesWithoutUpdate() {
        testNoRecreate("testNoRecreateZipfilesetExcludesWithoutUpdate");
    }

    public void testRecreateZipfilesetWithoutUpdateAdditionalFiles() {
        testRecreate("test4",
                     "testRecreateZipfilesetWithoutUpdateAdditionalFiles");
    }

    public void testRecreateZipfilesetWithUpdateAdditionalFiles() {
        testRecreate("test4",
                     "testRecreateZipfilesetWithUpdateAdditionalFiles");
    }

    public void testRecreateZipfilesetWithoutUpdateNewerFile() {
        testRecreate("testRecreateNewerFileSetup",
                     "testRecreateZipfilesetWithoutUpdateNewerFile");
    }

    public void testRecreateZipfilesetWithUpdateNewerFile() {
        testRecreate("testRecreateNewerFileSetup",
                     "testRecreateZipfilesetWithUpdateNewerFile");
    }

    public void testCreateWithEmptyFileset() {
        executeTarget("testCreateWithEmptyFilesetSetUp");
        executeTarget("testCreateWithEmptyFileset");
        executeTarget("testCreateWithEmptyFileset");
    }

    public void testUpdateIfOnlyManifestHasChanged() {
        executeTarget("testUpdateIfOnlyManifestHasChanged");
        File jarXml = getProject().resolveFile(tempDir + "jar.xml");
        assertTrue(jarXml.exists());
    }
}
