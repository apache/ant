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

package org.apache.tools.ant;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.tools.ant.taskdefs.condition.Os;

/**
 * JUnit 3 testcases for org.apache.tools.ant.IntrospectionHelper.
 *
 */

public class IntrospectionHelperTest extends TestCase {

    private Project p;
    private IntrospectionHelper ih;
    private static final String projectBasedir = File.separator;

    public IntrospectionHelperTest(String name) {
        super(name);
    }

    public void setUp() {
        p = new Project();
        p.setBasedir(projectBasedir);
        ih = IntrospectionHelper.getHelper(getClass());
    }

    public void testIsDynamic() {
        assertTrue("Not dynamic", false == ih.isDynamic());
    }

    public void testIsContainer() {
        assertTrue("Not a container", false == ih.isContainer());
    }

    public void testAddText() throws BuildException {
        ih.addText(p, this, "test");
        try {
            ih.addText(p, this, "test2");
            fail("test2 shouldn\'t be equal to test");
        } catch (BuildException be) {
            assertTrue(be.getException() instanceof AssertionFailedError);
        }

        ih = IntrospectionHelper.getHelper(String.class);
        try {
            ih.addText(p, "", "test");
            fail("String doesn\'t support addText");
        } catch (BuildException be) {
        }
    }

    public void testGetAddTextMethod() {
        Method m = ih.getAddTextMethod();
        assertMethod(m, "addText", String.class, "test", "bing!");

        ih = IntrospectionHelper.getHelper(String.class);
        try {
            m = ih.getAddTextMethod();
        } catch (BuildException e) {}
    }

    public void testSupportsCharacters() {
        assertTrue("IntrospectionHelperTest supports addText",
                   ih.supportsCharacters());

        ih = IntrospectionHelper.getHelper(String.class);
        assertTrue("String doesn\'t support addText", !ih.supportsCharacters());
    }

    public void addText(String text) {
        assertEquals("test", text);
    }

    public void testElementCreators() throws BuildException {
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
        assertEquals(String.class, ih.getElementType("six"));
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
        assertEquals(StringBuffer.class, ih.getElementType("thirteen"));
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

    private Map getExpectedNestedElements() {
        Map elemMap = new Hashtable();
        elemMap.put("six", String.class);
        elemMap.put("thirteen", StringBuffer.class);
        elemMap.put("fourteen", StringBuffer.class);
        elemMap.put("fifteen", StringBuffer.class);
        return elemMap;
    }

    public void testGetNestedElements() {
        Map elemMap = getExpectedNestedElements();
        Enumeration e = ih.getNestedElements();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            Class expect = (Class) elemMap.get(name);
            assertNotNull("Support for "+name+" in IntrospectioNHelperTest?",
                          expect);
            assertEquals("Return type of "+name, expect, ih.getElementType(name));
            elemMap.remove(name);
        }
        assertTrue("Found all", elemMap.isEmpty());
    }

    public void testGetNestedElementMap() {
        Map elemMap = getExpectedNestedElements();
        Map actualMap = ih.getNestedElementMap();
        for (Iterator i = actualMap.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String elemName = (String) entry.getKey();
            Class elemClass = (Class) elemMap.get(elemName);
            assertNotNull("Support for " + elemName +
                          " in IntrospectionHelperTest?", elemClass);
            assertEquals("Type of " + elemName, elemClass, entry.getValue());
            elemMap.remove(elemName);
        }
        assertTrue("Found all", elemMap.isEmpty());

        // Check it's a read-only map.
        try {
            actualMap.clear();
        } catch (UnsupportedOperationException e) {}
    }

    public void testGetElementMethod() {
        assertElemMethod("six", "createSix", String.class, null);
        assertElemMethod("thirteen", "addThirteen", null, StringBuffer.class);
        assertElemMethod("fourteen", "addFourteen", null, StringBuffer.class);
        assertElemMethod("fifteen", "createFifteen", StringBuffer.class, null);
    }

    private void assertElemMethod(String elemName, String methodName,
                                  Class returnType, Class methodArg) {
        Method m = ih.getElementMethod(elemName);
        assertEquals("Method name", methodName, m.getName());
        Class expectedReturnType = (returnType == null)? Void.TYPE: returnType;
        assertEquals("Return type", expectedReturnType, m.getReturnType());
        Class[] args = m.getParameterTypes();
        if (methodArg != null) {
            assertEquals("Arg Count", 1, args.length);
            assertEquals("Arg Type", methodArg, args[0]);
        } else {
            assertEquals("Arg Count", 0, args.length);
        }
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

    private Map getExpectedAttributes() {
        Map attrMap = new Hashtable();
        attrMap.put("seven", String.class);
        attrMap.put("eight", Integer.TYPE);
        attrMap.put("nine", Integer.class);
        attrMap.put("ten", File.class);
        attrMap.put("eleven", Boolean.TYPE);
        attrMap.put("twelve", Boolean.class);
        attrMap.put("thirteen", Class.class);
        attrMap.put("fourteen", StringBuffer.class);
        attrMap.put("fifteen", Character.TYPE);
        attrMap.put("sixteen", Character.class);
        attrMap.put("seventeen", Byte.TYPE);
        attrMap.put("eightteen", Short.TYPE);
        attrMap.put("nineteen", Double.TYPE);

        /*
         * JUnit 3.7 adds a getName method to TestCase - so we now
         * have a name attribute in IntrospectionHelperTest if we run
         * under JUnit 3.7 but not in earlier versions.
         *
         * Simply add it here and remove it after the tests.
         */
        attrMap.put("name", String.class);

        return attrMap;
    }

    public void testGetAttributes() {
        Map attrMap = getExpectedAttributes();
        Enumeration e = ih.getAttributes();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            Class expect = (Class) attrMap.get(name);
            assertNotNull("Support for "+name+" in IntrospectionHelperTest?",
                          expect);
            assertEquals("Type of "+name, expect, ih.getAttributeType(name));
            attrMap.remove(name);
        }
        attrMap.remove("name");
        assertTrue("Found all", attrMap.isEmpty());
    }

    public void testGetAttributeMap() {
        Map attrMap = getExpectedAttributes();
        Map actualMap = ih.getAttributeMap();
        for (Iterator i = actualMap.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String attrName = (String) entry.getKey();
            Class attrClass = (Class) attrMap.get(attrName);
            assertNotNull("Support for " + attrName +
                          " in IntrospectionHelperTest?", attrClass);
            assertEquals("Type of " + attrName, attrClass, entry.getValue());
            attrMap.remove(attrName);
        }
        attrMap.remove("name");
        assertTrue("Found all", attrMap.isEmpty());

        // Check it's a read-only map.
        try {
            actualMap.clear();
        } catch (UnsupportedOperationException e) {}
    }

    public void testGetAttributeMethod() {
        assertAttrMethod("seven", "setSeven", String.class,
                         "2", "3");
        assertAttrMethod("eight", "setEight", Integer.TYPE,
                         new Integer(2), new Integer(3));
        assertAttrMethod("nine", "setNine", Integer.class,
                         new Integer(2), new Integer(3));
        assertAttrMethod("ten", "setTen", File.class,
                         new File(projectBasedir + 2), new File("toto"));
        assertAttrMethod("eleven", "setEleven", Boolean.TYPE,
                         Boolean.FALSE, Boolean.TRUE);
        assertAttrMethod("twelve", "setTwelve", Boolean.class,
                         Boolean.FALSE, Boolean.TRUE);
        assertAttrMethod("thirteen", "setThirteen", Class.class,
                         Project.class, Map.class);
        assertAttrMethod("fourteen", "setFourteen", StringBuffer.class,
                         new StringBuffer("2"), new StringBuffer("3"));
        assertAttrMethod("fifteen", "setFifteen", Character.TYPE,
                         new Character('a'), new Character('b'));
        assertAttrMethod("sixteen", "setSixteen", Character.class,
                         new Character('a'), new Character('b'));
        assertAttrMethod("seventeen", "setSeventeen", Byte.TYPE,
                         new Byte((byte)17), new Byte((byte)10));
        assertAttrMethod("eightteen", "setEightteen", Short.TYPE,
                         new Short((short)18), new Short((short)10));
        assertAttrMethod("nineteen", "setNineteen", Double.TYPE,
                         new Double(19), new Double((short)10));

        try {
            assertAttrMethod("onehundred", null, null, null, null);
            fail("Should have raised a BuildException!");
        } catch (BuildException e) {}
    }

    private void assertAttrMethod(String attrName, String methodName,
                                  Class methodArg, Object arg, Object badArg) {
        Method m = ih.getAttributeMethod(attrName);
        assertMethod(m, methodName, methodArg, arg, badArg);
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
        String path = f.getAbsolutePath();
        if (Os.isFamily("unix") || Os.isFamily("openvms")) {
            assertEquals(projectBasedir+"2", path);
        } else if (Os.isFamily("netware")) {
            assertEquals(projectBasedir+"2", path.toLowerCase(Locale.US));
        } else {
            assertEquals(":"+projectBasedir+"2",
                         path.toLowerCase(Locale.US).substring(1));
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
        double diff = d - 19;
        assertTrue("Expected 19, received " + d, diff > -1e-6 && diff < 1e-6);
    }

    public void testGetExtensionPoints() {
        List extensions = ih.getExtensionPoints();
        final int adders = 2;
        assertEquals("extension count", adders, extensions.size());

        // this original test assumed something about the order of
        // add(Number) and addConfigured(Map) returned by reflection.
        // Unfortunately the assumption doesn't hold for all VMs
        // (failed on MacOS X using JDK 1.4.2_05) and the possible
        // combinatorics are too hard to check.  We really only want
        // to ensure that the more derived Hashtable can be found
        // before Map.
//        assertExtMethod(extensions.get(0), "add", Number.class,
//                        new Integer(2), new Integer(3));

        // addConfigured(Hashtable) should come before addConfigured(Map)
        assertExtMethod(extensions.get(adders - 2),
                        "addConfigured", Hashtable.class,
                        makeTable("key", "value"), makeTable("1", "2"));

        assertExtMethod(extensions.get(adders - 1), "addConfigured", Map.class,
                        new HashMap(), makeTable("1", "2"));
    }

    private void assertExtMethod(Object mo, String methodName, Class methodArg,
                                 Object arg, Object badArg) {
        assertMethod((Method) mo, methodName, methodArg, arg, badArg);
    }

    private void assertMethod(Method m, String methodName, Class methodArg,
                              Object arg, Object badArg) {
        assertEquals("Method name", methodName, m.getName());
        assertEquals("Return type", Void.TYPE, m.getReturnType());
        Class[] args = m.getParameterTypes();
        assertEquals("Arg Count", 1, args.length);
        assertEquals("Arg Type", methodArg, args[0]);

        try {
            m.invoke(this, new Object[] { arg });
        } catch (IllegalAccessException e) {
            throw new BuildException(e);
        } catch (InvocationTargetException e) {
            throw new BuildException(e);
        }

        try {
            m.invoke(this, new Object[] { badArg });
            fail("Should have raised an assertion exception");
        } catch (IllegalAccessException e) {
            throw new BuildException(e);
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            assertTrue(t instanceof junit.framework.AssertionFailedError);
        }
    }

    public List add(List l) {
        // INVALID extension point
        return null;
    }

    // see comments in testGetExtensionPoints
//    public void add(Number n) {
//        // Valid extension point
//        assertEquals(2, n.intValue());
//    }

    public void add(List l, int i) {
        // INVALID extension point
    }

    public void addConfigured(Map m) {
        // Valid extension point
        assertTrue(m.size() == 0);
    }

    public void addConfigured(Hashtable h) {
        // Valid extension point, more derived than Map above, but *after* it!
        assertEquals(makeTable("key", "value"), h);
    }

    private Hashtable makeTable(Object key, Object value) {
        Hashtable table = new Hashtable();
        table.put(key, value);
        return table;
    }

} // IntrospectionHelperTest

