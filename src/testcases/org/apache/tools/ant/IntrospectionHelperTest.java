/*
 * Copyright  2000-2001,2003-2004 The Apache Software Foundation
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

package org.apache.tools.ant;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;
import java.io.File;
import java.util.*;
import org.apache.tools.ant.taskdefs.condition.Os;

/**
 * JUnit 3 testcases for org.apache.tools.ant.IntrospectionHelper.
 *
 * @author Stefan Bodewig
 */

public class IntrospectionHelperTest extends TestCase {

    private Project p;
    private static final String projectBasedir = File.separator;

    public IntrospectionHelperTest(String name) {
        super(name);
    }

    public void setUp() {
        p = new Project();
        p.setBasedir(projectBasedir);
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
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
    }

    public void testSupportsCharacters() {
        IntrospectionHelper ih = IntrospectionHelper.getHelper(java.lang.String.class);
        assertTrue("String doesn\'t support addText", !ih.supportsCharacters());
        ih = IntrospectionHelper.getHelper(getClass());
        assertTrue("IntrospectionHelperTest supports addText",
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
            fail("addEleven takes primitive argument");
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
            assertTrue(be.getException() instanceof NullPointerException);
        }

        try {
            ih.createElement(p, this, "fourteen");
            fail("fifteen throws NullPointerException");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof NullPointerException);
        }
    }

    public void testGetNestedElements() {
        Hashtable h = new Hashtable();
        h.put("six", java.lang.String.class);
        h.put("thirteen", java.lang.StringBuffer.class);
        h.put("fourteen", java.lang.StringBuffer.class);
        h.put("fifteen", java.lang.StringBuffer.class);
        IntrospectionHelper ih = IntrospectionHelper.getHelper(getClass());
        Enumeration e = ih.getNestedElements();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            Class expect = (Class) h.get(name);
            assertNotNull("Support for "+name+" in IntrospectioNHelperTest?",
                          expect);
            assertEquals("Return type of "+name, expect, ih.getElementType(name));
            h.remove(name);
        }
        assertTrue("Found all", h.isEmpty());
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
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "eight", "2");
        try {
            ih.setAttribute(p, this, "eight", "3");
            fail("2 shouldn't be equals to three - as int");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "nine", "2");
        try {
            ih.setAttribute(p, this, "nine", "3");
            fail("2 shouldn't be equals to three - as Integer");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "ten", "2");
        try {
            ih.setAttribute(p, this, "ten", "3");
            fail(projectBasedir+"2 shouldn't be equals to "+projectBasedir+"3");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "eleven", "2");
        try {
            ih.setAttribute(p, this, "eleven", "on");
            fail("on shouldn't be false");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "twelve", "2");
        try {
            ih.setAttribute(p, this, "twelve", "on");
            fail("on shouldn't be false");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "thirteen", "org.apache.tools.ant.Project");
        try {
            ih.setAttribute(p, this, "thirteen", "org.apache.tools.ant.ProjectHelper");
            fail("org.apache.tools.ant.Project shouldn't be equal to org.apache.tools.ant.ProjectHelper");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
        try {
            ih.setAttribute(p, this, "thirteen", "org.apache.tools.ant.Project2");
            fail("org.apache.tools.ant.Project2 doesn't exist");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof ClassNotFoundException);
        }
        ih.setAttribute(p, this, "fourteen", "2");
        try {
            ih.setAttribute(p, this, "fourteen", "on");
            fail("2 shouldn't be equals to three - as StringBuffer");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "fifteen", "abcd");
        try {
            ih.setAttribute(p, this, "fifteen", "on");
            fail("o shouldn't be equal to a");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "sixteen", "abcd");
        try {
            ih.setAttribute(p, this, "sixteen", "on");
            fail("o shouldn't be equal to a");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "seventeen", "17");
        try {
            ih.setAttribute(p, this, "seventeen", "3");
            fail("17 shouldn't be equals to three");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "eightteen", "18");
        try {
            ih.setAttribute(p, this, "eightteen", "3");
            fail("18 shouldn't be equals to three");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
        }
        ih.setAttribute(p, this, "nineteen", "19");
        try {
            ih.setAttribute(p, this, "nineteen", "3");
            fail("19 shouldn't be equals to three");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
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
        h.put("seventeen", java.lang.Byte.TYPE);
        h.put("eightteen", java.lang.Short.TYPE);
        h.put("nineteen", java.lang.Double.TYPE);

        /*
         * JUnit 3.7 adds a getName method to TestCase - so we now
         * have a name attribute in IntrospectionHelperTest if we run
         * under JUnit 3.7 but not in earlier versions.
         *
         * Simply add it here and remove it after the tests.
         */
        h.put("name", java.lang.String.class);

        IntrospectionHelper ih = IntrospectionHelper.getHelper(getClass());
        Enumeration e = ih.getAttributes();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            Class expect = (Class) h.get(name);
            assertNotNull("Support for "+name+" in IntrospectionHelperTest?",
                          expect);
            assertEquals("Type of "+name, expect, ih.getAttributeType(name));
            h.remove(name);
        }
        h.remove("name");
        assertTrue("Found all", h.isEmpty());
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
        if (Os.isFamily("unix") || Os.isFamily("openvms")) {
            assertEquals(projectBasedir+"2", f.getAbsolutePath());
        } else if (Os.isFamily("netware")) {
            assertEquals(projectBasedir+"2", f.getAbsolutePath().toLowerCase(Locale.US));
        } else {
            assertEquals(":"+projectBasedir+"2", f.getAbsolutePath().toLowerCase(Locale.US).substring(1));
        }
    }

    public void setEleven(boolean b) {
        assertTrue(!b);
    }

    public void setTwelve(Boolean b) {
        assertTrue(!b.booleanValue());
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

    public void setSeventeen(byte b) {
        assertEquals(17, b);
    }

    public void setEightteen(short s) {
        assertEquals(18, s);
    }

    public void setNineteen(double d) {
        assertEquals(19, d, 1e-6);
    }

}// IntrospectionHelperTest
