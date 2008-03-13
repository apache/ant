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
import java.io.FileOutputStream;
import org.apache.tools.ant.BuildException;

/**
 * JUnit 3 testcases for org.apache.tools.ant.types.DirSet.
 *
 */
public class DirSetTest extends AbstractFileSetTest {

    public DirSetTest(String name) {
        super(name);
    }

    protected AbstractFileSet getInstance() {
        return new DirSet();
    }

    public void testFileSetIsNoDirSet() {
        DirSet ds = (DirSet) getInstance();
        ds.setProject(getProject());
        FileSet fs = new FileSet();
        fs.setProject(getProject());
        getProject().addReference("dummy", fs);
        ds.setRefid(new Reference(getProject(), "dummy"));
        try {
            ds.getDir(getProject());
            fail("DirSet created from FileSet reference");
        } catch (BuildException e) {
            assertEquals("dummy doesn\'t denote a DirSet", e.getMessage());
        }

        ds = (DirSet) getInstance();
        ds.setProject(getProject());
        getProject().addReference("dummy2", ds);
        fs.setRefid(new Reference(getProject(), "dummy2"));
        try {
            fs.getDir(getProject());
            fail("FileSet created from DirSet reference");
        } catch (BuildException e) {
            assertEquals("dummy2 doesn\'t denote a FileSet", e.getMessage());
        }
    }

    public void testToString() throws Exception {
        File tmp = File.createTempFile("DirSetTest", "");
        tmp.delete();
        File a = new File(tmp, "a");
        a.mkdirs();
        File b = new File(tmp, "b");
        File bc = new File(b, "c");
        bc.mkdirs();
        new FileOutputStream(new File(a, "x")).close();
        new FileOutputStream(new File(b, "x")).close();
        new FileOutputStream(new File(bc, "x")).close();
        DirSet ds = new DirSet();
        ds.setProject(getProject());
        ds.setDir(tmp);
        ds.setIncludes("b/");
        assertEquals("b;b" + File.separator + "c", ds.toString());
    }

}
