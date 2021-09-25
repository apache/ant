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

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNull;

public class TarTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/tar.xml");
        buildRule.executeTarget("setUp");
    }

    @Test
    public void test1() {
        try {
            buildRule.executeTarget("test1");
            fail("BuildException expected: required argument not specified");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void test2() {
        try {
            buildRule.executeTarget("test2");
            fail("BuildException expected: required argument not specified");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void test3() {
        try {
            buildRule.executeTarget("test3");
            fail("BuildException expected: required argument not specified");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void test4() {
        try {
            buildRule.executeTarget("test4");
            fail("BuildException expected: tar cannot include itself");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void test5() {
        buildRule.executeTarget("test5");
        File f
            = new File(buildRule.getProject().getProperty("output"), "test5.tar");

        if (!f.exists()) {
            fail("Tarring a directory failed");
        }
    }

    @Test
    public void test6() {
        try {
            buildRule.executeTarget("test6");
            fail("BuildException expected: Invalid value specified for longfile attribute.");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void test7() {
        test7("test7");
    }

    @Test
    public void test7UsingPlainFileSet() {
        test7("test7UsingPlainFileSet");
    }

    @Test
    public void test7UsingFileList() {
        test7("test7UsingFileList");
    }

    private void test7(String target) {
        buildRule.executeTarget(target);
        File f1
            = new File(buildRule.getProject().getProperty("output"), "untar/test7-prefix");

        if (!(f1.exists() && f1.isDirectory())) {
            fail("The prefix attribute is not working properly.");
        }

        File f2
            = new File(buildRule.getProject().getProperty("output"), "untar/test7dir");

        if (!(f2.exists() && f2.isDirectory())) {
            fail("The prefix attribute is not working properly.");
        }
    }

    @Test
    public void test8() {
        test8("test8");
    }

    @Test
    public void test8UsingZipFileset() {
        test8("test8UsingZipFileset");
    }

    @Test
    public void test8UsingZipFilesetSrc() {
        test8("test8UsingZipFilesetSrc");
    }

    @Test
    public void test8UsingTarFilesetSrc() {
        test8("test8UsingTarFilesetSrc");
    }

    @Test
    public void test8UsingZipEntry() {
        test8("test8UsingZipEntry");
    }

    private void test8(String target) {
        buildRule.executeTarget(target);
        File f1
            = new File(buildRule.getProject().getProperty("output"), "untar/test8.xml");
        if (! f1.exists()) {
            fail("The fullpath attribute or the preserveLeadingSlashes attribute does not work properly");
        }
    }

    @Test
    public void test9() {
        try {
            buildRule.executeTarget("test9");
            fail("BuildException expected: Invalid value specified for compression attribute.");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void test10() {
        buildRule.executeTarget("test10");
        File f1
            = new File(buildRule.getProject().getProperty("output"), "untar/test10.xml");
        if (! f1.exists()) {
            fail("The fullpath attribute or the preserveLeadingSlashes attribute does not work properly");
        }
    }

    @Test
    public void test11() {
        buildRule.executeTarget("test11");
        File f1
            = new File(buildRule.getProject().getProperty("output"), "untar/test11.xml");
        if (! f1.exists()) {
            fail("The fullpath attribute or the preserveLeadingSlashes attribute does not work properly");
        }
    }

    @Test
    public void testGZipResource() throws IOException {
        buildRule.executeTarget("testGZipResource");
        assertEquals(FileUtilities.getFileContents(buildRule.getProject().resolveFile("../asf-logo.gif")),
                FileUtilities.getFileContents(new File(buildRule.getProject().getProperty("output"), "untar/asf-logo.gif.gz")));
    }

    @Test
    public void testTarFilesetWithSymlinks() throws IOException {
        buildRule.executeTarget("testTarFilesetWithSymlinks");
        final File f = new File(buildRule.getProject().getProperty("output"), "result.tar");
        final TarInputStream tis = new TarInputStream(new FileInputStream(f));
        try {
            final TarEntry e1 = tis.getNextEntry();
            assertEquals("pre/dir/file", e1.getName());
            assertEquals("", e1.getLinkName());
            assertEquals(48, e1.getLinkFlag());

            final TarEntry e2 = tis.getNextEntry();
            assertEquals("pre/sub/file", e2.getName());
            assertEquals("../dir/file", e2.getLinkName());
            assertEquals(50, e2.getLinkFlag());

            assertNull(tis.getNextEntry());
        }
        finally {
            tis.close();
        }
    }
}
