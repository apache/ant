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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildFileTest;

/**
 * BuildFileTest testcases for org.apache.tools.ant.types.XMLCatalog
 *
 * @see org.apache.tools.ant.types.XMLCatalogTest
 *
 */
public class XMLCatalogBuildFileTest extends BuildFileTest {

    public XMLCatalogBuildFileTest(String name) {
        super(name);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    //
    // Ensure that an external entity resolves as expected with NO
    // XMLCatalog involvement:
    //
    // Transform an XML file that refers to the entity into a text
    // file, stuff result into property: val1
    //
    public void testEntityNoCatalog() {
        configureProject("src/etc/testcases/types/xmlcatalog.xml");
        expectPropertySet("testentitynocatalog", "val1",
                          "A stitch in time saves nine");
    }

    //
    // Ensure that an external entity resolves as expected Using an
    // XMLCatalog:
    //
    // Transform an XML file that refers to the entity into a text
    // file, entity is listed in the XMLCatalog pointing to a
    // different file.  Stuff result into property: val2
    //
    public void testEntityWithCatalog() {
        configureProject("src/etc/testcases/types/xmlcatalog.xml");
        expectPropertySet("testentitywithcatalog", "val2",
                          "No news is good news");
    }

    //
    // Ensure that an external entity resolves as expected with NO
    // XMLCatalog involvement:
    //
    // Transform an XML file that contains a reference to a _second_ XML file
    // via the document() function.  The _second_ XML file refers to an entity.
    // Stuff result into the property: val3
    //
    public void testDocumentNoCatalog() {
        configureProject("src/etc/testcases/types/xmlcatalog.xml");
        expectPropertySet("testdocumentnocatalog", "val3",
                          "A stitch in time saves nine");
    }

    //
    // Ensure that an external entity resolves as expected Using an
    // XMLCatalog:
    //
    // Transform an XML file that contains a reference to a _second_ XML file
    // via the document() function.  The _second_ XML file refers to an entity.
    // The entity is listed in the XMLCatalog pointing to a different file.
    // Stuff result into the property: val4
    //
    public void testDocumentWithCatalog() {
        configureProject("src/etc/testcases/types/xmlcatalog.xml");
        expectPropertySet("testdocumentwithcatalog", "val4",
                          "No news is good news");
    }
}
