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
package org.apache.tools.ant.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LayoutPreservingPropertiesTest {

    /**
     * Tests that a properties file read by the
     * LayoutPreservingPropertiesFile and then saves the properties in
     * it.
     */
    @Test
    public void testPreserve() throws Exception {
        File simple = new File(System.getProperty("root"),
                               "src/etc/testcases/util/simple.properties");
        FileInputStream fis = new FileInputStream(simple);
        LayoutPreservingProperties lpf = new LayoutPreservingProperties();
        lpf.load(fis);

        File tmp = File.createTempFile("tmp", "props");
        tmp.deleteOnExit();
        lpf.saveAs(tmp);

        // now compare original and tmp for property equivalence
        Properties originalProps = new Properties();
        originalProps.load(new FileInputStream(simple));

        Properties tmpProps = new Properties();
        tmpProps.load(new FileInputStream(tmp));

        assertEquals("properties corrupted", originalProps, tmpProps);

        // and now make sure that the comments made it into the new file
        String s = readFile(tmp);
        assertTrue("missing comment", s.indexOf("# a comment") > -1);
        assertTrue("missing comment", s.indexOf("! more comment") > -1);
    }

    /**
     * Tests that names and value are properly escaped when being
     * written out.
     */
    @Test
    public void testEscaping() throws Exception {
        LayoutPreservingProperties lpf = new LayoutPreservingProperties();

        lpf.setProperty(" prop one ", "  leading and trailing spaces ");
        lpf.setProperty("prop\ttwo", "contains\ttab");
        lpf.setProperty("prop\nthree", "contains\nnewline");
        lpf.setProperty("prop\rfour", "contains\rcarraige return");
        lpf.setProperty("prop\ffive", "contains\fform feed");
        lpf.setProperty("prop\\six", "contains\\backslash");
        lpf.setProperty("prop:seven", "contains:colon");
        lpf.setProperty("prop=eight", "contains=equals");
        lpf.setProperty("prop#nine", "contains#hash");
        lpf.setProperty("prop!ten", "contains!exclamation");

        File tmp = File.createTempFile("tmp", "props");
        tmp.deleteOnExit();
        lpf.saveAs(tmp);

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertTrue(s.indexOf("\\ prop\\ one\\ =\\ \\ leading and trailing"
                             + " spaces ") > -1);
        assertTrue(s.indexOf("prop\\ttwo=contains\\ttab") > -1);
        assertTrue(s.indexOf("prop\\nthree=contains\\nnewline") > -1);
        assertTrue(s.indexOf("prop\\rfour=contains\\rcarraige return") > -1);
        assertTrue(s.indexOf("prop\\\\six=contains\\\\backslash") > -1);
        assertTrue(s.indexOf("prop\\:seven=contains\\:colon") > -1);
        assertTrue(s.indexOf("prop\\=eight=contains\\=equals") > -1);
        assertTrue(s.indexOf("prop\\#nine=contains\\#hash") > -1);
        assertTrue(s.indexOf("prop\\!ten=contains\\!exclamation") > -1);
    }

    /**
     * Tests that properties are correctly indexed, so that when we set
     * an existing property, it updates the logical line, and it doesn't
     * append a new one.
     */
    @Test
    public void testOverwrite() throws Exception {
        File unusual = new File(System.getProperty("root"),
                                "src/etc/testcases/util/unusual.properties");
        FileInputStream fis = new FileInputStream(unusual);
        LayoutPreservingProperties lpf = new LayoutPreservingProperties();
        lpf.load(fis);

        lpf.setProperty(" prop one ", "new one");
        lpf.setProperty("prop\ttwo", "new two");
        lpf.setProperty("prop\nthree", "new three");

        File tmp = File.createTempFile("tmp", "props");
        tmp.deleteOnExit();
        lpf.saveAs(tmp);

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertTrue(s.indexOf("\\ prop\\ one\\ =\\ \\ leading and"
                             + " trailing spaces ") == -1);
        assertTrue(s.indexOf("\\ prop\\ one\\ =new one") > -1);
        assertTrue(s.indexOf("prop\\ttwo=contains\\ttab") == -1);
        assertTrue(s.indexOf("prop\\ttwo=new two") > -1);
        assertTrue(s.indexOf("prop\\nthree=contains\\nnewline") == -1);
        assertTrue(s.indexOf("prop\\nthree=new three") > -1);
    }

    @Test
    public void testStoreWithHeader() throws Exception {
        File simple = new File(System.getProperty("root"),
                               "src/etc/testcases/util/simple.properties");
        FileInputStream fis = new FileInputStream(simple);
        LayoutPreservingProperties lpf = new LayoutPreservingProperties();
        lpf.load(fis);

        File tmp = File.createTempFile("tmp", "props");
        tmp.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tmp);
        lpf.store(fos, "file-header");
        fos.close();

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertTrue("should have had header ", s.startsWith("#file-header"));
    }

    @Test
    public void testClear() throws Exception {
        File simple = new File(System.getProperty("root"),
                               "src/etc/testcases/util/simple.properties");
        FileInputStream fis = new FileInputStream(simple);
        LayoutPreservingProperties lpf = new LayoutPreservingProperties();
        lpf.load(fis);

        lpf.clear();

        File tmp = File.createTempFile("tmp", "props");
        tmp.deleteOnExit();
        lpf.saveAs(tmp);

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertTrue("should have had no properties ",
                   s.indexOf("prop.alpha") == -1);
        assertTrue("should have had no properties ",
                   s.indexOf("prop.beta") == -1);
        assertTrue("should have had no properties ",
                   s.indexOf("prop.gamma") == -1);

        assertTrue("should have had no comments",
                   s.indexOf("# a comment") == -1);
        assertTrue("should have had no comments",
                   s.indexOf("! more comment") == -1);
        assertTrue("should have had no comments",
                   s.indexOf("# now a line wrapping one") == -1);
    }

    @Test
    public void testRemove() throws Exception {
        File simple = new File(System.getProperty("root"),
                               "src/etc/testcases/util/simple.properties");
        FileInputStream fis = new FileInputStream(simple);
        LayoutPreservingProperties lpf = new LayoutPreservingProperties();
        lpf.load(fis);

        lpf.remove("prop.beta");

        File tmp = File.createTempFile("tmp", "props");
        tmp.deleteOnExit();
        lpf.saveAs(tmp);

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertTrue("should not have had prop.beta",
                   s.indexOf("prop.beta") == -1);
        assertTrue("should have had prop.beta's comment",
                   s.indexOf("! more comment") > -1);
    }

    @Test
    public void testRemoveWithComment() throws Exception {
        File simple = new File(System.getProperty("root"),
                               "src/etc/testcases/util/simple.properties");
        FileInputStream fis = new FileInputStream(simple);
        LayoutPreservingProperties lpf = new LayoutPreservingProperties();
        lpf.load(fis);

        lpf.setRemoveComments(true);

        lpf.remove("prop.beta");

        File tmp = File.createTempFile("tmp", "props");
        tmp.deleteOnExit();
        lpf.saveAs(tmp);

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertTrue("should not have had prop.beta",
                   s.indexOf("prop.beta") == -1);
        assertTrue("should not have had prop.beta's comment",
                   s.indexOf("! more comment") == -1);
    }

    @Test
    public void testClone() throws Exception {
        File simple = new File(System.getProperty("root"),
                               "src/etc/testcases/util/simple.properties");
        FileInputStream fis = new FileInputStream(simple);
        LayoutPreservingProperties lpf1 = new LayoutPreservingProperties();
        lpf1.load(fis);

        LayoutPreservingProperties lpf2 =
            (LayoutPreservingProperties) lpf1.clone();

        lpf2.setProperty("prop.new", "a new property");
        lpf2.setProperty("prop.beta", "a new value for beta");

        assertEquals("size of original is wrong", 3, lpf1.size());
        assertEquals("size of clone is wrong", 4, lpf2.size());

        File tmp1 = File.createTempFile("tmp", "props");
        tmp1.deleteOnExit();
        lpf1.saveAs(tmp1);
        String s1 = readFile(tmp1);

        File tmp2 = File.createTempFile("tmp", "props");
        tmp2.deleteOnExit();
        lpf2.saveAs(tmp2);
        String s2 = readFile(tmp2);

        // check original is untouched
        assertTrue("should have had 'simple'", s1.indexOf("simple") > -1);
        assertTrue("should not have had prop.new", s1.indexOf("prop.new") == -1);

        // check clone has the changes
        assertTrue("should have had 'a new value for beta'",
                   s2.indexOf("a new value for beta") > -1);
        assertTrue("should have had prop.new", s2.indexOf("prop.new") > -1);
    }

    @Test
    public void testPreserveEscapeName() throws Exception {
        LayoutPreservingProperties lpf = new LayoutPreservingProperties();
        File unusual = new File(System.getProperty("root"),
                                "src/etc/testcases/util/unusual.properties");
        FileInputStream fis = new FileInputStream(unusual);
        lpf.load(fis);

        lpf.setProperty("prop:seven", "new value for seven");
        lpf.setProperty("prop=eight", "new value for eight");
        lpf.setProperty("prop eleven", "new value for eleven");

        lpf.setProperty("alpha", "new value for alpha");
        lpf.setProperty("beta", "new value for beta");

        File tmp = File.createTempFile("tmp", "props");
        tmp.deleteOnExit();
        lpf.saveAs(tmp);

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertTrue(s.indexOf("prop\\:seven=new value for seven") > -1);
        assertTrue(s.indexOf("prop\\=eight=new value for eight") > -1);
        assertTrue(s.indexOf("prop\\ eleven=new value for eleven") > -1);
        assertTrue(s.indexOf("alpha=new value for alpha") > -1);
        assertTrue(s.indexOf("beta=new value for beta") > -1);

        assertTrue(s.indexOf("prop\\:seven=contains\\:colon") == -1);
        assertTrue(s.indexOf("prop\\=eight=contains\\=equals") == -1);
        assertTrue(s.indexOf("alpha:set with a colon") == -1);
        assertTrue(s.indexOf("beta set with a space") == -1);
    }

    private static String readFile(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        InputStreamReader isr = new InputStreamReader(fis);
        String s = FileUtils.readFully(isr);
        isr.close();
        fis.close();
        return s;
    }
}
