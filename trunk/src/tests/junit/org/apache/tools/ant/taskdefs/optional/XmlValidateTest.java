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
import org.apache.tools.ant.BuildFileTest;

/**
 * Tests the XMLValidate optional task, by running targets in the test script
 * <code>src/etc/testcases/taskdefs/optional/xmlvalidate.xml</code>
 * <p>
 *
 * @see XmlValidateCatalogTest
 * @since Ant 1.5
 */
public class XmlValidateTest extends BuildFileTest {

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
    public XmlValidateTest(String name) {
        super(name);
    }

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject(TASKDEFS_DIR + "xmlvalidate.xml");
    }

    /**
     * The teardown method for JUnit
     */
    public void tearDown() {}

    /**
     * Basic inline 'dtd' element test.
     */
    public void testValidate() throws Exception {
        executeTarget("testValidate");
    }

    /**
     * Test indirect validation.
     */
    public void testDeepValidate() throws Exception {
        executeTarget("testDeepValidate");
    }

    /**
     *
     */
    public void testXmlCatalog() {
        executeTarget("xmlcatalog");
    }

    /**
     *
     */
    public void testXmlCatalogViaRefid() {
        executeTarget("xmlcatalogViaRefid");
    }

    /**
     * Test that the nested dtd element is used when resolver.jar is not
     * present.  This test should pass either way.
     */
    public void testXmlCatalogFiles() {
        executeTarget("xmlcatalogfiles-override");
    }

    /**
     * Test nested catalogpath.
     * Test that the nested dtd element is used when resolver.jar is not
     * present.  This test should pass either way.
     */
    public void testXmlCatalogPath() {
        executeTarget("xmlcatalogpath-override");
    }

    /**
     * Test nested xmlcatalog definitions
     */
    public void testXmlCatalogNested() {
        executeTarget("xmlcatalognested");
    }

    /**
     * Test xml schema validation
     */
    public void testXmlSchemaGood() throws BuildException {
        try {
            executeTarget("testSchemaGood");
        } catch (BuildException e) {
            if (e
                .getMessage()
                .endsWith(" doesn't recognize feature http://apache.org/xml/features/validation/schema")
                || e.getMessage().endsWith(
                    " doesn't support feature http://apache.org/xml/features/validation/schema")) {
                System.err.println(" skipped, parser doesn't support schema");
            } else {
                throw e;
            }
        }
    }
    /**
     * Test xml schema validation
     */
    public void testXmlSchemaBad() {
        try {
            executeTarget("testSchemaBad");
            fail("Should throw BuildException because 'Bad Schema Validation'");

            expectBuildExceptionContaining(
                "testSchemaBad",
                "Bad Schema Validation",
                "not a valid XML document");
        } catch (BuildException e) {
            if (e
                .getMessage()
                .endsWith(" doesn't recognize feature http://apache.org/xml/features/validation/schema")
                || e.getMessage().endsWith(
                    " doesn't support feature http://apache.org/xml/features/validation/schema")) {
                System.err.println(" skipped, parser doesn't support schema");
            } else {
                assertTrue(
                    e.getMessage().indexOf("not a valid XML document") > -1);
            }
        }
    }

    /**
     * iso-2022-jp.xml is valid but wouldn't get recognized on systems
     * with a different native encoding.
     *
     * Bug 11279
     */
    public void testIso2022Jp() {
        executeTarget("testIso2022Jp");
    }

    /**
     * utf-8.xml is invalid as it contains non-UTF-8 characters, but
     * would pass on systems with a native iso-8859-1 (or similar)
     * encoding.
     *
     * Bug 11279
     */
    public void testUtf8() {
        expectBuildException("testUtf8", "invalid characters in file");
    }

    // Tests property element, using XML schema properties as an example.

    public void testPropertySchemaForValidXML() {
        executeTarget("testProperty.validXML");
    }

    public void testPropertySchemaForInvalidXML() {
        expectBuildException(
            "testProperty.invalidXML",
            "XML file does not satisfy schema.");
    }

}
