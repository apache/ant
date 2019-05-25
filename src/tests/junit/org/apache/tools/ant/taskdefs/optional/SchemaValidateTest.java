/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.rules.ExpectedException;

/**
 * Test schema validation
 */

public class SchemaValidateTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/schemavalidate.xml");
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
        thrown.expect(BuildException.class);
        thrown.expectMessage(SchemaValidate.SchemaLocation.ERROR_NO_URI);
        buildRule.executeTarget("testNoEmptySchemaNamespace");
    }

    @Test
    public void testNoEmptySchemaLocation() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(SchemaValidate.SchemaLocation.ERROR_NO_LOCATION);
        buildRule.executeTarget("testNoEmptySchemaLocation");
    }

    @Test
    public void testNoFile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(SchemaValidate.SchemaLocation.ERROR_NO_FILE);
        buildRule.executeTarget("testNoFile");
    }

    @Test
    public void testNoDoubleSchemaLocation() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(SchemaValidate.SchemaLocation.ERROR_TWO_LOCATIONS);
        buildRule.executeTarget("testNoDoubleSchemaLocation");
    }

    @Test
    public void testNoDuplicateSchema() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(SchemaValidate.ERROR_DUPLICATE_SCHEMA);
        buildRule.executeTarget("testNoDuplicateSchema");
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
