/*
 * Copyright  2002,2004 Apache Software Foundation
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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;

/**
 * JUnit 3 testcases for org.apache.tools.ant.types.DirSet.
 *
 * @author Stefan Bodewig
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
        ds.setRefid(new Reference("dummy"));
        try {
            ds.getDir(getProject());
            fail("DirSet created from FileSet reference");
        } catch (BuildException e) {
            assertEquals("dummy doesn\'t denote a DirSet", e.getMessage());
        }

        ds = (DirSet) getInstance();
        ds.setProject(getProject());
        getProject().addReference("dummy2", ds);
        fs.setRefid(new Reference("dummy2"));
        try {
            fs.getDir(getProject());
            fail("FileSet created from DirSet reference");
        } catch (BuildException e) {
            assertEquals("dummy2 doesn\'t denote a FileSet", e.getMessage());
        }
    }

}
