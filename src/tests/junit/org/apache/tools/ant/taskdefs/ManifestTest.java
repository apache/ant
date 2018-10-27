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

package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testcase for the Manifest class used in the jar task.
 *
 */
public class ManifestTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    private File expandedManifest;
    private File outDir;

    public static final String LONG_LINE
        = "AReallyLongLineToTestLineBreakingInManifests-ACapabilityWhich" +
          "IsSureToLeadToHundredsOfQuestionsAboutWhyAntMungesManifests" +
          "OfCourseTheAnswerIsThatIsWhatTheSpecRequiresAndIfAnythingHas" +
          "AProblemWithThatItIsNotABugInAnt";

    public static final String LONG_70_NAME 
        = "ThisNameIsJustSeventyCharactersWhichIsAllowedAccordingToTheSpecsFiller";
    public static final String LONG_68_NAME 
        = "ThisNameIsJustSixtyEightCharactersWhichIsAllowedAccordingToTheSpecsX";
    public static final String NOT_LONG_NAME 
        = "NameIsJustUnderSeventyCharactersWhichIsAllowedAccordingTheSpec";

    public static final String VALUE = "NOT_LONG";


    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/manifest.xml");
        outDir = new File(buildRule.getProject().getProperty("output"));
        expandedManifest = new File(outDir, "manifests/META-INF/MANIFEST.MF");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("tearDown");
    }

    /**
     * Empty manifest - is OK
     */
    @Test
    public void test1() throws ManifestException, IOException {
        buildRule.executeTarget("test1");
        Manifest manifest = getManifest(expandedManifest);
        String version = manifest.getManifestVersion();
        assertEquals("Manifest was not created with correct version - ", "1.0", version);
    }

    /**
     * Simple Manifest with version 2.0
     */
    @Test
    public void test2() throws ManifestException, IOException {
        buildRule.executeTarget("test2");
        Manifest manifest = getManifest(expandedManifest);
        String version = manifest.getManifestVersion();
        assertEquals("Manifest was not created with correct version - ", "2.0", version);
    }

    /**
     * Malformed manifest - no : on the line
     */
    @Test
    public void test3() {
        try {
            buildRule.executeTarget("test3");
            fail("BuildException expected: Manifest is invalid - no colon on header line");
        } catch (BuildException ex) {
            assertContains("Invalid Manifest", ex.getMessage());
        }
    }

    /**
     * Malformed manifest - starts with continuation line
     */
    @Test
    public void test4() {
        try {
            buildRule.executeTarget("test4");
            fail("BuildException expected: Manifest is invalid - section starts with continuation line");
        } catch (BuildException ex) {
            assertContains("Invalid Manifest", ex.getMessage());
        }
   }

    /**
     * Malformed manifest - Name attribute in main section
     */
    @Test
    public void test5() {
        buildRule.executeTarget("test5");
        String output = buildRule.getLog();
        boolean hasWarning = output.indexOf("Manifest warning: \"Name\" attributes should not occur in the main section") != -1;
        assertTrue("Expected warning about Name in main section", hasWarning);
    }

    /**
     * New Section not starting with Name attribute.
     */
    @Test
    public void test6() {
        try {
            buildRule.executeTarget("test6");
            fail("BuildException expected: Manifest is invalid - section starts with incorrect attribute");
        } catch (BuildException ex) {
            assertContains("Invalid Manifest", ex.getMessage());
        }
        String output = buildRule.getLog();
        boolean hasWarning = output.indexOf("Manifest sections should start with a \"Name\" attribute") != -1;
        assertTrue("Expected warning about section not starting with Name: attribute", hasWarning);
    }


    /**
     * From attribute is illegal
     */
    @Test
    public void test7() {
        buildRule.executeTarget("test7");

        boolean hasWarning = buildRule.getLog().indexOf(Manifest.ERROR_FROM_FORBIDDEN) != -1;
        assertTrue("Expected warning about From: attribute", hasWarning);
    }

    /**
     * Inline manifest - OK
     */
    @Test
    public void test8() throws IOException, ManifestException {
        buildRule.executeTarget("test8");
        Manifest manifest = getManifest(expandedManifest);
        Manifest.Section mainSection = manifest.getMainSection();
        String classpath = mainSection.getAttributeValue("class-path");
        assertEquals("Class-Path attribute was not set correctly - ", "fubar", classpath);

        Manifest.Section testSection = manifest.getSection("Test");
        String testAttr = testSection.getAttributeValue("TestAttr");
        assertEquals("TestAttr attribute was not set correctly - ", "Test", testAttr);
    }

    /**
     * Inline manifest - Invalid since has a Name attribute in the section element
     */
    @Test
    public void test9() {
        try {
            buildRule.executeTarget("test9");
            fail("BuildException expected: Construction is invalid - Name attribute should not be used");
        } catch (BuildException ex) {
            assertContains("Specify the section name using the \"name\" attribute of the <section> element",
                           ex.getMessage());
        }
    }

    /**
     * Inline manifest - Invalid attribute without name
     */
    @Test
    public void test10() {
        try {
            buildRule.executeTarget("test10");
            fail("BuildException expected: Attribute has no name");
        } catch (BuildException ex) {
            assertContains("Attributes must have name and value", ex.getMessage());
        }
    }

    /**
     * Inline manifest - Invalid attribute without value
     */
    @Test
    public void test11() {
        try {
            buildRule.executeTarget("test11");
            fail("BuildException expected: Attribute has no value");
        } catch (BuildException ex) {
            assertContains("Attributes must have name and value", ex.getMessage());
        }
    }

    /**
     * Inline manifest - Invalid attribute without value
     */
    @Test
    public void test12() {
        try {
            buildRule.executeTarget("test12");
            fail("BuildException expected: Section with no name");
        } catch (BuildException ex) {
            assertContains("Sections must have a name", ex.getMessage());
        }
    }

    /**
     * Inline manifest - Duplicate attribute
     */
    @Test
    public void test13() {
        try {
            buildRule.executeTarget("test13");
            fail("BuildException expected: Duplicate Attribute");
        } catch (BuildException ex) {
            assertContains("The attribute \"Test\" may not occur more than once in the same section", ex.getMessage());
        }
    }

    /**
     * Inline manifest - OK since classpath entries can be duplicated.
     */
    @Test
    public void test14() throws IOException, ManifestException {
        buildRule.executeTarget("test14");
        Manifest manifest = getManifest(expandedManifest);
        Manifest.Section mainSection = manifest.getMainSection();
        String classpath = mainSection.getAttributeValue("class-path");
        assertEquals("Class-Path attribute was not set correctly - ",
            "Test1 Test2 Test3 Test4", classpath);
    }

    /**
     * Tets long line wrapping
     */
    @Test
    public void testLongLine() throws IOException, ManifestException {
        Project p = buildRule.getProject();
        p.setUserProperty("test.longline", LONG_LINE);
        p.setUserProperty("test.long68name" , LONG_68_NAME);
        p.setUserProperty("test.long70name" , LONG_70_NAME);
        p.setUserProperty("test.notlongname" , NOT_LONG_NAME);
        p.setUserProperty("test.value", VALUE);
        buildRule.executeTarget("testLongLine");

        Manifest manifest = getManifest(expandedManifest);
        Manifest.Section mainSection = manifest.getMainSection();
        String classpath = mainSection.getAttributeValue("class-path");
        assertEquals("Class-Path attribute was not set correctly - ",
            LONG_LINE, classpath);

        String value = mainSection.getAttributeValue(LONG_68_NAME);
        assertEquals("LONG_68_NAME_VALUE_MISMATCH", VALUE, value);
        value = mainSection.getAttributeValue(LONG_70_NAME);
        assertEquals("LONG_70_NAME_VALUE_MISMATCH", VALUE, value);
        value = mainSection.getAttributeValue(NOT_LONG_NAME);
        assertEquals("NOT_LONG_NAME_VALUE_MISMATCH", VALUE, value);

        Set set = new HashSet();
        FileReader fin = new FileReader(expandedManifest);
        try {
            BufferedReader in = new BufferedReader(fin);

            String read = in.readLine();
            while (read != null) {
                set.add(read);
                read = in.readLine();
            }
            in.close();
        } finally {
            fin.close();
        }

        assertTrue("Manifest file should have contained string ", set
                .remove(" NOT_LONG"));
        assertTrue("Manifest file should have contained string ", set
                .remove(" NG"));
        assertTrue("Manifest file should have contained string ", set
                .remove(LONG_70_NAME + ": "));
        assertTrue("Manifest file should have contained string ", set
                .remove(NOT_LONG_NAME + ": NOT_LO"));
    }

    /**
     * Tests ordering of sections
     */
    @Test
    public void testOrder1() throws IOException, ManifestException {
        buildRule.executeTarget("testOrder1");

        Manifest manifest = getManifest(expandedManifest);
        Enumeration e = manifest.getSectionNames();
        String section1 = (String)e.nextElement();
        String section2 = (String)e.nextElement();
        assertEquals("First section name unexpected", "Test1", section1);
        assertEquals("Second section name unexpected", "Test2", section2);

        Manifest.Section section = manifest.getSection("Test1");
        e = section.getAttributeKeys();
        String attr1Key = (String)e.nextElement();
        String attr2Key = (String)e.nextElement();
        String attr1 = section.getAttribute(attr1Key).getName();
        String attr2 = section.getAttribute(attr2Key).getName();
        assertEquals("First attribute name unexpected", "TestAttr1", attr1);
        assertEquals("Second attribute name unexpected", "TestAttr2", attr2);
    }

    /**
     * Tests ordering of sections
     */
    @Test
    public void testOrder2() throws IOException, ManifestException {
        buildRule.executeTarget("testOrder2");

        Manifest manifest = getManifest(expandedManifest);
        Enumeration e = manifest.getSectionNames();
        String section1 = (String)e.nextElement();
        String section2 = (String)e.nextElement();
        assertEquals("First section name unexpected", "Test2", section1);
        assertEquals("Second section name unexpected", "Test1", section2);

        Manifest.Section section = manifest.getSection("Test1");
        e = section.getAttributeKeys();
        String attr1Key = (String)e.nextElement();
        String attr2Key = (String)e.nextElement();
        String attr1 = section.getAttribute(attr1Key).getName();
        String attr2 = section.getAttribute(attr2Key).getName();
        assertEquals("First attribute name unexpected", "TestAttr2", attr1);
        assertEquals("Second attribute name unexpected", "TestAttr1", attr2);
    }

    /**
     * file attribute for manifest task is required.
     */
    @Test
    public void testNoFile() {
        try {
            buildRule.executeTarget("testNoFile");
            fail("BuildException expected: file is required");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    /**
     * replace changes Manifest-Version from 2.0 to 1.0
     */
    @Test
    public void testReplace() throws IOException, ManifestException {
        buildRule.executeTarget("testReplace");
        Manifest mf = getManifest(new File(outDir, "mftest.mf"));
        assertNotNull(mf);
        assertEquals(Manifest.getDefaultManifest(), mf);
    }

    /**
     * update keeps the Manifest-Version and adds a new attribute Foo
     */
    @Test
    public void testUpdate() throws IOException, ManifestException {
        buildRule.executeTarget("testUpdate");
        Manifest mf = getManifest(new File(outDir, "mftest.mf"));
        assertNotNull(mf);
        assertTrue(!Manifest.getDefaultManifest().equals(mf));
        String mfAsString = mf.toString();
        assertNotNull(mfAsString);
        assertTrue(mfAsString.startsWith("Manifest-Version: 2.0"));
        assertTrue(mfAsString.indexOf("Foo: Bar") > -1);

        mf = getManifest(new File(outDir, "mftest2.mf"));
        assertNotNull(mf);
        mfAsString = mf.toString();
        assertNotNull(mfAsString);
        assertEquals(-1, mfAsString.indexOf("Foo: Bar"));
        assertTrue(mfAsString.indexOf("Foo: Baz") > -1);
    }

    @Test
    public void testFrom() {
        buildRule.executeTarget("testFrom");
        assertContains(Manifest.ERROR_FROM_FORBIDDEN, buildRule.getLog());
    }

    @Test
    public void testIllegalName() {
        try {
            buildRule.executeTarget("testIllegalName");
            fail("BuildException expected: Manifest attribute names must not contain ' '");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void testIllegalNameInSection() {
        try {
            buildRule.executeTarget("testIllegalNameInSection");
            fail("BuildException expected: Manifest attribute names must not contain ' '");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void testIllegalNameBegin() {
        try {
            buildRule.executeTarget("testIllegalNameInSection");
            fail("BuildException expected: Manifest attribute names must not start with '-' at the begin.");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void testIllegalName2() {
        try {
            buildRule.executeTarget("testIllegalName");
            fail("BuildException expected: Manifest attribute names must not contain '.'");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void testIllegalName3() {
        try {
            buildRule.executeTarget("testIllegalName");
            fail("BuildException expected: Manifest attribute names must not contain '*'");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    /**
     * Reads mftest.mf.
     */
    private Manifest getManifest(File file) throws IOException, ManifestException {
        FileReader r = new FileReader(file);
        try {
            return new Manifest(r);
        } finally {
            r.close();
        }
    }
}
