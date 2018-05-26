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
import java.io.IOException;
import java.io.InputStreamReader;
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
        assertTrue("missing comment", s.contains("# a comment"));
        assertTrue("missing comment", s.contains("! more comment"));
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
        lpf.setProperty("prop\rfour", "contains\rcarriage return");
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

        assertTrue(s.contains("\\ prop\\ one\\ =\\ \\ leading and trailing spaces "));
        assertTrue(s.contains("prop\\ttwo=contains\\ttab"));
        assertTrue(s.contains("prop\\nthree=contains\\nnewline"));
        assertTrue(s.contains("prop\\rfour=contains\\rcarriage return"));
        assertTrue(s.contains("prop\\\\six=contains\\\\backslash"));
        assertTrue(s.contains("prop\\:seven=contains\\:colon"));
        assertTrue(s.contains("prop\\=eight=contains\\=equals"));
        assertTrue(s.contains("prop\\#nine=contains\\#hash"));
        assertTrue(s.contains("prop\\!ten=contains\\!exclamation"));
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

        assertTrue(!s.contains("\\ prop\\ one\\ =\\ \\ leading and trailing spaces "));
        assertTrue(s.contains("\\ prop\\ one\\ =new one"));
        assertTrue(!s.contains("prop\\ttwo=contains\\ttab"));
        assertTrue(s.contains("prop\\ttwo=new two"));
        assertTrue(!s.contains("prop\\nthree=contains\\nnewline"));
        assertTrue(s.contains("prop\\nthree=new three"));
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

        assertTrue("should have had no properties ", !s.contains("prop.alpha"));
        assertTrue("should have had no properties ", !s.contains("prop.beta"));
        assertTrue("should have had no properties ", !s.contains("prop.gamma"));

        assertTrue("should have had no comments", !s.contains("# a comment"));
        assertTrue("should have had no comments", !s.contains("! more comment"));
        assertTrue("should have had no comments", !s.contains("# now a line wrapping one"));
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

        assertTrue("should not have had prop.beta", !s.contains("prop.beta"));
        assertTrue("should have had prop.beta's comment", s.contains("! more comment"));
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

        assertTrue("should not have had prop.beta", !s.contains("prop.beta"));
        assertTrue("should not have had prop.beta's comment", !s.contains("! more comment"));
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
        assertTrue("should have had 'simple'", s1.contains("simple"));
        assertTrue("should not have had prop.new", !s1.contains("prop.new"));

        // check clone has the changes
        assertTrue("should have had 'a new value for beta'",
                s2.contains("a new value for beta"));
        assertTrue("should have had prop.new", s2.contains("prop.new"));
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

        assertTrue(s.contains("prop\\:seven=new value for seven"));
        assertTrue(s.contains("prop\\=eight=new value for eight"));
        assertTrue(s.contains("prop\\ eleven=new value for eleven"));
        assertTrue(s.contains("alpha=new value for alpha"));
        assertTrue(s.contains("beta=new value for beta"));

        assertTrue(!s.contains("prop\\:seven=contains\\:colon"));
        assertTrue(!s.contains("prop\\=eight=contains\\=equals"));
        assertTrue(!s.contains("alpha:set with a colon"));
        assertTrue(!s.contains("beta set with a space"));
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
