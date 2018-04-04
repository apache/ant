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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.fail;

/**
 * Test schema validation
 */

public class SchemaValidateTest {

    /**
     * where tasks run
     */
    private static final String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject(TASKDEFS_DIR + "schemavalidate.xml");
    }

    /**
     * test with no namespace
     */
    @Test
    public void testNoNamespace() {
        buildRule.executeTarget("testNoNamespace");
    }

    /**
     * add namespace awareness.
     */
    @Test
    public void testNSMapping() {
        buildRule.executeTarget("testNSMapping");
    }

    @Test
    public void testNoEmptySchemaNamespace() {
        try {
            buildRule.executeTarget("testNoEmptySchemaNamespace");
            fail("Empty namespace URI");
        } catch (BuildException ex) {
            assertContains(SchemaValidate.SchemaLocation.ERROR_NO_URI, ex.getMessage());
        }
    }

    @Test
    public void testNoEmptySchemaLocation() {
        try {
            buildRule.executeTarget("testNoEmptySchemaLocation");
            fail("Empty schema location");
        } catch (BuildException ex) {
            assertContains(SchemaValidate.SchemaLocation.ERROR_NO_LOCATION,
                    ex.getMessage());
        }
    }

    @Test
    public void testNoFile() {
        try {
            buildRule.executeTarget("testNoFile");
            fail("No file at file attribute");
        } catch (BuildException ex) {
            assertContains(SchemaValidate.SchemaLocation.ERROR_NO_FILE,
                    ex.getMessage());
        }
    }

    @Test
    public void testNoDoubleSchemaLocation() {
        try {
            buildRule.executeTarget("testNoDoubleSchemaLocation");
            fail("Two locations for schemas");
        } catch (BuildException ex) {
            assertContains(SchemaValidate.SchemaLocation.ERROR_TWO_LOCATIONS,
                    ex.getMessage());
        }
    }

    @Test
    public void testNoDuplicateSchema() {
        try {
            buildRule.executeTarget("testNoDuplicateSchema");
            fail("duplicate schemas with different values");
        } catch (BuildException ex) {
            assertContains(SchemaValidate.ERROR_DUPLICATE_SCHEMA,
                    ex.getMessage());
        }
    }

    @Test
    public void testEqualsSchemasOK() {
        buildRule.executeTarget("testEqualsSchemasOK");
    }

    @Test
    public void testFileset() {
        buildRule.executeTarget("testFileset");
    }
}
