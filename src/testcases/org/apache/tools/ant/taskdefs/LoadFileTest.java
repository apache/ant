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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;

/**
 * Test the load file task
 *
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
                "File not found", " doesn't exist");
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
