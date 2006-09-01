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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.taskdefs.optional.dotnet.WsdlToDotnet;

/**
 * Tests the WsdlToDotnet task.
 *
 * @since Ant 1.5
 */
public class WsdlToDotnetTest extends BuildFileTest {

    /**
     * dir for taskdefs
     */
    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";

    /**
     * message from exec
     */
    private static final String WSDL_FAILED = "WSDL returned:";


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
                WsdlToDotnet.ERROR_NO_DEST_FILE);
    }

    /**
     * A unit test for JUnit
     */
    public void testNoSrc() throws Exception {
        expectBuildExceptionContaining("testNoSrc",
                "expected failure",
                WsdlToDotnet.Schema.ERROR_NONE_DECLARED);
    }

    /**
     * A unit test for JUnit
     */
    public void testDestIsDir() throws Exception {
        expectBuildExceptionContaining("testDestIsDir",
                "expected failure",
                WsdlToDotnet.ERROR_DEST_FILE_IS_DIR);
    }

    /**
     * A unit test for JUnit
     */
    public void testBothSrc() throws Exception {
        expectBuildExceptionContaining("testBothSrc",
                "expected failure",
                WsdlToDotnet.Schema.ERROR_BOTH_DECLARED);
    }
     /**
     * A unit test for JUnit
     */
    public void testSrcIsDir() throws Exception {
        expectBuildExceptionContaining("testSrcIsDir",
                "expected failure",
                WsdlToDotnet.Schema.ERROR_FILE_IS_DIR);
    }

    /**
     * A unit test for JUnit
     */
    public void testSrcIsMissing() throws Exception {
        expectBuildExceptionContaining("testSrcIsMissing",
                "expected failure",
                WsdlToDotnet.Schema.ERROR_FILE_NOT_FOUND);
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
                WSDL_FAILED);
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
                WSDL_FAILED);
    }

    /**
     * as if parseable errors were not ignored, mono and WSE1.0 would
     * crash and burn. So here we verify the property exists,
     * and that it is not passed to the app when false
     */
    public void testParseableErrorsIgnoredWhenFalse() throws Exception {
        executeTarget("testLocalWsdl");
    }

    /**
     * A unit test for JUnit
     */
    public void testSchemaFileMustExist() throws Exception {
        expectBuildExceptionContaining("testSchemaFileMustExist",
                "expected failure",
                WsdlToDotnet.Schema.ERROR_FILE_NOT_FOUND);
    }

    /**
     * A unit test for JUnit
     */
    public void testSchemaFileMustHaveOneOptionOnly() throws Exception {
        expectBuildExceptionContaining("testSchemaFileMustHaveOneOptionOnly",
                "expected failure",
                WsdlToDotnet.Schema.ERROR_BOTH_DECLARED);
    }

    /**
     * A unit test for JUnit
     */
    public void testSchemaMustBeSet() throws Exception {
        expectBuildExceptionContaining("testSchemaMustBeSet",
                "expected failure",
                WsdlToDotnet.Schema.ERROR_NONE_DECLARED);
    }


}

