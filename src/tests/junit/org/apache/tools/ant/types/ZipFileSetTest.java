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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * JUnit 3 testcases for org.apache.tools.ant.types.ZipFileSet.
 *
 * <p>This doesn't actually test much, mainly reference handling.
 *
 */

public class ZipFileSetTest extends AbstractFileSetTest {

    protected AbstractFileSet getInstance() {
        return new ZipFileSet();
    }

    @Test
    public final void testAttributes() {
        ZipFileSet f = (ZipFileSet)getInstance();
        //check that dir and src are incompatible
        f.setSrc(new File("example.zip"));
        try {
            f.setDir(new File("examples"));
            fail("can add dir to "
                    + f.getDataTypeName()
                    + " when a src is already present");
        } catch (BuildException be) {
            assertEquals("Cannot set both dir and src attributes",be.getMessage());
        }
        f = (ZipFileSet)getInstance();
        //check that dir and src are incompatible
        f.setDir(new File("examples"));
        try {
            f.setSrc(new File("example.zip"));
            fail("can add src to "
                    + f.getDataTypeName()
                    + " when a dir is already present");
        } catch (BuildException be) {
            assertEquals("Cannot set both dir and src attributes",be.getMessage());
        }
        //check that fullpath and prefix are incompatible
        f = (ZipFileSet)getInstance();
        f.setSrc(new File("example.zip"));
        f.setPrefix("/examples");
        try {
            f.setFullpath("/doc/manual/index.html");
            fail("Can add fullpath to "
                    + f.getDataTypeName()
                    + " when a prefix is already present");
        } catch (BuildException be) {
            assertEquals("Cannot set both fullpath and prefix attributes", be.getMessage());
        }
        f = (ZipFileSet)getInstance();
        f.setSrc(new File("example.zip"));
        f.setFullpath("/doc/manual/index.html");
        try {
            f.setPrefix("/examples");
            fail("Can add prefix to "
                    + f.getDataTypeName()
                    + " when a fullpath is already present");
        } catch (BuildException be) {
            assertEquals("Cannot set both fullpath and prefix attributes", be.getMessage());
        }
        // check that reference zipfilesets cannot have specific attributes
        f = (ZipFileSet)getInstance();
        f.setRefid(new Reference(getProject(), "test"));
        try {
            f.setSrc(new File("example.zip"));
            fail("Can add src to "
                    + f.getDataTypeName()
                    + " when a refid is already present");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one "
            + "attribute when using refid", be.getMessage());
        }
        // check that a reference zipfileset gets the same attributes as the original
        f = (ZipFileSet)getInstance();
        f.setSrc(new File("example.zip"));
        f.setPrefix("/examples");
        f.setFileMode("600");
        f.setDirMode("530");
        getProject().addReference("test",f);
        ZipFileSet zid=(ZipFileSet)getInstance();
        zid.setRefid(new Reference(getProject(), "test"));
        assertTrue("src attribute copied by copy constructor",zid.getSrc(getProject()).equals(f.getSrc(getProject())));
        assertTrue("prefix attribute copied by copy constructor",f.getPrefix(getProject()).equals(zid.getPrefix(getProject())));
        assertTrue("file mode attribute copied by copy constructor",f.getFileMode(getProject())==zid.getFileMode(getProject()));
        assertTrue("dir mode attribute copied by copy constructor",f.getDirMode(getProject())==zid.getDirMode(getProject()));
      }


}
