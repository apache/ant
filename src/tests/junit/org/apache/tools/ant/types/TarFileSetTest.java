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
 * <p>This doesn't actually test much, mainly reference handling.</p>
 */

package org.apache.tools.ant.types;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * JUnit 4 testcases for org.apache.tools.ant.types.TarFileSet.
 *
 * <p>This doesn't actually test much, mainly reference handling.</p>
 */
public class TarFileSetTest extends AbstractFileSetTest {

    private TarFileSet tfs;

    protected AbstractFileSet getInstance() {
        return new TarFileSet();
    }

    @Before
    public void setUp() {
        super.setUp();
        tfs = (TarFileSet) getInstance();
    }

    /**
     * check that dir and src are incompatible
     */
    @Test
    public final void testSrcDirAttributes() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot set both dir and src attributes");
        tfs.setSrc(new File("example.tar"));
        tfs.setDir(new File("examples"));
    }

    /**
     * check that dir and src are incompatible
     */
    @Test
    public final void testDirSrcAttributes() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot set both dir and src attributes");
        tfs.setDir(new File("examples"));
        tfs.setSrc(new File("example.tar"));
    }

    /**
     * check that fullpath and prefix are incompatible
     */
    @Test
    public final void testPrefixFullpathAttributes() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot set both fullpath and prefix attributes");
        tfs.setSrc(new File("example.tar"));
        tfs.setPrefix("/examples");
        tfs.setFullpath("/doc/manual/index.html");
    }

    @Test
    public final void testFullpathPrefixAttributes() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot set both fullpath and prefix attributes");
        tfs.setSrc(new File("example.tar"));
        tfs.setFullpath("/doc/manual/index.html");
        tfs.setPrefix("/examples");
    }

    /**
     * check that reference tarfilesets cannot have specific attributes
     */
    @Test
    public final void testRefidSrcAttributes() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        tfs.setRefid(new Reference(getProject(), "test"));
        tfs.setSrc(new File("example.tar"));
    }

    /**
     * check that a reference tarfileset gets the same attributes as the original
     */
    @Test
    public final void testAttributes() {
        tfs.setSrc(new File("example.tar"));
        tfs.setPrefix("/examples");
        tfs.setFileMode("600");
        tfs.setDirMode("530");
        getProject().addReference("test", tfs);
        TarFileSet zid = (TarFileSet) getInstance();
        zid.setRefid(new Reference(getProject(), "test"));
        assertEquals("src attribute copied by copy constructor",
                tfs.getSrc(getProject()), zid.getSrc(getProject()));
        assertEquals("prefix attribute copied by copy constructor",
                tfs.getPrefix(getProject()), zid.getPrefix(getProject()));
        assertEquals("file mode attribute copied by copy constructor",
                tfs.getFileMode(getProject()), zid.getFileMode(getProject()));
        assertEquals("dir mode attribute copied by copy constructor",
                tfs.getDirMode(getProject()), zid.getDirMode(getProject()));
      }

}
