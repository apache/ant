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

import java.io.*;
import org.apache.tools.ant.BuildFileTest;

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
public class ANTLRTest extends BuildFileTest {

    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/antlr/";

    public ANTLRTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject(TASKDEFS_DIR + "antlr.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void test1() {
        expectBuildException("test1", "required argument, target, missing");
    }

    public void test2() {
        expectBuildException("test2", "Invalid output directory");
    }

    public void test3() {
        executeTarget("test3");
    }

    public void test4() {
        executeTarget("test4");
    }

    public void test5() {
        // should print "panic: Cannot find importVocab file 'JavaTokenTypes.txt'"
        // since it needs to run java.g first before java.tree.g
        expectBuildException("test5", "ANTLR returned: 1");
    }

    public void test6() {
        executeTarget("test6");
    }

    public void test7() {
        expectBuildException("test7", "Unable to determine generated class");
    }

    /**
     * This is a negative test for the super grammar (glib) option.
     */
    public void test8() {
        expectBuildException("test8", "Invalid super grammar file");
    }

    /**
     * This is a positive test for the super grammar (glib) option.  ANTLR
     * will throw an error if everything is not correct.
     */
    public void test9() {
        executeTarget("test9");
    }

    /**
     * This test creates an html-ized version of the calculator grammar.
     * The sanity check is simply whether or not an html file was generated.
     */
    public void test10() {
        executeTarget("test10");
        File outputDirectory = new File(System.getProperty("root"), TASKDEFS_DIR + "antlr.tmp");
        String[] calcFiles = outputDirectory.list(new HTMLFilter());
        assertTrue(calcFiles.length > 0);
    }

    /**
     * This is just a quick sanity check to run the diagnostic option and
     * make sure that it doesn't throw any funny exceptions.
     */
    public void test11() {
        executeTarget("test11");
    }

    /**
     * This is just a quick sanity check to run the trace option and
     * make sure that it doesn't throw any funny exceptions.
     */
    public void test12() {
        executeTarget("test12");
    }

    /**
     * This is just a quick sanity check to run all the rest of the
     * trace options (traceLexer, traceParser, and traceTreeWalker) to
     * make sure that they don't throw any funny exceptions.
     */
    public void test13() {
        executeTarget("test13");
    }

    public void testNoRecompile() {
        executeTarget("test9");
        assertEquals(-1, getFullLog().indexOf("Skipped grammar file."));
        executeTarget("noRecompile");
        assertTrue(-1 != getFullLog().indexOf("Skipped grammar file."));
    }

    public void testNormalRecompile() {
        executeTarget("test9");
        assertEquals(-1, getFullLog().indexOf("Skipped grammar file."));
        executeTarget("normalRecompile");
        assertEquals(-1, getFullLog().indexOf("Skipped grammar file."));
    }

    // Bugzilla Report 12961
    public void testSupergrammarChangeRecompile() {
        executeTarget("test9");
        assertEquals(-1, getFullLog().indexOf("Skipped grammar file."));
        executeTarget("supergrammarChangeRecompile");
        assertEquals(-1, getFullLog().indexOf("Skipped grammar file."));
    }
}

class CalcFileFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        return name.startsWith("Calc");
    }
}

class HTMLFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        return name.endsWith("html");
    }
}
