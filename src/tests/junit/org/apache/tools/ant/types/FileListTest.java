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

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;

/**
 * Some tests for filelist.
 */

public class FileListTest extends BuildFileTest {

    public FileListTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/types/filelist.xml");
    }
    
    public void testEmptyElementIfIsReference() {
        FileList f = new FileList();
        f.setDir(project.resolveFile("."));
        try {
            f.setRefid(new Reference("dummyref"));
            fail("Can add reference to FileList with directory attribute set.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        f = new FileList();
        f.setFiles("foo.xml,c/d/bar.xml");
        try {
            f.setRefid(new Reference("dummyref"));
            fail("Can add reference to FileList with file attribute set.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        f = new FileList();
        f.setRefid(new Reference("dummyref"));
        try {
            f.setFiles("a/b/foo.java");
            fail("Can set files in FileList that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
        try {
            f.setDir(project.resolveFile("."));
            fail("Can set dir in FileList that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
    }

    public void testCircularReferenceCheck() {
        FileList f = new FileList();
        project.addReference("dummy", f);
        f.setRefid(new Reference("dummy"));
        try {
            f.getDir(project);
            fail("Can make FileList a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }
        try {
            f.getFiles(project);
            fail("Can make FileList a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3 --> dummy1
        FileList f1 = new FileList();
        project.addReference("dummy1", f1);
        f1.setRefid(new Reference("dummy2"));
        FileList f2 = new FileList();
        project.addReference("dummy2", f2);
        f2.setRefid(new Reference("dummy3"));
        FileList f3 = new FileList();
        project.addReference("dummy3", f3);
        f3.setRefid(new Reference("dummy1"));
        try {
            f1.getDir(project);
            fail("Can make circular reference.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }
        try {
            f1.getFiles(project);
            fail("Can make circular reference.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3
        // (which has the Project's basedir as root).
        f1 = new FileList();
        project.addReference("dummy1", f1);
        f1.setRefid(new Reference("dummy2"));
        f2 = new FileList();
        project.addReference("dummy2", f2);
        f2.setRefid(new Reference("dummy3"));
        f3 = new FileList();
        project.addReference("dummy3", f3);
        f3.setDir(project.resolveFile("."));
        File dir = f1.getDir(project);
        assertEquals("Dir is basedir", dir, project.getBaseDir());
    }
    
    public void testSimple() {
        expectLog("simple", "/abc/a");
    }

    public void testDouble() {
        expectLog("double", "/abc/a:/abc/b");
    }

    public void testNested() {
        expectLog("nested", "/abc/a:/abc/b");
    }
}
