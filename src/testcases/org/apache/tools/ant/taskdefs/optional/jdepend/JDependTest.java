/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.jdepend;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.DirectoryScanner;

/**
 * Testcase for the JDepend optional task.
 *
 * @author Peter Reilly
 */
public class JDependTest extends BuildFileTest {
    public static final String RESULT_FILESET = "result";

    public JDependTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject(
            "src/etc/testcases/taskdefs/optional/jdepend/jdepend.xml");
    }

    /**
     * Test simple
     */
    public void testSimple() {
        expectOutputContaining(
            "simple", "Package: org.apache.tools.ant.util.facade");
    }
    
    /**
     * Test xml
     */
    public void testXml() {
        expectOutputContaining(
            "xml", "<Package>java.lang</Package>");
    }

    /**
     * Test fork
     * - forked output goes to log
     */
    public void testFork() {
        expectLogContaining(
            "fork", "Package: org.apache.tools.ant.util.facade");
    }
    
    /**
     * Test fork xml
     */
    public void testForkXml() {
        expectLogContaining(
            "fork-xml", "<Package>java.lang</Package>");
    }

    /**
     * Test timeout
     */
    public void testTimeout() {
        expectLogContaining(
            "fork-timeout", "JDepend FAILED - Timed out");
    }
    

    /**
     * Test timeout without timing out
     */
    public void testTimeoutNot() {
        expectLogContaining(
            "fork-timeout-not", "Package: org.apache.tools.ant.util.facade");
    }

    /**
     * Assert that the given substring is in the output messages
     */

    protected void assertOutputContaining(String substring) {
        String realOutput = getOutput();
        assertTrue("expecting output to contain \"" + substring + "\" output was \""
                   + realOutput + "\"",
                   realOutput.indexOf(substring) >= 0);
    }
    
    /**
     * Assert that the given message has been outputted
     */
    protected void expectOutputContaining(String target, String substring) {
        executeTarget(target);
        assertOutputContaining(substring);
    }

}
