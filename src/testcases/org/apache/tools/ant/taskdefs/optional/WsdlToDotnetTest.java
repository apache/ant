/*
 * Copyright  2002,2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
import java.util.Properties;

import org.apache.tools.ant.BuildFileTest;

/**
 * Tests the WsdlToDotnet task.
 *
 * @author steve loughran
 * @since Ant 1.5
 */
public class WsdlToDotnetTest extends BuildFileTest {

    /**
     * Description of the Field
     */
    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";


    /**
     * Constructor
     *
     * @param name testname
     */
    public WsdlToDotnetTest(String name) {
        super(name);
    }


    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject(TASKDEFS_DIR + "WsdlToDotnet.xml");
    }


    /**
     * The teardown method for JUnit
     */
    public void tearDown() {
        executeTarget("teardown");
    }



    /**
     * A unit test for JUnit
     */
    public void testNoParams() throws Exception {
        expectBuildExceptionContaining("testNoParams",
                "expected failure",
                "destination file must be specified");
    }

    /**
     * A unit test for JUnit
     */
    public void testNoSrc() throws Exception {
        expectBuildExceptionContaining("testNoSrc",
                "expected failure",
                "you must specify either a source file or a URL");
    }

    /**
     * A unit test for JUnit
     */
    public void testDestIsDir() throws Exception {
        expectBuildExceptionContaining("testDestIsDir",
                "expected failure",
                "is a directory");
    }

    /**
     * A unit test for JUnit
     */
    public void testBothSrc() throws Exception {
        expectBuildExceptionContaining("testBothSrc",
                "expected failure",
                "both a source file and a URL");
    }
     /**
     * A unit test for JUnit
     */
    public void testSrcIsDir() throws Exception {
        expectBuildExceptionContaining("testSrcIsDir",
                "expected failure",
                "is a directory");
    }

    /**
     * A unit test for JUnit
     */
    public void testSrcIsMissing() throws Exception {
        expectBuildExceptionContaining("testSrcIsMissing",
                "expected failure",
                "does not exist");
    }

    /**
     * A unit test for JUnit
     */
    public void testLocalWsdl() throws Exception {
        executeTarget("testLocalWsdl");
    }
    /**
     * A unit test for JUnit
     */
    public void testLocalWsdlServer() throws Exception {
        executeTarget("testLocalWsdlServer");
    }
     /**
     * A unit test for JUnit
     */
    public void testInvalidExtraOps() throws Exception {
        expectBuildExceptionContaining("testInvalidExtraOps",
                "expected failure",
                "WSDL returned: 1");
    }

    /**
     * A unit test for JUnit
     */
    public void testLocalWsdlVB() throws Exception {
        executeTarget("testLocalWsdlVB");
    }
    /**
     * A unit test for JUnit
     */
    public void testLocalWsdlServerVB() throws Exception {
        executeTarget("testLocalWsdlServerVB");
    }
     /**
     * A unit test for JUnit
     */
    public void testInvalidExtraOpsVB() throws Exception {
        expectBuildExceptionContaining("testInvalidExtraOpsVB",
                "expected failure",
                "WSDL returned: 1");
    }
}

