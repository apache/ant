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

package org.apache.tools.ant;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;
import java.io.File;
import java.util.*;

/**
 * JUnit 3 testcases for org.apache.tools.ant.IntrospectionHelper.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */

public class IntrospectionHelperTest extends TestCase {

    private Project p;

    public static boolean isUnixStyle = File.pathSeparatorChar == ':';

    public IntrospectionHelperTest(String name) {
        super(name);
    }
    
    public void setUp() {
        p = new Project();
        p.setBasedir("/tmp");
    }

    public void testAddText() throws BuildException {
        IntrospectionHelper ih = IntrospectionHelper.getHelper(java.lang.String.class);
        try {
            ih.addText(p, "", "test");
            fail("String doesn\'t support addText");
        } catch (BuildException be) {
        }

        ih = IntrospectionHelper.getHelper(getClass());
        ih.addText(p, this, "test");
        try {
            ih.addText(p, this, "test2");
            fail("test2 shouldn\'t be equal to test");
        } catch (BuildException be) {
            assert(be.getException() instanceof AssertionFailedError);
        }
    }

    public void testSupportsCharacters() {
        IntrospectionHelper ih = IntrospectionHelper.getHelper(java.lang.String.class);
        assert("String doesn\'t support addText", !ih.supportsCharacters());
        ih = IntrospectionHelper.getHelper(getClass());
        assert("IntrospectionHelperTest supports addText", 
               ih.supportsCharacters());
    }

    public void addText(String text) {
        assertEquals("test", text);
    }
    
    public void testElementCreators() throws BuildException {
        IntrospectionHelper ih = IntrospectionHelper.getHelper(getClass());
        try {
            ih.getElementType("one");
            fail("don't have element type one");
        } catch (BuildException be) {
        }
        try {
            ih.getElementType("two");
            fail("createTwo takes arguments");
        } catch (BuildException be) {
        }
        try {
            ih.getElementType("three");
            fail("createThree returns void");
        } catch (BuildException be) {
        }
        try {
            ih.getElementType("four");
            fail("createFour returns array");
        } catch (BuildException be) {
        }
        try {
            ih.getElementType("five");
            fail("createFive returns primitive type");
        } catch (BuildException be) {
        }
        assertEquals(java.lang.String.class, ih.getElementType("six"));
        assertEquals("test", ih.createElement(p, this, "six"));

        try {
            ih.getElementType("seven");
            fail("addSeven takes two arguments");
        } catch (BuildException be) {
        }
        try {
            ih.getElementType("eight");
            fail("addEight takes no arguments");
        } catch (BuildException be) {
        }
        try {
            ih.getElementType("nine");
            fail("nine return non void");
        } catch (BuildException be) {
        }
        try {
            ih.getElementType("ten");
            fail("addTen takes array argument");
        } catch (BuildException be) {
        }
        try {
            ih.getElementType("eleven");
            fail("addTen takes primitive argument");
        } catch (BuildException be) {
        }
        try {
            ih.getElementType("twelve");
            fail("no primitive constructor for java.lang.Class");
        } catch (BuildException be) {
        }
        assertEquals(java.lang.StringBuffer.class, ih.getElementType("thirteen"));
        assertEquals("test", ih.createElement(p, this, "thirteen").toString());

        try {
            ih.createElement(p, this, "fourteen");
            fail("fourteen throws NullPointerException");
        } catch (BuildException be) {
            assert(be.getException() instanceof NullPointerException);
        }

        try {
            ih.createElement(p, this, "fourteen");
            fail("fifteen throws NullPointerException");
        } catch (BuildException be) {
            assert(be.getException() instanceof NullPointerException);
        }
    }
    
    public void testGetNestedElements() {
        Hashtable h = new Hashtable();
        h.put("six", java.lang.String.class);
        h.put("thirteen", java.lang.StringBuffer.class);
        h.put("fourteen", java.lang.StringBuffer.class);
        h.put("fifteen", java.lang.StringBuffer.class);
        IntrospectionHelper ih = IntrospectionHelper.getHelper(getClass());
        Enumeration enum = ih.getNestedElements();
        while (enum.hasMoreElements()) {
            String name = (String) enum.nextElement();
            Class expect = (Class) h.get(name);
            assertNotNull("Support for "+name+" in IntrospectioNHelperTest?",
                          expect);
            assertEquals("Return type of "+name, expect, ih.getElementType(name));
            h.remove(name);
        }
        assert("Found all", h.isEmpty());
    }

    public Object createTwo(String s) {
        return null;
    }

    public void createThree() {}

    public Object[] createFour() {
        return null;
    }

    public int createFive() {
        return 0;
    }

    public String createSix() {
        return "test";
    }

    public StringBuffer createFifteen() {
        throw new NullPointerException();
    }

    public void addSeven(String s, String s2) {}

    public void addEight() {}

    public String addNine(String s) {
        return null;
    }

    public void addTen(String[] s) {}

    public void addEleven(int i) {}

    public void addTwelve(Class c) {}

    public void addThirteen(StringBuffer sb) {
        sb.append("test");
    }
    
    public void addFourteen(StringBuffer s) {
        throw new NullPointerException();
    }

    public void testAttributeSetters() throws BuildException {
        IntrospectionHelper ih = IntrospectionHelper.getHelper(getClass());
        try {
            ih.setAttribute(p, this, "one", "test");
            fail("setOne doesn't exist");
        } catch (BuildException be) {
        }
        try {
            ih.setAttribute(p, this, "two", "test");
            fail("setTwo returns non void");
        } catch (BuildException be) {
        }
        try {
            ih.setAttribute(p, this, "three", "test");
            fail("setThree takes no args");
        } catch (BuildException be) {
        }
        try {
            ih.setAttribute(p, this, "four", "test");
            fail("setFour takes two args");
        } catch (BuildException be) {
        }
        try {
            ih.setAttribute(p, this, "five", "test");
            fail("setFive takes array arg");
        } catch (BuildException be) {
        }
        try {
            ih.setAttribute(p, this, "six", "test");
            fail("Project doesn't have a String constructor");
        } catch (BuildException be) {
        }
        ih.setAttribute(p, this, "seven", "2");
        try {
            ih.setAttribute(p, this, "seven", "3");
            fail("2 shouldn't be equals to three");
        } catch (BuildException be) {
            assert(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "eight", "2");
        try {
            ih.setAttribute(p, this, "eight", "3");
            fail("2 shouldn't be equals to three - as int");
        } catch (BuildException be) {
            assert(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "nine", "2");
        try {
            ih.setAttribute(p, this, "nine", "3");
            fail("2 shouldn't be equals to three - as Integer");
        } catch (BuildException be) {
            assert(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "ten", "2");
        try {
            ih.setAttribute(p, this, "ten", "3");
            fail("/tmp/2 shouldn't be equals to /tmp/3");
        } catch (BuildException be) {
            assert(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "eleven", "2");
        try {
            ih.setAttribute(p, this, "eleven", "on");
            fail("on shouldn't be false");
        } catch (BuildException be) {
            assert(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "twelve", "2");
        try {
            ih.setAttribute(p, this, "twelve", "on");
            fail("on shouldn't be false");
        } catch (BuildException be) {
            assert(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "thirteen", "org.apache.tools.ant.Project");
        try {
            ih.setAttribute(p, this, "thirteen", "org.apache.tools.ant.ProjectHelper");
            fail("org.apache.tools.ant.Project shouldn't be equal to org.apache.tools.ant.ProjectHelper");
        } catch (BuildException be) {
            assert(be.getException() instanceof AssertionFailedError);
        }
        try {
            ih.setAttribute(p, this, "thirteen", "org.apache.tools.ant.Project2");
            fail("org.apache.tools.ant.Project2 doesn't exist");
        } catch (BuildException be) {
            assert(be.getException() instanceof ClassNotFoundException);
        }
        ih.setAttribute(p, this, "fourteen", "2");
        try {
            ih.setAttribute(p, this, "fourteen", "on");
            fail("2 shouldn't be equals to three - as StringBuffer");
        } catch (BuildException be) {
            assert(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "fifteen", "abcd");
        try {
            ih.setAttribute(p, this, "fifteen", "on");
            fail("o shouldn't be equal to a");
        } catch (BuildException be) {
            assert(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "sixteen", "abcd");
        try {
            ih.setAttribute(p, this, "sixteen", "on");
            fail("o shouldn't be equal to a");
        } catch (BuildException be) {
            assert(be.getException() instanceof AssertionFailedError);
        }
    }

    public void testGetAttributes() {
        Hashtable h = new Hashtable();
        h.put("seven", java.lang.String.class);
        h.put("eight", java.lang.Integer.TYPE);
        h.put("nine", java.lang.Integer.class);
        h.put("ten", java.io.File.class);
        h.put("eleven", java.lang.Boolean.TYPE);
        h.put("twelve", java.lang.Boolean.class);
        h.put("thirteen", java.lang.Class.class);
        h.put("fourteen", java.lang.StringBuffer.class);
        h.put("fifteen", java.lang.Character.TYPE);
        h.put("sixteen", java.lang.Character.class);

        /*
         * JUnit 3.7 adds a getName method to TestCase - so we now
         * have a name attribute in IntrospectionHelperTest if we run
         * under JUnit 3.7 but not in earlier versions.
         *
         * Simply add it here and remove it after the tests.
         */
        h.put("name", java.lang.String.class);

        IntrospectionHelper ih = IntrospectionHelper.getHelper(getClass());
        Enumeration enum = ih.getAttributes();
        while (enum.hasMoreElements()) {
            String name = (String) enum.nextElement();
            Class expect = (Class) h.get(name);
            assertNotNull("Support for "+name+" in IntrospectionHelperTest?",
                          expect);
            assertEquals("Type of "+name, expect, ih.getAttributeType(name));
            h.remove(name);
        }
        h.remove("name");
        assert("Found all", h.isEmpty());
    }

    public int setTwo(String s) {
        return 0;
    }

    public void setThree() {}

    public void setFour(String s1, String s2) {}

    public void setFive(String[] s) {}

    public void setSix(Project p) {}

    public void setSeven(String s) {
        assertEquals("2", s);
    }

    public void setEight(int i) {
        assertEquals(2, i);
    }

    public void setNine(Integer i) {
        assertEquals(2, i.intValue());
    }

    public void setTen(File f) {
        if (isUnixStyle) { 
            assertEquals("/tmp/2", f.getAbsolutePath());
        } else {
            assertEquals(":\\tmp\\2", f.getAbsolutePath().toLowerCase().substring(1));
        }
    }

    public void setEleven(boolean b) {
        assert(!b);
    }

    public void setTwelve(Boolean b) {
        assert(!b.booleanValue());
    }

    public void setThirteen(Class c) {
        assertEquals(Project.class, c);
    }

    public void setFourteen(StringBuffer sb) {
        assertEquals("2", sb.toString());
    }

    public void setFifteen(char c) {
        assertEquals(c, 'a');
    }

    public void setSixteen(Character c) {
        assertEquals(c.charValue(), 'a');
    }

}// IntrospectionHelperTest
