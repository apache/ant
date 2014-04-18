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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.apache.tools.ant.AntAssert.assertNotContains;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * If you want to run tests, it is highly recommended
 * to download ANTLR (www.antlr.org), build the 'antlrall.jar' jar
 * with  <code>make antlr-all.jar</code> and drop the jar (about 300KB) into
 * Ant lib.
 * - Running w/ the default antlr.jar (70KB) does not work (missing class)
 *
 * Unless of course you specify the ANTLR classpath in your
 * system classpath. (see ANTLR install.html)
 *
 */
public class ANTLRTest {

    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/antlr/";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject(TASKDEFS_DIR + "antlr.xml");
    }

    @Test
    public void test1() {
        try {
            buildRule.executeTarget("test1");
            fail("required argument, target, missing");
        } catch (BuildException ex) {
            //TODO should check exception message
        }
    }

    @Test
    public void test2() {
        try {
            buildRule.executeTarget("test2");
            fail("Invalid output directory");
        } catch (BuildException ex) {
            //TODO should check exception message
        }
    }

    @Test
    public void test3() {
        buildRule.executeTarget("test3");
    }

    @Test
    public void test4() {
        buildRule.executeTarget("test4");
    }

    @Test
    public void test5() {
        // should print "panic: Cannot find importVocab file 'JavaTokenTypes.txt'"
        // since it needs to run java.g first before java.tree.g
        try {
            buildRule.executeTarget("test5");
            fail("ANTLR returned: 1");
        } catch (BuildException ex) {
            //TODO should check exception message
        }
    }

    @Test
    public void test6() {
        buildRule.executeTarget("test6");
    }

    @Test
    public void test7() {
        try {
            buildRule.executeTarget("test7");
            fail("Unable to determine generated class");
        } catch (BuildException ex) {
            //TODO should check exception message
        }
    }

    /**
     * This is a negative test for the super grammar (glib) option.
     */
    @Test
    public void test8() {
        try {
            buildRule.executeTarget("test8");
            fail("Invalid super grammar file");
        } catch (BuildException ex) {
            //TODO should check exception message
        }
    }

    /**
     * This is a positive test for the super grammar (glib) option.  ANTLR
     * will throw an error if everything is not correct.
     */
    @Test
    public void test9() {
        buildRule.executeTarget("test9");
    }

    /**
     * This test creates an html-ized version of the calculator grammar.
     * The sanity check is simply whether or not an html file was generated.
     */
    @Test
    public void test10() {
        buildRule.executeTarget("test10");
        File outputDirectory = new File(buildRule.getProject().getProperty("output"));
        String[] calcFiles = outputDirectory.list(new HTMLFilter());
        assertTrue(calcFiles.length > 0);
    }

    /**
     * This is just a quick sanity check to run the diagnostic option and
     * make sure that it doesn't throw any funny exceptions.
     */
    @Test
    public void test11() {
        buildRule.executeTarget("test11");
    }

    /**
     * This is just a quick sanity check to run the trace option and
     * make sure that it doesn't throw any funny exceptions.
     */
    @Test
    public void test12() {
        buildRule.executeTarget("test12");
    }

    /**
     * This is just a quick sanity check to run all the rest of the
     * trace options (traceLexer, traceParser, and traceTreeWalker) to
     * make sure that they don't throw any funny exceptions.
     */
    @Test
    public void test13() {
        buildRule.executeTarget("test13");
    }

    @Test
    public void testNoRecompile() {
        buildRule.executeTarget("test9");
        assertNotContains("Skipped grammar file.", buildRule.getFullLog());
        buildRule.executeTarget("noRecompile");
        assertContains("Skipped grammar file.", buildRule.getFullLog());
    }

    @Test
    public void testNormalRecompile() {
        buildRule.executeTarget("test9");
        assertNotContains("Skipped grammar file.", buildRule.getFullLog());

        FileUtilities.rollbackTimetamps(buildRule.getOutputDir(), 5);

        buildRule.executeTarget("normalRecompile");
        assertNotContains("Skipped grammar file.", buildRule.getFullLog());
    }

    @Test
    // Bugzilla Report 12961
    public void testSupergrammarChangeRecompile() {
        buildRule.executeTarget("test9");
        assertNotContains("Skipped grammar file.", buildRule.getFullLog());

        FileUtilities.rollbackTimetamps(buildRule.getOutputDir(), 5);

        buildRule.executeTarget("supergrammarChangeRecompile");
        assertNotContains("Skipped grammar file.", buildRule.getFullLog());

    }

}

class HTMLFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        return name.endsWith("html");
    }
}
