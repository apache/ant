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
 * Tests the XMLValidate optional task with nested external catalogs.
 *
 * @see XmlValidateTest
 * @since Ant 1.6
 */
public class XmlValidateCatalogTest extends BuildFileTest {

    /**
     * where tasks run
     */
    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";


    /**
     * Constructor
     *
     * @param name testname
     */
    public XmlValidateCatalogTest(String name) {
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
    public void tearDown() {

    }

    /**
     * catalogfiles fileset should be ignored
     * if resolver.jar is not present, but will
     * be used if it is.  either way, test should
     * work b/c we have a nested dtd with the same
     * entity
     */
    public void testXmlCatalogFiles() {
        executeTarget("xmlcatalogfiles");
    }

    /**
     * Test nested catalogpath.
     * It should be ignored if resolver.jar is not
     * present, but will be used if it is.  either
     * way, test should work b/c we have a nested
     * dtd with the same entity
     */
    public void testXmlCatalogPath() {
        executeTarget("xmlcatalogpath");
    }

}
