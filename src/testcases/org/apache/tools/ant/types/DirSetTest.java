/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
 * JUnit 3 testcases for org.apache.tools.ant.types.DirSet.
 *
 * <p>This doesn't actually test much, mainly reference handling.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */

public class DirSetTest extends TestCase {

    private Project project;

    public DirSetTest(String name) {
        super(name);
    }

    public void setUp() {
        project = new Project();
        project.setBasedir(".");
    }

    public void testEmptyElementIfIsReference() {
        DirSet f = new DirSet();
        f.setIncludes("**/*.java");
        try {
            f.setRefid(new Reference("dummyref"));
            fail("Can add reference to DirSet with elements from setIncludes");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        f = new DirSet();
        f.createPatternSet();
        try {
            f.setRefid(new Reference("dummyref"));
            fail("Can add reference to DirSet with nested patternset element.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }

        f = new DirSet();
        f.createInclude();
        try {
            f.setRefid(new Reference("dummyref"));
            fail("Can add reference to DirSet with nested include element.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        f = new DirSet();
        f.setRefid(new Reference("dummyref"));
        try {
            f.setIncludes("**/*.java");
            fail("Can set includes in DirSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
        try {
            f.setIncludesfile(new File("/a"));
            fail("Can set includesfile in DirSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
        try {
            f.setExcludes("**/*.java");
            fail("Can set excludes in DirSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
        try {
            f.setExcludesfile(new File("/a"));
            fail("Can set excludesfile in DirSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
        try {
            f.setDir(project.resolveFile("."));
            fail("Can set dir in DirSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
        try {
            f.createInclude();
            fail("Can add nested include in DirSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }
        try {
            f.createExclude();
            fail("Can add nested exclude in DirSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }
        try {
            f.createIncludesFile();
            fail("Can add nested includesfile in DirSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }
        try {
            f.createExcludesFile();
            fail("Can add nested excludesfile in DirSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }
        try {
            f.createPatternSet();
            fail("Can add nested patternset in DirSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }
    }

    public void testCircularReferenceCheck() {
        DirSet f = new DirSet();
        project.addReference("dummy", f);
        f.setRefid(new Reference("dummy"));
        try {
            f.getDir(project);
            fail("Can make DirSet a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }
        try {
            f.getDirectoryScanner(project);
            fail("Can make DirSet a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3 --> dummy1
        DirSet f1 = new DirSet();
        project.addReference("dummy1", f1);
        f1.setRefid(new Reference("dummy2"));
        DirSet f2 = new DirSet();
        project.addReference("dummy2", f2);
        f2.setRefid(new Reference("dummy3"));
        DirSet f3 = new DirSet();
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
            f1.getDirectoryScanner(project);
            fail("Can make circular reference.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3 
        // (which has the Project's basedir as root).
        f1 = new DirSet();
        project.addReference("dummy1", f1);
        f1.setRefid(new Reference("dummy2"));
        f2 = new DirSet();
        project.addReference("dummy2", f2);
        f2.setRefid(new Reference("dummy3"));
        f3 = new DirSet();
        project.addReference("dummy3", f3);
        f3.setDir(project.resolveFile("."));
        File dir = f1.getDir(project);
        assertEquals("Dir is basedir", dir, project.getBaseDir());
    }

    public void testFileSetIsNoDirSet() {
        DirSet ds = new DirSet();
        ds.setProject(project);
        FileSet fs = new FileSet();
        fs.setProject(project);
        project.addReference("dummy", fs);
        ds.setRefid(new Reference("dummy"));
        try {
            ds.getDir(project);
            fail("DirSet created from FileSet reference");
        } catch (BuildException e) {
            assertEquals("dummy doesn\'t denote a dirset", e.getMessage());
        }

        ds = new DirSet();
        ds.setProject(project);
        project.addReference("dummy2", ds);
        fs.setRefid(new Reference("dummy2"));
        try {
            fs.getDir(project);
            fail("FileSet created from DirSet reference");
        } catch (BuildException e) {
            assertEquals("dummy2 doesn\'t denote a fileset", e.getMessage());
        }

    }

}
