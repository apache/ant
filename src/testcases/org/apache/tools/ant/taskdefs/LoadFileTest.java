/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "Ant" and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import java.io.File;

/**
 * Test the load file task
 *
 * @author Steve Loughran
 * @author Magesh Umasankar
 * @created 10 December 2001
 */
public class LoadFileTest extends BuildFileTest {

    /**
     * Constructor for the LoadFileTest object
     *
     * @param name Description of Parameter
     */
    public LoadFileTest(String name) {
        super(name);
    }


    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/loadfile.xml");
    }


    /**
     * The teardown method for JUnit
     */
    public void tearDown() {
        executeTarget("cleanup");
    }


    /**
     * A unit test for JUnit
     */
    public void testNoSourcefileDefined() {
        expectBuildException("testNoSourcefileDefined",
                "source file not defined");
    }


    /**
     * A unit test for JUnit
     */
    public void testNoPropertyDefined() {
        expectBuildException("testNoPropertyDefined",
                "output property not defined");
    }


    /**
     * A unit test for JUnit
     */
    public void testNoSourcefilefound() {
        expectBuildExceptionContaining("testNoSourcefilefound",
                "File not found",
                "Unable to load file");
    }

    /**
     * A unit test for JUnit
     */
    public void testFailOnError()
            throws BuildException {
        expectPropertyUnset("testFailOnError","testFailOnError");
    }


    /**
     * A unit test for JUnit
     */
    public void testLoadAFile()
            throws BuildException {
        executeTarget("testLoadAFile");
        if(project.getProperty("testLoadAFile").indexOf("eh?")<0) {
            fail("property is not all in the file");
        }
    }


    /**
     * A unit test for JUnit
     */
    public void testLoadAFileEnc()
            throws BuildException {
        executeTarget("testLoadAFileEnc");
        if(project.getProperty("testLoadAFileEnc")==null) {
            fail("file load failed");
        }
    }

    /**
     * A unit test for JUnit
     */
    public void testEvalProps()
            throws BuildException {
        executeTarget("testEvalProps");
        if(project.getProperty("testEvalProps").indexOf("rain")<0) {
            fail("property eval broken");
        }
    }

    /**
     * Test FilterChain and FilterReaders
     */
    public void testFilterChain()
            throws BuildException {
        executeTarget("testFilterChain");
        if(project.getProperty("testFilterChain").indexOf("World!")<0) {
            fail("Filter Chain broken");
        }
    }

    /**
     * Test StripJavaComments filterreader functionality.
     */
    public final void testStripJavaComments()
            throws BuildException {
        executeTarget("testStripJavaComments");
        final String expected = project.getProperty("expected");
        final String generated = project.getProperty("testStripJavaComments");
        assertEquals(expected, generated);
    }

    /**
     * A unit test for JUnit
     */
    public void testOneLine()
            throws BuildException {
            expectPropertySet("testOneLine","testOneLine","1,2,3,4");

    }
}
