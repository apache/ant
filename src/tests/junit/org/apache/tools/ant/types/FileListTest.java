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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Some tests for filelist.
 */

public class FileListTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();
    
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/filelist.xml");
    }
    
    @Test
    public void testEmptyElementIfIsReference() {
        FileList f = new FileList();
        f.setDir(buildRule.getProject().resolveFile("."));
        try {
            f.setRefid(new Reference(buildRule.getProject(), "dummyref"));
            fail("Can add reference to FileList with directory attribute set.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        f = new FileList();
        f.setFiles("foo.xml,c/d/bar.xml");
        try {
            f.setRefid(new Reference(buildRule.getProject(), "dummyref"));
            fail("Can add reference to FileList with file attribute set.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        f = new FileList();
        f.setRefid(new Reference(buildRule.getProject(), "dummyref"));
        try {
            f.setFiles("a/b/foo.java");
            fail("Can set files in FileList that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
        try {
            f.setDir(buildRule.getProject().resolveFile("."));
            fail("Can set dir in FileList that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
    }

    @Test
    public void testCircularReferenceCheck() {
        FileList f = new FileList();
        buildRule.getProject().addReference("dummy", f);
        f.setRefid(new Reference(buildRule.getProject(), "dummy"));
        try {
            f.getDir(buildRule.getProject());
            fail("Can make FileList a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }
        try {
            f.getFiles(buildRule.getProject());
            fail("Can make FileList a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3 --> dummy1
        FileList f1 = new FileList();
        buildRule.getProject().addReference("dummy1", f1);
        f1.setRefid(new Reference(buildRule.getProject(), "dummy2"));
        FileList f2 = new FileList();
        buildRule.getProject().addReference("dummy2", f2);
        f2.setRefid(new Reference(buildRule.getProject(), "dummy3"));
        FileList f3 = new FileList();
        buildRule.getProject().addReference("dummy3", f3);
        f3.setRefid(new Reference(buildRule.getProject(), "dummy1"));
        try {
            f1.getDir(buildRule.getProject());
            fail("Can make circular reference.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }
        try {
            f1.getFiles(buildRule.getProject());
            fail("Can make circular reference.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3
        // (which has the Project's basedir as root).
        f1 = new FileList();
        buildRule.getProject().addReference("dummy1", f1);
        f1.setRefid(new Reference(buildRule.getProject(), "dummy2"));
        f2 = new FileList();
        buildRule.getProject().addReference("dummy2", f2);
        f2.setRefid(new Reference(buildRule.getProject(), "dummy3"));
        f3 = new FileList();
        buildRule.getProject().addReference("dummy3", f3);
        f3.setDir(buildRule.getProject().resolveFile("."));
        File dir = f1.getDir(buildRule.getProject());
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
