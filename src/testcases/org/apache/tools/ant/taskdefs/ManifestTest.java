/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
import java.util.Date;
import org.apache.tools.ant.BuildFileTest;

/**
 * Testcase for the Manifest class used in the jar task. 
 * 
 * @author Conor MacNeill <conor@apache.org>
 */
public class ManifestTest extends BuildFileTest {

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
    public void test1() {
        executeTarget("test1");
    }
    
    /**
     * Simple Manifest with version 2.0
     */
    public void test2() {
        executeTarget("test2");
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
}
