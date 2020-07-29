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
 */

package org.apache.tools.ant;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * JUnit testcases for org.apache.tools.ant.IntrospectionHelper.
 *
 */

public class IntrospectionHelperTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Project p;
    private IntrospectionHelper ih;
    private static final String projectBasedir = File.separator;

    @Before
    public void setUp() {
        p = new Project();
        p.setBasedir(projectBasedir);
        ih = IntrospectionHelper.getHelper(getClass());
    }

    @Test
    public void testIsDynamic() {
        assertFalse("Not dynamic", ih.isDynamic());
    }

    @Test
    public void testIsContainer() {
        assertFalse("Not a container", ih.isContainer());
    }

    @Test
    public void testAddText() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(ComparisonFailure.class)));
        ih.addText(p, this, "test");
        ih.addText(p, this, "test2");
    }

    @Test(expected = BuildException.class)
    public void testAddTextToString() throws BuildException {
        ih = IntrospectionHelper.getHelper(String.class);
        ih.addText(p, "", "test");
    }

    @Ignore("This silently ignores a build exception")
    @Test(expected = BuildException.class)
    public void testGetAddTextMethod() {
        Method m = ih.getAddTextMethod();
        assertMethod(m, "addText", String.class, "test", "bing!");
        IntrospectionHelper.getHelper(String.class).getAddTextMethod();
    }

    @Test
    public void testSupportsCharacters() {
        assertTrue("IntrospectionHelperTest supports addText", ih.supportsCharacters());
        ih = IntrospectionHelper.getHelper(String.class);
        assertFalse("String doesn't support addText", ih.supportsCharacters());
    }

    public void addText(String text) {
        assertEquals("test", text);
    }

    /**
     * Fail: don't have element type one
     */
    @Test(expected = BuildException.class)
    public void testElementCreatorOne() {
        ih.getElementType("one");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: createTwo takes arguments
     */
    @Test(expected = BuildException.class)
    public void testElementCreatorTwo() {
        ih.getElementType("two");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: createThree returns void
     */
    @Test(expected = BuildException.class)
    public void testElementCreatorThree() {
        ih.getElementType("three");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: createFour returns array
     */
    @Test(expected = BuildException.class)
    public void testElementCreatorFour() {
        ih.getElementType("four");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: createFive returns primitive type
     */
    @Test(expected = BuildException.class)
    public void testElementCreatorFive() {
        ih.getElementType("five");
        // TODO we should be asserting a value in here
    }

    @Test
    public void testElementCreatorSix() throws BuildException {
        assertEquals(String.class, ih.getElementType("six"));
        assertEquals("test", ih.createElement(p, this, "six"));
    }

    /**
     * Fail: addSeven takes two arguments
     */
    @Test(expected = BuildException.class)
    public void testElementCreatorSeven() {
        ih.getElementType("seven");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: addEight takes no arguments
     */
    @Test(expected = BuildException.class)
    public void testElementCreatorEight() {
        ih.getElementType("eight");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: addNine returns non void
     */
    @Test(expected = BuildException.class)
    public void testElementCreatorNine() {
        ih.getElementType("nine");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: addTen takes array argument
     */
    @Test(expected = BuildException.class)
    public void testElementCreatorTen() {
        ih.getElementType("ten");
        // TODO we should be asserting a value in here
    }

    /**
     * addEleven takes primitive argument
     */
    @Test(expected = BuildException.class)
    public void testElementCreatorEleven() {
        ih.getElementType("eleven");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: no primitive constructor for java.lang.Class
     */
    @Test(expected = BuildException.class)
    public void testElementCreatorTwelve() throws BuildException {
        ih.getElementType("twelve");
        // TODO we should be asserting a value in here
    }

    @Test
    public void testElementCreatorThirteen() throws BuildException {
        assertEquals(StringBuffer.class, ih.getElementType("thirteen"));
        assertEquals("test", ih.createElement(p, this, "thirteen").toString());
    }

    /**
     * Fail: fourteen throws NullPointerException
     */
    @Test
    public void testElementCreatorFourteen() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(NullPointerException.class)));
        ih.createElement(p, this, "fourteen");
    }

    /**
     * Fail: fifteen throws NullPointerException
     */
    @Test
    public void testElementCreatorFifteen() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(NullPointerException.class)));
        ih.createElement(p, this, "fifteen");
    }

    private Map<String, Class<?>> getExpectedNestedElements() {
        Map<String, Class<?>> elemMap = new Hashtable<>();
        elemMap.put("six", String.class);
        elemMap.put("thirteen", StringBuffer.class);
        elemMap.put("fourteen", StringBuffer.class);
        elemMap.put("fifteen", StringBuffer.class);
        return elemMap;
    }

    @Test
    public void testGetNestedElements() {
        Map<String, Class<?>> elemMap = getExpectedNestedElements();
        for (String name : Collections.list(ih.getNestedElements())) {
            Class<?> expect = elemMap.get(name);
            assertNotNull("Support for " + name + " in IntrospectionHelperTest?",
                          expect);
            assertEquals("Return type of " + name, expect, ih.getElementType(name));
            elemMap.remove(name);
        }
        assertTrue("Found all", elemMap.isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetNestedElementMap() {
        Map<String, Class<?>> elemMap = getExpectedNestedElements();
        Map<String, Class<?>> actualMap = ih.getNestedElementMap();
        actualMap.forEach((elemName, value) -> {
            Class<?> elemClass = elemMap.get(elemName);
            assertNotNull("Support for " + elemName + " in IntrospectionHelperTest?", elemClass);
            assertEquals("Type of " + elemName, elemClass, value);
            elemMap.remove(elemName);
        });
        assertTrue("Found all", elemMap.isEmpty());

        // Check it's a read-only map.
        actualMap.clear();
        // TODO we should be asserting a value somewhere in here
    }

    @Test
    public void testGetElementMethod() {
        assertElemMethod("six", "createSix", String.class, null);
        assertElemMethod("thirteen", "addThirteen", null, StringBuffer.class);
        assertElemMethod("fourteen", "addFourteen", null, StringBuffer.class);
        assertElemMethod("fifteen", "createFifteen", StringBuffer.class, null);
    }

    private void assertElemMethod(String elemName, String methodName,
                                  Class<?> returnType, Class<?> methodArg) {
        Method m = ih.getElementMethod(elemName);
        assertEquals("Method name", methodName, m.getName());
        Class<?> expectedReturnType = (returnType == null) ? Void.TYPE : returnType;
        assertEquals("Return type", expectedReturnType, m.getReturnType());
        Class<?>[] args = m.getParameterTypes();
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

    public void createThree() {
    }

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

    public void addSeven(String s, String s2) {
    }

    public void addEight() {
    }

    public String addNine(String s) {
        return null;
    }

    public void addTen(String[] s) {
    }

    public void addEleven(int i) {
    }

    public void addTwelve(Class<?> c) {
    }

    public void addThirteen(StringBuffer sb) {
        sb.append("test");
    }

    public void addFourteen(StringBuffer s) {
        throw new NullPointerException();
    }

    /**
     * Fail: setOne doesn't exist
     */
    @Test(expected = BuildException.class)
    public void testAttributeSetterOne() {
        ih.setAttribute(p, this, "one", "test");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: setTwo returns non void
     */
    @Test(expected = BuildException.class)
    public void testAttributeSetterTwo() {
        ih.setAttribute(p, this, "two", "test");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: setThree takes no args
     */
    @Test(expected = BuildException.class)
    public void testAttributeSetterThree() {
        ih.setAttribute(p, this, "three", "test");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: setFour takes two args
     */
    @Test(expected = BuildException.class)
    public void testAttributeSetterFour() {
        ih.setAttribute(p, this, "four", "test");
        //TODO we should be asserting a value in here
    }

    /**
     * Fail: setFive takes array arg
     */
    @Test(expected = BuildException.class)
    public void testAttributeSetterFive() {
        ih.setAttribute(p, this, "five", "test");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: Project doesn't have a String constructor
     */
    @Test(expected = BuildException.class)
    public void testAttributeSetterSix() {
        ih.setAttribute(p, this, "six", "test");
        // TODO we should be asserting a value in here
    }

    /**
     * Fail: 2 shouldn't be equal to three
     */
    @Test
    public void testAttributeSetterSeven() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(ComparisonFailure.class)));
        ih.setAttribute(p, this, "seven", "2");
        ih.setAttribute(p, this, "seven", "3");
    }

    /**
     * Fail: 2 shouldn't be equal to three as int
     */
    @Test
    public void testAttributeSetterEight() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(AssertionError.class)));
        ih.setAttribute(p, this, "eight", "2");
        ih.setAttribute(p, this, "eight", "3");
    }

    /**
     * Fail: 2 shouldn't be equal to three as Integer
     */
    @Test
    public void testAttributeSetterNine() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(AssertionError.class)));
        ih.setAttribute(p, this, "nine", "2");
        ih.setAttribute(p, this, "nine", "3");
    }

    /**
     * Fail: string + 2 shouldn't be equal to string + 3
     */
    @Test
    public void testAttributeSetterTen() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(AssertionError.class)));
        ih.setAttribute(p, this, "ten", "2");
        ih.setAttribute(p, this, "ten", "3");
    }

    /**
     * Fail: on shouldn't be false
     */
    @Test
    public void testAttributeSetterEleven() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(AssertionError.class)));
        ih.setAttribute(p, this, "eleven", "2");
        ih.setAttribute(p, this, "eleven", "on");
    }

    /**
     * Fail: on shouldn't be false
     */
    @Test
    public void testAttributeSetterTwelve() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(AssertionError.class)));
        ih.setAttribute(p, this, "twelve", "2");
        ih.setAttribute(p, this, "twelve", "on");
    }

    /**
     * Fail: org.apache.tools.ant.Project shouldn't be equal to org.apache.tools.ant.ProjectHelper
     */
    @Test
    public void testAttributeSetterThirteen() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(AssertionError.class)));
        ih.setAttribute(p, this, "thirteen", "org.apache.tools.ant.Project");
        ih.setAttribute(p, this, "thirteen", "org.apache.tools.ant.ProjectHelper");
    }

    /**
     * Fail: org.apache.tools.ant.Project2 doesn't exist
     */
    @Test
    public void testAttributeSetterThirteenNonExistentClass() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(ClassNotFoundException.class)));
        ih.setAttribute(p, this, "thirteen", "org.apache.tools.ant.Project2");
    }

    /**
     * Fail: 2 shouldn't be equal to three as StringBuffer
     */
    @Test
    public void testAttributeSetterFourteen() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(ComparisonFailure.class)));
        ih.setAttribute(p, this, "fourteen", "2");
            ih.setAttribute(p, this, "fourteen", "on");
    }

    /**
     * Fail: o shouldn't be equal to a
     */
    @Test
    public void testAttributeSetterFifteen() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(AssertionError.class)));
        ih.setAttribute(p, this, "fifteen", "abcd");
        ih.setAttribute(p, this, "fifteen", "on");
    }

    /**
     * Fail: o shouldn't be equal to a
     */
    @Test
    public void testAttributeSetterSixteen() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(AssertionError.class)));
        ih.setAttribute(p, this, "sixteen", "abcd");
        ih.setAttribute(p, this, "sixteen", "on");
    }

    /**
     * Fail: 17 shouldn't be equals to three
     */
    @Test
    public void testAttributeSetterSeventeen() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(AssertionError.class)));
        ih.setAttribute(p, this, "seventeen", "17");
        ih.setAttribute(p, this, "seventeen", "3");
    }

    /**
     * Fail: 18 shouldn't be equals to three
     */
    @Test
    public void testAttributeSetterEighteen() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(AssertionError.class)));
        ih.setAttribute(p, this, "eightteen", "18");
        ih.setAttribute(p, this, "eightteen", "3");
    }

    /**
     * Fail: 19 shouldn't be equals to three
     */
    @Test
    public void testAttributeSetterNineteen() throws BuildException {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("cause", instanceOf(AssertionError.class)));
        ih.setAttribute(p, this, "nineteen", "19");
        ih.setAttribute(p, this, "nineteen", "3");
    }

    private Map<String, Class<?>> getExpectedAttributes() {
        Map<String, Class<?>> attrMap = new Hashtable<>();
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
        attrMap.put("twenty", Path.class);

        /*
         * JUnit 3.7 adds a getName method to TestCase - so we now
         * have a name attribute in IntrospectionHelperTest.
         *
         * Simply add it here and remove it after the tests.
         */
        attrMap.put("name", String.class);

        return attrMap;
    }

    @Test
    public void testGetAttributes() {
        Map<String, Class<?>> attrMap = getExpectedAttributes();
        for (String name : Collections.list(ih.getAttributes())) {
            Class<?> expect = attrMap.get(name);
            assertNotNull("Support for " + name + " in IntrospectionHelperTest?",
                          expect);
            assertEquals("Type of " + name, expect, ih.getAttributeType(name));
            attrMap.remove(name);
        }
        attrMap.remove("name");
        assertTrue("Found all", attrMap.isEmpty());
    }

    @Test
    public void testGetAttributeMap() {
        Map<String, Class<?>> attrMap = getExpectedAttributes();
        ih.getAttributeMap().forEach((attrName, value) -> {
            Class<?> attrClass = attrMap.get(attrName);
            assertNotNull("Support for " + attrName + " in IntrospectionHelperTest?", attrClass);
            assertEquals("Type of " + attrName, attrClass, value);
            attrMap.remove(attrName);
        });
        attrMap.remove("name");
        assertTrue("Found all", attrMap.isEmpty());
    }

    @Test
    public void testClearGetAttributeMap() {
        thrown.expect(UnsupportedOperationException.class);
        // TODO we should be asserting a value somewhere in here
        // Check it's a read-only map.
        ih.getAttributeMap().clear();
    }

    @Test
    public void testGetAttributeMethod() {
        assertAttrMethod("seven", "setSeven", String.class,
                "2", "3");
        assertAttrMethod("eight", "setEight", Integer.TYPE,
                2, 3);
        assertAttrMethod("nine", "setNine", Integer.class,
                2, 3);
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
                'a', 'b');
        assertAttrMethod("sixteen", "setSixteen", Character.class,
                'a', 'b');
        assertAttrMethod("seventeen", "setSeventeen", Byte.TYPE,
                (byte) 17, (byte) 10);
        assertAttrMethod("eightteen", "setEightteen", Short.TYPE,
                (short) 18, (short) 10);
        assertAttrMethod("nineteen", "setNineteen", Double.TYPE,
                19d, (double) (short) 10);
        assertAttrMethod("twenty", "setTwenty", Path.class,
                new File(projectBasedir + 20).toPath(), Paths.get("toto"));

        thrown.expect(BuildException.class);
        thrown.expectMessage("doesn't support the \"onehundred\" attribute.");
        assertAttrMethod("onehundred", null, null, null, null);
    }

    private void assertAttrMethod(String attrName, String methodName,
                                  Class<?> methodArg, Object arg, Object badArg) {
        assertMethod(ih.getAttributeMethod(attrName), methodName, methodArg, arg, badArg);
    }

    public int setTwo(String s) {
        return 0;
    }

    public void setThree() {
    }

    public void setFour(String s1, String s2) {
    }

    public void setFive(String[] s) {
    }

    public void setSix(Project p) {
    }

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
            assertEquals(projectBasedir + "2", path);
        } else if (Os.isFamily("netware")) {
            assertEquals(projectBasedir + "2", path.toLowerCase(Locale.US));
        } else {
            assertEquals(":" + projectBasedir + "2",
                         path.toLowerCase(Locale.US).substring(1));
        }
    }

    public void setEleven(boolean b) {
        assertFalse(b);
    }

    public void setTwelve(Boolean b) {
        assertFalse(b);
    }

    public void setThirteen(Class<?> c) {
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

    public void setTwenty(Path p) {
        String path = p.toAbsolutePath().toString();
        if (Os.isFamily("unix") || Os.isFamily("openvms")) {
            assertEquals(projectBasedir + "20", path);
        } else if (Os.isFamily("netware")) {
            assertEquals(projectBasedir + "20", path.toLowerCase(Locale.US));
        } else {
            assertEquals(":" + projectBasedir + "20",
                         path.toLowerCase(Locale.US).substring(1));
        }
    }

    @Test
    public void testGetExtensionPoints() {
        List<Method> extensions = ih.getExtensionPoints();
        final int adders = 2;
        assertEquals("extension count", adders, extensions.size());

        // this original test assumed something about the order of
        // add(Number) and addConfigured(Map) returned by reflection.
        // Unfortunately the assumption doesn't hold for all VMs
        // (failed on MacOS X using JDK 1.4.2_05) and the possible
        // combinatorics are too hard to check.  We really only want
        // to ensure that the more derived Hashtable can be found
        // before Map.
        // assertMethod(extensions.get(0), "add", Number.class, new Integer(2), new Integer(3));

        // addConfigured(Hashtable) should come before addConfigured(Map)
        assertMethod(extensions.get(adders - 2),
                        "addConfigured", Hashtable.class,
                        makeTable("key", "value"), makeTable("1", "2"));

        assertMethod(extensions.get(adders - 1), "addConfigured", Map.class,
                        new HashMap<String, String>(), makeTable("1", "2"));
    }

    private void assertMethod(Method m, String methodName, Class<?> methodArg,
                              Object arg, Object badArg) {
        assertEquals("Method name", methodName, m.getName());
        assertEquals("Return type", Void.TYPE, m.getReturnType());
        Class<?>[] args = m.getParameterTypes();
        assertEquals("Arg Count", 1, args.length);
        assertEquals("Arg Type", methodArg, args[0]);

        try {
            m.invoke(this, arg);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new BuildException(e);
        }

        try {
            m.invoke(this, badArg);
        } catch (IllegalAccessException e) {
            throw new BuildException(e);
        } catch (InvocationTargetException e) {
            assertThat(e, hasProperty("cause", instanceOf(AssertionError.class)));
        }
    }

    public List<Object> add(List<Object> l) {
        // INVALID extension point
        return null;
    }

    // see comments in testGetExtensionPoints
//    public void add(Number n) {
//        // Valid extension point
//        assertEquals(2, n.intValue());
//    }

    public void add(List<Object> l, int i) {
        // INVALID extension point
    }

    public void addConfigured(Map<Object, Object> m) {
        // Valid extension point
        assertTrue(m.isEmpty());
    }

    public void addConfigured(Hashtable<String, String> h) {
        // Valid extension point, more derived than Map above, but *after* it!
        assertEquals(makeTable("key", "value"), h);
    }

    private Hashtable<String, String> makeTable(String key, String value) {
        Hashtable<String, String> table = new Hashtable<>();
        table.put(key, value);
        return table;
    }

}
