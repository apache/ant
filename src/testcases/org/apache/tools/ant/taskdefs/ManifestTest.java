/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;

/**
 * Testcase for the Manifest class used in the jar task. 
 * 
 * @author Conor MacNeill
 */
public class ManifestTest extends BuildFileTest {

    public static final String EXPANDED_MANIFEST
        = "src/etc/testcases/taskdefs/manifests/META-INF/MANIFEST.MF";

    public static final String LONG_LINE
        = "AReallyLongLineToTestLineBreakingInManifests-ACapabilityWhich" + 
          "IsSureToLeadToHundredsOfQuestionsAboutWhyAntMungesManifests" +
          "OfCourseTheAnswerIsThatIsWhatTheSpecRequiresAndIfAnythingHas" +
          "AProblemWithThatItIsNotABugInAnt";
        
    public ManifestTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/manifest.xml");
    }

    public void tearDown() {
        executeTarget("clean");
    }

    /**
     * Empty manifest - is OK 
     */
    public void test1() throws ManifestException, IOException {
        executeTarget("test1");
        Manifest manifest = getManifest(EXPANDED_MANIFEST);
        String version = manifest.getManifestVersion();
        assertEquals("Manifest was not created with correct version - ", "1.0", version);
    }
    
    /**
     * Simple Manifest with version 2.0
     */
    public void test2() throws ManifestException, IOException {
        executeTarget("test2");
        Manifest manifest = getManifest(EXPANDED_MANIFEST);
        String version = manifest.getManifestVersion();
        assertEquals("Manifest was not created with correct version - ", "2.0", version);
    }
    
    /**
     * Malformed manifest - no : on the line
     */
    public void test3() {
        expectBuildExceptionContaining("test3", "Manifest is invalid - no colon on header line",
                                       "Invalid Manifest");
    }

    /**
     * Malformed manifest - starts with continuation line
     */
    public void test4() {
        expectBuildExceptionContaining("test4", "Manifest is invalid - section starts with continuation line",
                                       "Invalid Manifest");
   }

    /**
     * Malformed manifest - Name attribute in main section
     */
    public void test5() {
        executeTarget("test5");
        String output = getLog();
        boolean hasWarning = output.indexOf("Manifest warning: \"Name\" attributes should not occur in the main section") != -1;
        assertEquals("Expected warning about Name in main section", true, hasWarning);
    }
    
    /**
     * New Section not starting with Name attribute.
     */
    public void test6() {
        expectBuildExceptionContaining("test6", "Manifest is invalid - section starts with incorrect attribute",
                                       "Invalid Manifest");
        String output = getLog();
        boolean hasWarning = output.indexOf("Manifest sections should start with a \"Name\" attribute") != -1;
        assertEquals("Expected warning about section not starting with Name: attribute", true, hasWarning);
    }
     
    /**
     * From attribute is illegal
     */
    public void test7() {
        executeTarget("test7");

        boolean hasWarning = getLog().indexOf("Manifest attributes should not start with \"From\"") != -1;
        assertEquals("Expected warning about From: attribute", true, hasWarning);
    }

    /**
     * Inline manifest - OK
     */
    public void test8() throws IOException, ManifestException {
        executeTarget("test8");
        Manifest manifest = getManifest(EXPANDED_MANIFEST);
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
    public void test9() {
        expectBuildExceptionContaining("test9", "Construction is invalid - Name attribute should not be used",
                                       "Specify the section name using the \"name\" attribute of the <section> element");
    }
     
    /**
     * Inline manifest - Invalid attribute without name
     */
    public void test10() {
        expectBuildExceptionContaining("test10", "Attribute has no name",
                                       "Attributes must have name and value");
    }
     
    /**
     * Inline manifest - Invalid attribute without value
     */
    public void test11() {
        expectBuildExceptionContaining("test11", "Attribute has no value",
                                       "Attributes must have name and value");
    }
     
    /**
     * Inline manifest - Invalid attribute without value
     */
    public void test12() {
        expectBuildExceptionContaining("test12", "Section with no name",
                                       "Sections must have a name");
    }
     
    /**
     * Inline manifest - Duplicate attribute
     */
    public void test13() {
        expectBuildExceptionContaining("test13", "Duplicate Attribute",
                                       "The attribute \"Test\" may not occur more than once in the same section");
    }
     
    /**
     * Inline manifest - OK since classpath entries can be duplicated.
     */
    public void test14() throws IOException, ManifestException {
        executeTarget("test14");
        Manifest manifest = getManifest(EXPANDED_MANIFEST);
        Manifest.Section mainSection = manifest.getMainSection();
        String classpath = mainSection.getAttributeValue("class-path");
        assertEquals("Class-Path attribute was not set correctly - ", 
            "Test1 Test2 Test3 Test4", classpath);
    }
     
    /**
     * Tets long line wrapping
     */
    public void testLongLine() throws IOException, ManifestException {
        Project project = getProject();
        project.setUserProperty("test.longline", LONG_LINE);
        executeTarget("testLongLine");

        Manifest manifest = getManifest(EXPANDED_MANIFEST);
        Manifest.Section mainSection = manifest.getMainSection();
        String classpath = mainSection.getAttributeValue("class-path");
        assertEquals("Class-Path attribute was not set correctly - ", 
            LONG_LINE, classpath);
    }
     
    /**
     * Tests ordering of sections
     */
    public void testOrder1() throws IOException, ManifestException {
        executeTarget("testOrder1");

        Manifest manifest = getManifest(EXPANDED_MANIFEST);
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
    public void testOrder2() throws IOException, ManifestException {
        executeTarget("testOrder2");

        Manifest manifest = getManifest(EXPANDED_MANIFEST);
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
    public void testNoFile() {
        expectBuildException("testNoFile", "file is required");
    }
    
    /**
     * replace changes Manifest-Version from 2.0 to 1.0
     */
    public void testReplace() throws IOException, ManifestException {
        executeTarget("testReplace");
        Manifest mf = getManifest("src/etc/testcases/taskdefs/mftest.mf");
        assertNotNull(mf);
        assertEquals(Manifest.getDefaultManifest(), mf);
    }

    /**
     * update keeps the Manifest-Version and adds a new attribute Foo
     */
    public void testUpdate() throws IOException, ManifestException {
        executeTarget("testUpdate");
        Manifest mf = getManifest("src/etc/testcases/taskdefs/mftest.mf");
        assertNotNull(mf);
        assertTrue(!Manifest.getDefaultManifest().equals(mf));
        String mfAsString = mf.toString();
        assertNotNull(mfAsString);
        assertTrue(mfAsString.startsWith("Manifest-Version: 2.0"));
        assertTrue(mfAsString.indexOf("Foo: Bar") > -1);

        mf = getManifest("src/etc/testcases/taskdefs/mftest2.mf");
        assertNotNull(mf);
        mfAsString = mf.toString();
        assertNotNull(mfAsString);
        assertEquals(-1, mfAsString.indexOf("Foo: Bar"));
        assertTrue(mfAsString.indexOf("Foo: Baz") > -1);
    }

    /**
     * Reads mftest.mf.
     */
    private Manifest getManifest(String filename) throws IOException, ManifestException {
        FileReader r = new FileReader(filename);
        try {
            return new Manifest(r);
        } finally {
            r.close();
        }
    }
}
