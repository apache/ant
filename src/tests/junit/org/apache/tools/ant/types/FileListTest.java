/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Some tests for filelist.
 */

public class FileListTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    private FileList f;

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/filelist.xml");
        f = new FileList();
    }

    /**
     * Can add reference to FileList with directory attribute set.
     */
    @Test
    public void testEmptyElementSetDirThenRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        f.setDir(buildRule.getProject().resolveFile("."));
        f.setRefid(new Reference(buildRule.getProject(), "dummyref"));
    }

    @Test
    public void testEmptyElementSetFilesThenRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        f.setFiles("foo.xml,c/d/bar.xml");
        f.setRefid(new Reference(buildRule.getProject(), "dummyref"));
    }

    @Test
    public void testEmptyElementSetRefidThenFiles() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        f.setRefid(new Reference(buildRule.getProject(), "dummyref"));
        f.setFiles("a/b/foo.java");
    }

    @Test
    public void testEmptyElementSetRefidThenDir() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        f.setRefid(new Reference(buildRule.getProject(), "dummyref"));
        f.setDir(buildRule.getProject().resolveFile("."));
    }

    @Test
    public void testCircularReferenceCheckDir() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        buildRule.getProject().addReference("dummy", f);
        f.setRefid(new Reference(buildRule.getProject(), "dummy"));
        f.getDir(buildRule.getProject());
    }

    @Test
    public void testCircularReferenceCheckFiles() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        buildRule.getProject().addReference("dummy", f);
        f.setRefid(new Reference(buildRule.getProject(), "dummy"));
        f.getFiles(buildRule.getProject());
    }

    @Test
    public void testLoopReferenceCheckDir() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        // dummy --> dummyA --> dummyB --> dummy
        buildRule.getProject().addReference("dummy", f);
        f.setRefid(new Reference(buildRule.getProject(), "dummyA"));
        FileList fa = new FileList();
        buildRule.getProject().addReference("dummyA", fa);
        fa.setRefid(new Reference(buildRule.getProject(), "dummyB"));
        FileList fb = new FileList();
        buildRule.getProject().addReference("dummyB", fb);
        fb.setRefid(new Reference(buildRule.getProject(), "dummy"));
        f.getDir(buildRule.getProject());
    }

    @Test
    public void testLoopReferenceCheckFiles() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        // dummy --> dummyA --> dummyB --> dummy
        buildRule.getProject().addReference("dummy", f);
        f.setRefid(new Reference(buildRule.getProject(), "dummyA"));
        FileList fa = new FileList();
        buildRule.getProject().addReference("dummyA", fa);
        fa.setRefid(new Reference(buildRule.getProject(), "dummyB"));
        FileList fb = new FileList();
        buildRule.getProject().addReference("dummyB", fb);
        fb.setRefid(new Reference(buildRule.getProject(), "dummy"));
        f.getFiles(buildRule.getProject());
    }

    @Test
    public void testLoopReferenceCheck() {
        // dummy --> dummyA --> dummyB
        // (which has the Project's basedir as root).
        buildRule.getProject().addReference("dummy", f);
        f.setRefid(new Reference(buildRule.getProject(), "dummyA"));
        FileList fa = new FileList();
        buildRule.getProject().addReference("dummyA", fa);
        fa.setRefid(new Reference(buildRule.getProject(), "dummyB"));
        FileList fb = new FileList();
        buildRule.getProject().addReference("dummyB", fb);
        fb.setDir(buildRule.getProject().resolveFile("."));
        File dir = f.getDir(buildRule.getProject());
        assertEquals("Dir is basedir", dir, buildRule.getProject().getBaseDir());
    }

    @Test
    public void testSimple() {
        buildRule.executeTarget("simple");
        assertEquals("/abc/a", buildRule.getLog());
    }

    @Test
    public void testDouble() {
        buildRule.executeTarget("double");
        assertEquals("/abc/a:/abc/b", buildRule.getLog());
    }

    @Test
    public void testNested() {
        buildRule.executeTarget("nested");
        assertEquals("/abc/a:/abc/b", buildRule.getLog());
    }
}
