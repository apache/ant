/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional;

import java.io.*;
import org.apache.tools.ant.BuildFileTest;
/**
 * If you want to run tests, it is highly recommended
 * to download ANTLR (www.antlr.org), build the 'all' jar
 * with the mkalljar script and drop the jar (about 300KB) into
 * Ant lib.
 * - Running w/ the default antlr.jar (70KB) does not work (missing class)
 * - Running w/ the antlr jar made w/ mkjar (88KB) does not work (still another class missing)
 *
 * Unless of course you specify the ANTLR classpath in your
 * system classpath. (see ANTLR install.html)
 *
 * @author Erik Meade <emeade@geekfarm.org>
 * @author Stephen Chin <aphid@browsecode.org>
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
        File outputDirectory = new File(TASKDEFS_DIR + "antlr.tmp");
        String[] calcFiles = outputDirectory.list(new CalcFileFilter());
        assertEquals(5, calcFiles.length);
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
        File outputDirectory = new File(TASKDEFS_DIR + "antlr.tmp");
        String[] calcFiles = outputDirectory.list(new HTMLFilter());
        assertEquals(1, calcFiles.length);
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
