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

package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

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

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/antlr/antlr.xml");
    }

    /**
     * Expected failure due to missing required argument, target
     */
    @Test(expected = BuildException.class)
    public void test1() {
        buildRule.executeTarget("test1");
        // TODO Check exception message
    }

    /**
     * Expected failure due to invalid output directory
     */
    @Test(expected = BuildException.class)
    public void test2() {
        buildRule.executeTarget("test2");
        // TODO Check exception message
     }

    @Test
    public void test3() {
        buildRule.executeTarget("test3");
    }

    @Test
    public void test4() {
        buildRule.executeTarget("test4");
    }

    /**
     * should print "panic: Cannot find importVocab file 'JavaTokenTypes.txt'"
     * since it needs to run java.g first before java.tree.g
     */
    @Test(expected = BuildException.class)
    public void test5() {
        buildRule.executeTarget("test5");
        // TODO Check exception message
    }

    @Test
    public void test6() {
        buildRule.executeTarget("test6");
    }

    /**
     * Expected failure due to inability to determine generated class
     */
    @Test(expected = BuildException.class)
    public void test7() {
        buildRule.executeTarget("test7");
        // TODO Check exception message
    }

    /**
     * Expected failure due to invalid super grammar (glib) option.
     */
    @Test(expected = BuildException.class)
    public void test8() {
        buildRule.executeTarget("test8");
        // TODO Check exception message
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
        assertNotEquals(outputDirectory.list(new HTMLFilter()).length, 0);
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
        assertThat(buildRule.getFullLog(), not(containsString("Skipped grammar file.")));
        buildRule.executeTarget("noRecompile");
        assertThat(buildRule.getFullLog(), containsString("Skipped grammar file."));
    }

    @Test
    public void testNormalRecompile() {
        buildRule.executeTarget("test9");
        assertThat(buildRule.getFullLog(), not(containsString("Skipped grammar file.")));

        FileUtilities.rollbackTimestamps(buildRule.getOutputDir(), 5);

        buildRule.executeTarget("normalRecompile");
        assertThat(buildRule.getFullLog(), not(containsString("Skipped grammar file.")));
    }

    @Test
    // Bugzilla Report 12961
    public void testSupergrammarChangeRecompile() {
        buildRule.executeTarget("test9");
        assertThat(buildRule.getFullLog(), not(containsString("Skipped grammar file.")));

        FileUtilities.rollbackTimestamps(buildRule.getOutputDir(), 5);

        buildRule.executeTarget("supergrammarChangeRecompile");
        assertThat(buildRule.getFullLog(), not(containsString("Skipped grammar file.")));

    }

    class HTMLFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name.endsWith("html");
        }
    }

}
