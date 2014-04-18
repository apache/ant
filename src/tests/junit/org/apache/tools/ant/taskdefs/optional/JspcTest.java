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
package org.apache.tools.ant.taskdefs.optional;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.taskdefs.optional.jsp.Jasper41Mangler;
import org.apache.tools.ant.taskdefs.optional.jsp.JspMangler;
import org.apache.tools.ant.taskdefs.optional.jsp.JspNameMangler;
import org.apache.tools.ant.taskdefs.optional.jsp.compilers.JspCompilerAdapter;
import org.apache.tools.ant.taskdefs.optional.jsp.compilers.JspCompilerAdapterFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the Jspc task.
 *
 * created 07 March 2002
 * @since Ant 1.5
 */
public class JspcTest {

    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";


    @Rule
    public BuildFileRule buildRule = new BuildFileRule();
    
    @Before
    public void setUp() {
        buildRule.configureProject(TASKDEFS_DIR + "jspc.xml");
     }



    @Test
    public void testSimple() {
        executeJspCompile("testSimple", "simple_jsp.java");
    }


    @Test
    public void testUriroot() throws Exception {
        executeJspCompile("testUriroot", "uriroot_jsp.java");
    }


    @Test
    public void testXml() throws Exception {
        executeJspCompile("testXml", "xml_jsp.java");
    }


    /**
     * try a keyword in a file
     */
    @Test
    public void testKeyword() throws Exception {
        executeJspCompile("testKeyword", "default_jsp.java");
    }


    /**
     * what happens to 1nvalid-classname
     */
    @Test
    public void testInvalidClassname() throws Exception {
        executeJspCompile("testInvalidClassname",
                "_1nvalid_0002dclassname_jsp.java");
    }


    @Test
    public void testNoTld() throws Exception {
        try {
            buildRule.executeTarget("testNoTld");
            fail("Not found");
        } catch (BuildException ex) {
            assertEquals("Java returned: 9", ex.getMessage());
        }
    }


    @Test
    public void testNotAJspFile()  throws Exception {
        buildRule.executeTarget("testNotAJspFile");
    }

    /**
     * webapp test is currently broken, because it picks up
     * on the missing_tld file, and bails.
     */
    @Ignore("picks up on the missing_tld file, and incorrectly bails")
    @Test
    public void testWebapp()  throws Exception {
        buildRule.executeTarget("testWebapp");
    }

    /**
     * run a target then verify the named file gets created
     *
     * @param target Description of Parameter
     * @param javafile Description of Parameter
     */
    protected void executeJspCompile(String target, String javafile) {
        buildRule.executeTarget(target);
        assertJavaFileCreated(javafile);
    }


    /**
     * verify that a named file was created
     *
     * @param filename Description of Parameter
     */
    protected void assertJavaFileCreated(String filename) {
        File file = getOutputFile(filename);
        assertTrue("file " + filename + " not found", file.exists());
        assertTrue("file " + filename + " is empty", file.length() > 0);
    }

    /**
     * Gets the OutputFile attribute of the JspcTest object
     *
     * @param subpath Description of Parameter
     * @return The OutputFile value
     */
    protected File getOutputFile(String subpath) {
        return new File(buildRule.getProject().getProperty("output"), subpath);
    }

    /**
     * verify that we select the appropriate mangler
     */
    @Test
    public void testJasperNameManglerSelection() {
        JspCompilerAdapter adapter=
                JspCompilerAdapterFactory.getCompiler("jasper", null,null);
        JspMangler mangler=adapter.createMangler();
        assertTrue(mangler instanceof JspNameMangler);
        adapter= JspCompilerAdapterFactory.getCompiler("jasper41", null, null);
        mangler = adapter.createMangler();
        assertTrue(mangler instanceof Jasper41Mangler);
    }

    @Test
    public void testJasper41() {
        JspMangler mangler = new Jasper41Mangler();
        //java keywords are not special
        assertMapped(mangler, "for.jsp", "for_jsp");
        //underscores go in front of invalid start chars
        assertMapped(mangler, "0.jsp", "_0_jsp");
        //underscores at the front get an underscore too
        assertMapped(mangler, "_.jsp", "___jsp");
        //non java char at start => underscore then the the _hex value
        assertMapped(mangler, "-.jsp", "__0002d_jsp");
        //and paths are stripped
        char s = File.separatorChar;
        assertMapped(mangler, "" + s + s + "somewhere" + s + "file" + s + "index.jsp", "index_jsp");
    }

    /**
     * assert our mapping rules
     * @param mangler
     * @param filename
     * @param classname
     */
    protected void assertMapped(JspMangler mangler, String filename, String classname) {
        String mappedname = mangler.mapJspToJavaName(new File(filename));
        assertTrue(filename+" should have mapped to "+classname
                    +" but instead mapped to "+mappedname,
                    classname.equals(mappedname));
    }


}

