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
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Testcase for the Manifest class used in the jar task.
 *
 */
public class ManifestTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private File expandedManifest;
    private File outDir;

    public static final String LONG_LINE
            = "AReallyLongLineToTestLineBreakingInManifests-ACapabilityWhich"
            + "IsSureToLeadToHundredsOfQuestionsAboutWhyAntMungesManifests"
            + "OfCourseTheAnswerIsThatIsWhatTheSpecRequiresAndIfAnythingHas"
            + "AProblemWithThatItIsNotABugInAnt";

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
        thrown.expect(BuildException.class);
        thrown.expectMessage("Invalid Manifest");
        buildRule.executeTarget("test3");
    }

    /**
     * Malformed manifest - starts with continuation line
     */
    @Test
    public void test4() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Invalid Manifest");
        buildRule.executeTarget("test4");
   }

    /**
     * Malformed manifest - Name attribute in main section
     */
    @Test
    public void test5() {
        buildRule.executeTarget("test5");
        assertThat("Expected warning about Name in main section", buildRule.getLog(),
                containsString("Manifest warning: \"Name\" attributes should not occur in the main section"));
    }

    /**
     * New Section not starting with Name attribute.
     */
    @Test
    public void test6() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Invalid Manifest");
        try {
            buildRule.executeTarget("test6");
        } finally {
            assertThat("Expected warning about section not starting with Name: attribute", buildRule.getLog(),
                    containsString("Manifest sections should start with a \"Name\" attribute"));

        }
    }

    /**
     * From attribute is illegal
     */
    @Test
    public void test7() {
        buildRule.executeTarget("test7");
        assertThat("Expected warning about From: attribute", buildRule.getLog(),
                containsString(Manifest.ERROR_FROM_FORBIDDEN));
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
        thrown.expect(BuildException.class);
        thrown.expectMessage("Specify the section name using the \"name\" attribute of the <section> element");
        buildRule.executeTarget("test9");
    }

    /**
     * Inline manifest - Invalid attribute without name
     */
    @Test
    public void test10() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Attributes must have name and value");
        buildRule.executeTarget("test10");
    }

    /**
     * Inline manifest - Invalid attribute without value
     */
    @Test
    public void test11() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Attributes must have name and value");
        buildRule.executeTarget("test11");
    }

    /**
     * Inline manifest - Invalid attribute without value
     */
    @Test
    public void test12() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Sections must have a name");
        buildRule.executeTarget("test12");
    }

    /**
     * Inline manifest - Duplicate attribute
     */
    @Test
    public void test13() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("The attribute \"Test\" may not occur more than once in the same section");
        buildRule.executeTarget("test13");
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
        p.setUserProperty("test.long68name", LONG_68_NAME);
        p.setUserProperty("test.long70name", LONG_70_NAME);
        p.setUserProperty("test.notlongname", NOT_LONG_NAME);
        p.setUserProperty("test.value", VALUE);
        buildRule.executeTarget("testLongLine");

        Manifest manifest = getManifest(expandedManifest);
        Manifest.Section mainSection = manifest.getMainSection();
        String classpath = mainSection.getAttributeValue("class-path");
        assertEquals("Class-Path attribute was not set correctly - ",
            LONG_LINE, classpath);

        assertEquals("LONG_68_NAME_VALUE_MISMATCH", VALUE,
                mainSection.getAttributeValue(LONG_68_NAME));
        assertEquals("LONG_70_NAME_VALUE_MISMATCH", VALUE,
                mainSection.getAttributeValue(LONG_70_NAME));
        assertEquals("NOT_LONG_NAME_VALUE_MISMATCH", VALUE,
                mainSection.getAttributeValue(NOT_LONG_NAME));

        Set<String> set = new HashSet<>();
        try (FileReader fin = new FileReader(expandedManifest)) {
            BufferedReader in = new BufferedReader(fin);

            String read = in.readLine();
            while (read != null) {
                set.add(read);
                read = in.readLine();
            }
            in.close();
        }

        assertTrue("Manifest file should have contained string ",
                set.remove(" NOT_LONG"));
        assertTrue("Manifest file should have contained string ",
                set.remove(" NG"));
        assertTrue("Manifest file should have contained string ",
                set.remove(LONG_70_NAME + ": "));
        assertTrue("Manifest file should have contained string ",
                set.remove(NOT_LONG_NAME + ": NOT_LO"));
    }

    /**
     * Tests ordering of sections
     */
    @Test
    public void testOrder1() throws IOException, ManifestException {
        buildRule.executeTarget("testOrder1");

        Manifest manifest = getManifest(expandedManifest);
        Enumeration<String> e = manifest.getSectionNames();
        String section1 = e.nextElement();
        String section2 = e.nextElement();
        assertEquals("First section name unexpected", "Test1", section1);
        assertEquals("Second section name unexpected", "Test2", section2);

        Manifest.Section section = manifest.getSection("Test1");
        e = section.getAttributeKeys();
        String attr1Key = e.nextElement();
        String attr2Key = e.nextElement();
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
        Enumeration<String> e = manifest.getSectionNames();
        String section1 = e.nextElement();
        String section2 = e.nextElement();
        assertEquals("First section name unexpected", "Test2", section1);
        assertEquals("Second section name unexpected", "Test1", section2);

        Manifest.Section section = manifest.getSection("Test1");
        e = section.getAttributeKeys();
        String attr1Key = e.nextElement();
        String attr2Key = e.nextElement();
        String attr1 = section.getAttribute(attr1Key).getName();
        String attr2 = section.getAttribute(attr2Key).getName();
        assertEquals("First attribute name unexpected", "TestAttr2", attr1);
        assertEquals("Second attribute name unexpected", "TestAttr1", attr2);
    }

    /**
     * file attribute for manifest task is required.
     */
    @Test(expected = BuildException.class)
    public void testNoFile() {
        buildRule.executeTarget("testNoFile");
        //TODO assert value
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
        assertNotEquals(Manifest.getDefaultManifest(), mf);
        String mfAsString = mf.toString();
        assertNotNull(mfAsString);
        assertThat(mfAsString, startsWith("Manifest-Version: 2.0"));
        assertThat(mfAsString, containsString("Foo: Bar"));

        mf = getManifest(new File(outDir, "mftest2.mf"));
        assertNotNull(mf);
        mfAsString = mf.toString();
        assertNotNull(mfAsString);
        assertThat(mfAsString, containsString("Foo: Baz"));
        assertThat(mfAsString, not(containsString("Foo: Bar")));
    }

    @Test
    public void testFrom() {
        buildRule.executeTarget("testFrom");
        assertThat(buildRule.getLog(), containsString(Manifest.ERROR_FROM_FORBIDDEN));
    }

    /**
     * Expected failure: manifest attribute names must not contain ' '
     */
    @Test(expected = BuildException.class)
    public void testIllegalName() {
        buildRule.executeTarget("testIllegalName");
        // TODO assert value
    }

    /**
     * Expected failure: manifest section names must not contain ' '
     */
    @Test(expected = BuildException.class)
    public void testIllegalNameInSection() {
        buildRule.executeTarget("testIllegalNameInSection");
        // TODO assert value
    }

    /**
     * Expected failure: manifest attribute names must not begin with '-'
     */
    @Test(expected = BuildException.class)
    public void testIllegalNameBegin() {
        buildRule.executeTarget("testIllegalNameInSection");
        // TODO assert value
    }

    /**
     * Expected failure: manifest attribute names must not contain '.'
     */
    @Test(expected = BuildException.class)
    public void testIllegalName2() {
        buildRule.executeTarget("testIllegalName");
        // TODO assert value
    }

    /**
     * Expected failure: manifest attribute names must not contain '*'
     */
    @Test(expected = BuildException.class)
    public void testIllegalName3() {
        buildRule.executeTarget("testIllegalName");
        // TODO assert value
     }

    /**
     * Reads mftest.mf.
     */
    private Manifest getManifest(File file) throws IOException, ManifestException {
        try (FileReader r = new FileReader(file)) {
            return new Manifest(r);
        }
    }
}
