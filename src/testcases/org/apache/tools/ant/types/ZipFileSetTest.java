/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import java.io.File;

/**
 * JUnit 3 testcases for org.apache.tools.ant.types.ZipFileSet.
 *
 * <p>This doesn't actually test much, mainly reference handling.
 *
 * @author Antoine Levy-Lambert
 */

public class ZipFileSetTest extends AbstractFileSetTest {

    public ZipFileSetTest(String name) {
        super(name);
    }

    protected AbstractFileSet getInstance() {
        return new ZipFileSet();
    }
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
        f.setRefid(new Reference("test"));
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
        zid.setRefid(new Reference("test"));
        assertTrue("src attribute copied by copy constructor",zid.getSrc(getProject()).equals(f.getSrc(getProject())));
        assertTrue("prefix attribute copied by copy constructor",f.getPrefix(getProject()).equals(zid.getPrefix(getProject())));
        assertTrue("file mode attribute copied by copy constructor",f.getFileMode(getProject())==zid.getFileMode(getProject()));
        assertTrue("dir mode attribute copied by copy constructor",f.getDirMode(getProject())==zid.getDirMode(getProject()));
      }


}
