/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * JUnit 3 testcases for org.apache.tools.ant.types.Path
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */

public class PathTest extends TestCase {

    public static boolean isUnixStyle = File.pathSeparatorChar == ':';

    private Project project;

    public PathTest(String name) {
        super(name);
    }

    public void setUp() {
        project = new Project();
        project.setBasedir(".");
    }

    // actually tests constructor as well as setPath
    public void testConstructor() {
        Path p = new Path(project, "/a:/b");
        String[] l = p.list();
        assertEquals("two items, Unix style", 2, l.length);
        if (isUnixStyle) {
            assertEquals("/a", l[0]);
            assertEquals("/b", l[1]);
        } else {
            assertEquals(":\\a", l[0].substring(1));
            assertEquals(":\\b", l[1].substring(1));
        }        

        p = new Path(project, "\\a;\\b");
        l = p.list();
        assertEquals("two items, DOS style", 2, l.length);
        if (isUnixStyle) {
            assertEquals("/a", l[0]);
            assertEquals("/b", l[1]);
        } else {
            assertEquals(":\\a", l[0].substring(1));
            assertEquals(":\\b", l[1].substring(1));
        }        

        p = new Path(project, "\\a;\\b:/c");
        l = p.list();
        assertEquals("three items, mixed style", 3, l.length);
        if (isUnixStyle) {
            assertEquals("/a", l[0]);
            assertEquals("/b", l[1]);
            assertEquals("/c", l[2]);
        } else {
            assertEquals(":\\a", l[0].substring(1));
            assertEquals(":\\b", l[1].substring(1));
            assertEquals(":\\c", l[2].substring(1));
        }        

        p = new Path(project, "c:\\test");
        l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 2, l.length);
            assert("c resolved relative to project\'s basedir", 
                   l[0].endsWith("/c"));
            assertEquals("/test", l[1]);
        } else {
            assertEquals("drives on DOS", 1, l.length);
            assertEquals("c:\\test", l[0].toLowerCase());
        }

        p = new Path(project, "c:/test");
        l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 2, l.length);
            assert("c resolved relative to project\'s basedir", 
                   l[0].endsWith("/c"));
            assertEquals("/test", l[1]);
        } else {
            assertEquals("drives on DOS", 1, l.length);
            assertEquals("c:\\test", l[0].toLowerCase());
        }
    }

    public void testSetLocation() {
        Path p = new Path(project);
        p.setLocation(new File(File.separatorChar+"a"));
        String[] l = p.list();
        if (isUnixStyle) {
            assertEquals(1, l.length);
            assertEquals("/a", l[0]);
        } else {
            assertEquals(1, l.length);
            assertEquals(":\\a", l[0].substring(1));
        }
    }

    public void testAppending() {
        Path p = new Path(project, "/a:/b");
        String[] l = p.list();
        assertEquals("2 after construction", 2, l.length);
        p.setLocation(new File("/c"));
        l = p.list();
        assertEquals("3 after setLocation", 3, l.length);
        p.setPath("\\d;\\e");
        l = p.list();
        assertEquals("5 after setPath", 5, l.length);
        p.append(new Path(project, "\\f"));
        l = p.list();
        assertEquals("6 after append", 6, l.length);
        p.createPath().setLocation(new File("/g"));
        l = p.list();
        assertEquals("7 after append", 7, l.length);
    }

    public void testEmpyPath() {
        Path p = new Path(project, "");
        String[] l = p.list();
        assertEquals("0 after construction", 0, l.length);
        p.setPath("");
        l = p.list();
        assertEquals("0 after setPath", 0, l.length);
        p.append(new Path(project));
        l = p.list();
        assertEquals("0 after append", 0, l.length);
        p.createPath();
        l = p.list();
        assertEquals("0 after append", 0, l.length);
    }

    public void testUnique() {
        Path p = new Path(project, "/a:/a");
        String[] l = p.list();
        assertEquals("1 after construction", 1, l.length);
        p.setLocation(new File(File.separatorChar+"a"));
        l = p.list();
        assertEquals("1 after setLocation", 1, l.length);
        p.setPath("\\a;/a");
        l = p.list();
        assertEquals("1 after setPath", 1, l.length);
        p.append(new Path(project, "/a;\\a:\\a"));
        l = p.list();
        assertEquals("1 after append", 1, l.length);
        p.createPath().setPath("\\a:/a");
        l = p.list();
        assertEquals("1 after append", 1, l.length);
    }

    public void testEmptyElementIfIsReference() {
        Path p = new Path(project, "/a:/a");
        try {
            p.setRefid(new Reference("dummyref"));
            fail("Can add reference to Path with elements from constructor");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        p = new Path(project);
        p.setLocation(new File("/a"));
        try {
            p.setRefid(new Reference("dummyref"));
            fail("Can add reference to Path with elements from setLocation");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        p = new Path(project);
        p.setRefid(new Reference("dummyref"));
        try {
            p.setLocation(new File("/a"));
            fail("Can set location in Path that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        try {
            p.setPath("/a;\\a");
            fail("Can set path in Path that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        try {
            p.createPath();
            fail("Can create nested Path in Path that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }

        try {
            p.createPathElement();
            fail("Can create nested PathElement in Path that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }

        try {
            p.addFileset(new FileSet());
            fail("Can add nested FileSet in Path that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }
    }

    public void testCircularReferenceCheck() {
        Path p = new Path(project);
        project.addReference("dummy", p);
        p.setRefid(new Reference("dummy"));
        try {
            p.list();
            fail("Can make Path a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3 --> dummy1
        Path p1 = new Path(project);
        project.addReference("dummy1", p1);
        Path p2 = p1.createPath();
        project.addReference("dummy2", p2);
        Path p3 = p2.createPath();
        project.addReference("dummy3", p3);
        p3.setRefid(new Reference("dummy1"));
        try {
            p1.list();
            fail("Can make circular reference.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3 (with Path "/a")
        p1 = new Path(project);
        project.addReference("dummy1", p1);
        p2 = p1.createPath();
        project.addReference("dummy2", p2);
        p3 = p2.createPath();
        project.addReference("dummy3", p3);
        p3.setLocation(new File("/a"));
        String[] l = p1.list();
        assertEquals("One element buried deep inside a nested path structure",
                     1, l.length);
        if (isUnixStyle) {
            assertEquals("/a", l[0]);
        } else {
            assertEquals(":\\a", l[0].substring(1));
        }
    }

}
