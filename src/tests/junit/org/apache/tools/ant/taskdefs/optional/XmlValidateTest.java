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
 * Tests the XMLValidate optional task, by running targets in the test script
 * <code>src/etc/testcases/taskdefs/optional/xmlvalidate.xml</code>
 * <p>
 *
 * @see XmlValidateCatalogTest
 * @since Ant 1.5
 */
public class XmlValidateTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/xmlvalidate.xml");
    }

    /**
     * Basic inline 'dtd' element test.
     */
    @Test
    public void testValidate() {
        buildRule.executeTarget("testValidate");
    }

    /**
     * Test indirect validation.
     */
    @Test
    public void testDeepValidate() {
        buildRule.executeTarget("testDeepValidate");
    }

    @Test
    public void testXmlCatalog() {
        buildRule.executeTarget("xmlcatalog");
    }

    @Test
    public void testXmlCatalogViaRefid() {
        buildRule.executeTarget("xmlcatalogViaRefid");
    }

    /**
     * Test that the nested dtd element is used when resolver.jar is not
     * present.  This test should pass either way.
     */
    @Test
    public void testXmlCatalogFiles() {
        buildRule.executeTarget("xmlcatalogfiles-override");
    }

    /**
     * Test nested catalogpath.
     * Test that the nested dtd element is used when resolver.jar is not
     * present.  This test should pass either way.
     */
    @Test
    public void testXmlCatalogPath() {
        buildRule.executeTarget("xmlcatalogpath-override");
    }

    /**
     * Test nested xmlcatalog definitions
     */
    @Test
    public void testXmlCatalogNested() {
        buildRule.executeTarget("xmlcatalognested");
    }

    /**
     * Test xml schema validation,
     * implicitly assume schema-supporting parser (Java 5+)
     */
    @Test
    public void testXmlSchemaGood() throws BuildException {
        buildRule.executeTarget("testSchemaGood");
    }

    /**
     * Test xml schema validation,
     * implicitly assume schema-supporting parser (Java 5+)
     */
    @Test
    public void testXmlSchemaBad() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("not a valid XML document");
        buildRule.executeTarget("testSchemaBad");
    }

    /**
     * iso-2022-jp.xml is valid but wouldn't get recognized on systems
     * with a different native encoding.
     *
     * Bug 11279
     */
    @Test
    public void testIso2022Jp() {
        buildRule.executeTarget("testIso2022Jp");
    }

    /**
     * utf-8.xml is invalid as it contains non-UTF-8 characters, but
     * would pass on systems with a native iso-8859-1 (or similar)
     * encoding.
     *
     * Bug 11279
     */
    @Test(expected = BuildException.class)
    public void testUtf8() {
        buildRule.executeTarget("testUtf8");
        //TODO assert exception message
    }

    /**
     * Tests property element, using XML schema properties as an example.
     */
    @Test
    public void testPropertySchemaForValidXML() {
        buildRule.executeTarget("testProperty.validXML");
    }

    /**
     * Test should fail due to unsatisfied schema
     */
    @Test(expected = BuildException.class)
    public void testPropertySchemaForInvalidXML() {
        buildRule.executeTarget("testProperty.invalidXML");
        //TODO assert exception message
    }

}
