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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

package org.apache.tools.ant;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import java.io.File;

/**
 * JUnit 3 testcases for org.apache.tools.ant.Path
 *
 * @author Stefan Bodewig <a href="mailto:stefan.bodewig@megabit.net">stefan.bodewig@megabit.net</a> 
 */

public class PathTest extends TestCase {

    public static boolean isUnixStyle = File.pathSeparatorChar == ':';

    public PathTest(String name) {
        super(name);
    }

    // actually tests constructor as well as setPath
    public void testConstructor() {
        Path p = new Path("/a:/b");
        String[] l = p.list();
        assertEquals("two items, Unix style", 2, l.length);
        if (isUnixStyle) {
            assertEquals("/a", l[0]);
            assertEquals("/b", l[1]);
        } else {
            assertEquals("\\a", l[0]);
            assertEquals("\\b", l[1]);
        }        

        p = new Path("\\a;\\b");
        l = p.list();
        assertEquals("two items, DOS style", 2, l.length);
        if (isUnixStyle) {
            assertEquals("/a", l[0]);
            assertEquals("/b", l[1]);
        } else {
            assertEquals("\\a", l[0]);
            assertEquals("\\b", l[1]);
        }        

        p = new Path("\\a;\\b:/c");
        l = p.list();
        assertEquals("three items, mixed style", 3, l.length);
        if (isUnixStyle) {
            assertEquals("/a", l[0]);
            assertEquals("/b", l[1]);
            assertEquals("/c", l[2]);
        } else {
            assertEquals("\\a", l[0]);
            assertEquals("\\b", l[1]);
            assertEquals("\\c", l[2]);
        }        

        p = new Path("c:\\test");
        l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 2, l.length);
            assertEquals("c", l[0]);
            assertEquals("/test", l[1]);
        } else {
            assertEquals("drives on DOS", 1, l.length);
            assertEquals("c:\\test", l[0]);
        }

        p = new Path("c:/test");
        l = p.list();
        if (isUnixStyle) {
            assertEquals("no drives on Unix", 2, l.length);
            assertEquals("c", l[0]);
            assertEquals("/test", l[1]);
        } else {
            assertEquals("drives on DOS", 1, l.length);
            assertEquals("c:\\test", l[0]);
        }
    }

    public void testSetLocation() {
        Path p = new Path();
        p.setLocation("/a");
        String[] l = p.list();
        if (isUnixStyle) {
            assertEquals(1, l.length);
            assertEquals("/a", l[0]);
        } else {
            assertEquals(1, l.length);
            assertEquals("\\a", l[0]);
        }

        p = new Path();
        p.setLocation("\\a");
        l = p.list();
        if (isUnixStyle) {
            assertEquals(1, l.length);
            assertEquals("/a", l[0]);
        } else {
            assertEquals(1, l.length);
            assertEquals("\\a", l[0]);
        }
    }

    public void testAppending() {
        Path p = new Path("/a:/b");
        String[] l = p.list();
        assertEquals("2 after construction", 2, l.length);
        p.setLocation("/c");
        l = p.list();
        assertEquals("3 after setLocation", 3, l.length);
        p.setPath("\\d;\\e");
        l = p.list();
        assertEquals("5 after setPath", 5, l.length);
        p.append(new Path("\\f"));
        l = p.list();
        assertEquals("6 after append", 6, l.length);
    }

    public void testEmpyPath() {
        Path p = new Path("");
        String[] l = p.list();
        assertEquals("0 after construction", 0, l.length);
        p.setLocation("");
        l = p.list();
        assertEquals("0 after setLocation", 0, l.length);
        p.setPath("");
        l = p.list();
        assertEquals("0 after setPath", 0, l.length);
        p.append(new Path());
        l = p.list();
        assertEquals("0 after append", 0, l.length);
    }

    public void testUnique() {
        Path p = new Path("/a:/a");
        String[] l = p.list();
        assertEquals("1 after construction", 1, l.length);
        p.setLocation("\\a");
        l = p.list();
        assertEquals("1 after setLocation", 1, l.length);
        p.setPath("\\a;/a");
        l = p.list();
        assertEquals("1 after setPath", 1, l.length);
        p.append(new Path("/a;\\a:\\a"));
        l = p.list();
        assertEquals("1 after append", 1, l.length);
    }

}
