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
package org.apache.tools.ant.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.tools.ant.MagicTestNames;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.apache.tools.ant.util.FileUtils.readFully;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class LayoutPreservingPropertiesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final String ROOT = System.getProperty(MagicTestNames.TEST_ROOT_DIRECTORY);

    private LayoutPreservingProperties lpf;

    private File tmp;

    @Before
    public void setUp() throws IOException {
        lpf = new LayoutPreservingProperties();
        tmp = folder.newFile("tmp.properties");
    }
    /**
     * Tests that a properties file read by the
     * LayoutPreservingPropertiesFile and then saves the properties in
     * it.
     */
    @Test
    public void testPreserve() throws Exception {
        File simple = new File(ROOT, "src/etc/testcases/util/simple.properties");
        FileInputStream fis = new FileInputStream(simple);
        lpf.load(fis);

        lpf.saveAs(tmp);

        // now compare original and tmp for property equivalence
        Properties originalProps = new Properties();
        originalProps.load(new FileInputStream(simple));

        Properties tmpProps = new Properties();
        tmpProps.load(new FileInputStream(tmp));

        assertEquals("properties corrupted", originalProps, tmpProps);

        // and now make sure that the comments made it into the new file
        String s = readFile(tmp);
        assertThat("missing comment", s, containsString(("# a comment")));
        assertThat("missing comment", s, containsString(("! more comment")));
    }

    /**
     * Tests that names and value are properly escaped when being
     * written out.
     */
    @Test
    public void testEscaping() throws Exception {
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

        lpf.saveAs(tmp);

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertThat(s, containsString("\\ prop\\ one\\ =\\ \\ leading and trailing spaces "));
        assertThat(s, containsString("prop\\ttwo=contains\\ttab"));
        assertThat(s, containsString("prop\\nthree=contains\\nnewline"));
        assertThat(s, containsString("prop\\rfour=contains\\rcarriage return"));
        assertThat(s, containsString("prop\\\\six=contains\\\\backslash"));
        assertThat(s, containsString("prop\\:seven=contains\\:colon"));
        assertThat(s, containsString("prop\\=eight=contains\\=equals"));
        assertThat(s, containsString("prop\\#nine=contains\\#hash"));
        assertThat(s, containsString("prop\\!ten=contains\\!exclamation"));
    }

    /**
     * Tests that properties are correctly indexed, so that when we set
     * an existing property, it updates the logical line, and it doesn't
     * append a new one.
     */
    @Test
    public void testOverwrite() throws Exception {
        File unusual = new File(ROOT, "src/etc/testcases/util/unusual.properties");
        FileInputStream fis = new FileInputStream(unusual);
        lpf.load(fis);

        lpf.setProperty(" prop one ", "new one");
        lpf.setProperty("prop\ttwo", "new two");
        lpf.setProperty("prop\nthree", "new three");

        lpf.saveAs(tmp);

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertThat(s, not(containsString("\\ prop\\ one\\ =\\ \\ leading and trailing spaces ")));
        assertThat(s, containsString("\\ prop\\ one\\ =new one"));
        assertThat(s, not(containsString("prop\\ttwo=contains\\ttab")));
        assertThat(s, containsString("prop\\ttwo=new two"));
        assertThat(s, not(containsString("prop\\nthree=contains\\nnewline")));
        assertThat(s, containsString("prop\\nthree=new three"));
    }

    @Test
    public void testStoreWithHeader() throws Exception {
        File simple = new File(ROOT, "src/etc/testcases/util/simple.properties");
        FileInputStream fis = new FileInputStream(simple);
        lpf.load(fis);

        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            lpf.store(fos, "file-header");
        }

        // and check that the resulting file looks okay
        assertThat("should have had header ", readFile(tmp), startsWith("#file-header"));
    }

    @Test
    public void testClear() throws Exception {
        File simple = new File(ROOT, "src/etc/testcases/util/simple.properties");
        FileInputStream fis = new FileInputStream(simple);
        lpf.load(fis);

        lpf.clear();

        lpf.saveAs(tmp);

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertThat("should have had no properties ", s, not(containsString(("prop.alpha"))));
        assertThat("should have had no properties ", s, not(containsString(("prop.beta"))));
        assertThat("should have had no properties ", s, not(containsString(("prop.gamma"))));

        assertThat("should have had no comments", s, not(containsString(("# a comment"))));
        assertThat("should have had no comments", s, not(containsString(("! more comment"))));
        assertThat("should have had no comments", s, not(containsString(("# now a line wrapping one"))));
    }

    @Test
    public void testRemove() throws Exception {
        File simple = new File(ROOT, "src/etc/testcases/util/simple.properties");
        FileInputStream fis = new FileInputStream(simple);
        lpf.load(fis);

        lpf.remove("prop.beta");

        lpf.saveAs(tmp);

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertThat("should not have had prop.beta", s, not(containsString(("prop.beta"))));
        assertThat("should have had prop.beta's comment", s, containsString("! more comment"));
    }

    @Test
    public void testRemoveWithComment() throws Exception {
        File simple = new File(ROOT, "src/etc/testcases/util/simple.properties");
        FileInputStream fis = new FileInputStream(simple);
        lpf.load(fis);

        lpf.setRemoveComments(true);

        lpf.remove("prop.beta");

        lpf.saveAs(tmp);

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertThat("should not have had prop.beta", s, not(containsString(("prop.beta"))));
        assertThat("should not have had prop.beta's comment", s, not(containsString(("! more comment"))));
    }

    @Test
    public void testClone() throws Exception {
        File simple = new File(ROOT, "src/etc/testcases/util/simple.properties");
        FileInputStream fis = new FileInputStream(simple);
        lpf.load(fis);

        LayoutPreservingProperties lpfClone = (LayoutPreservingProperties) lpf.clone();

        lpfClone.setProperty("prop.new", "a new property");
        lpfClone.setProperty("prop.beta", "a new value for beta");

        assertEquals("size of original is wrong", 3, lpf.size());
        assertEquals("size of clone is wrong", 4, lpfClone.size());

        lpf.saveAs(tmp);
        String s1 = readFile(tmp);

        File tmpClone = folder.newFile("tmp-clone.properties");
        lpfClone.saveAs(tmpClone);
        String s2 = readFile(tmpClone);

        // check original is untouched
        assertThat("should have had 'simple'", s1, containsString(("simple")));
        assertThat("should not have had prop.new", s1, not(containsString(("prop.new"))));

        // check clone has the changes
        assertThat("should have had 'a new value for beta'", s2, containsString(("a new value for beta")));
        assertThat("should have had prop.new", s2, containsString(("prop.new")));
    }

    @Test
    public void testPreserveEscapeName() throws Exception {
        File unusual = new File(ROOT, "src/etc/testcases/util/unusual.properties");
        FileInputStream fis = new FileInputStream(unusual);
        lpf.load(fis);

        lpf.setProperty("prop:seven", "new value for seven");
        lpf.setProperty("prop=eight", "new value for eight");
        lpf.setProperty("prop eleven", "new value for eleven");

        lpf.setProperty("alpha", "new value for alpha");
        lpf.setProperty("beta", "new value for beta");

        lpf.saveAs(tmp);

        // and check that the resulting file looks okay
        String s = readFile(tmp);

        assertThat(s, containsString("prop\\:seven=new value for seven"));
        assertThat(s, containsString("prop\\=eight=new value for eight"));
        assertThat(s, containsString("prop\\ eleven=new value for eleven"));
        assertThat(s, containsString("alpha=new value for alpha"));
        assertThat(s, containsString("beta=new value for beta"));

        assertThat(s, not(containsString("prop\\:seven=contains\\:colon")));
        assertThat(s, not(containsString("prop\\=eight=contains\\=equals")));
        assertThat(s, not(containsString("alpha:set with a colon")));
        assertThat(s, not(containsString("beta set with a space")));
    }

    private static String readFile(File f) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(f))) {
            return readFully(isr);
        }
    }
}
