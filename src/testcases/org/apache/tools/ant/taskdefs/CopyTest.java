/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import java.io.File;
import java.io.IOException;

/**
 * Tests FileSet using the Copy task.
 *
 * @author David Rees <dave@ubiqsoft.com>
 */
public class CopyTest extends BuildFileTest {

    public CopyTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/copy.xml");
    }

    public void test1() {
        executeTarget("test1");
        File f = new File(getProjectDir(), "copytest1.tmp");
        if ( !f.exists()) {
            fail("Copy failed");
        }
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void test2() {
        executeTarget("test2");
        File f = new File(getProjectDir(), "copytest1dir/copy.xml");
        if ( !f.exists()) {
            fail("Copy failed");
        }
    }

    public void test3() {
        executeTarget("test3");
        File file3  = new File(getProjectDir(), "copytest3.tmp");
        assertTrue(file3.exists());
        File file3a = new File(getProjectDir(), "copytest3a.tmp");
        assertTrue(file3a.exists());
        File file3b = new File(getProjectDir(), "copytest3b.tmp");
        assertTrue(file3b.exists());
        File file3c = new File(getProjectDir(), "copytest3c.tmp");
        assertTrue(file3c.exists());

        //file length checks rely on touch generating a zero byte file
        if(file3.length()==0) {
            fail("could not overwrite an existing, older file");
        }
        if(file3c.length()!=0) {
            fail("could not force overwrite an existing, newer file");
        }
        if(file3b.length()==0) {
            fail("unexpectedly overwrote an existing, newer file");
        }

        //file time checks for java1.2+
        if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            assertTrue(file3a.lastModified()==file3.lastModified());
            assertTrue(file3c.lastModified()<file3a.lastModified());
        }

    }

    public void testFilterSet() throws IOException {
        executeTarget("testFilterSet");
        FileUtils fileUtils = FileUtils.newFileUtils();
        File tmp  = new File(getProjectDir(), "copy.filterset.tmp");
        File check  = new File(getProjectDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertTrue(fileUtils.contentEquals(tmp, check));
    }

    public void testFilterChain() throws IOException {
        executeTarget("testFilterChain");
        FileUtils fileUtils = FileUtils.newFileUtils();
        File tmp  = new File(getProjectDir(), "copy.filterchain.tmp");
        File check  = new File(getProjectDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertTrue(fileUtils.contentEquals(tmp, check));
    }

    public void testSingleFileFileset() {
        executeTarget("test_single_file_fileset");
        File file  = new File(getProjectDir(),
                                        "copytest_single_file_fileset.tmp");
        assertTrue(file.exists());
    }
}
