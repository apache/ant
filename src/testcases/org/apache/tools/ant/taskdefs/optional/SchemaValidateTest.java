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

/**
 * Test schema validation
 */

public class SchemaValidateTest extends BuildFileTest {

    /**
     * where tasks run
     */
    private final static String TASKDEFS_DIR =
            "src/etc/testcases/taskdefs/optional/";

    /**
     * Constructor
     *
     * @param name testname
     */
    public SchemaValidateTest(String name) {
        super(name);
    }

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject(TASKDEFS_DIR + "schemavalidate.xml");
    }

    /**
     * test with no namespace
     */
    public void testNoNamespace() throws Exception {
        executeTarget("testNoNamespace");
    }

    /**
     * add namespace awareness.
     */
    public void testNSMapping() throws Exception {
        executeTarget("testNSMapping");
    }

    public void testNoEmptySchemaNamespace() throws Exception {
        expectBuildExceptionContaining("testNoEmptySchemaNamespace",
                "empty namespace URI",SchemaValidate.SchemaLocation.ERROR_NO_URI);
    }

    public void testNoEmptySchemaLocation() throws Exception {
        expectBuildExceptionContaining("testNoEmptySchemaLocation",
                "empty schema location",
                SchemaValidate.SchemaLocation.ERROR_NO_LOCATION);
    }

    public void testNoFile() throws Exception {
        expectBuildExceptionContaining("testNoFile",
                "no file at file attribute",
                SchemaValidate.SchemaLocation.ERROR_NO_FILE);
    }

    public void testNoDoubleSchemaLocation() throws Exception {
        expectBuildExceptionContaining("testNoDoubleSchemaLocation",
                "two locations for schemas",
                SchemaValidate.SchemaLocation.ERROR_TWO_LOCATIONS);
    }
    public void testNoDuplicateSchema() throws Exception {
        expectBuildExceptionContaining("testNoDuplicateSchema",
                "duplicate schemas with different values",
                SchemaValidate.ERROR_DUPLICATE_SCHEMA);
    }

    public void testEqualsSchemasOK() throws Exception {
        executeTarget("testEqualsSchemasOK");
    }

    public void testFileset() throws Exception {
        executeTarget("testFileset");
    }
}
