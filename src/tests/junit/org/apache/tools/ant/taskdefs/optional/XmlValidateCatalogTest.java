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

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the XMLValidate optional task with nested external catalogs.
 *
 * @see XmlValidateTest
 * @since Ant 1.6
 */
public class XmlValidateCatalogTest {

    /**
     * where tasks run
     */
    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject(TASKDEFS_DIR + "xmlvalidate.xml");
    }


    /**
     * catalogfiles fileset should be ignored
     * if resolver.jar is not present, but will
     * be used if it is.  either way, test should
     * work b/c we have a nested dtd with the same
     * entity
     */
    @Test
    public void testXmlCatalogFiles() {
        buildRule.executeTarget("xmlcatalogfiles");
    }

    /**
     * Test nested catalogpath.
     * It should be ignored if resolver.jar is not
     * present, but will be used if it is.  either
     * way, test should work b/c we have a nested
     * dtd with the same entity
     */
    @Test
    public void testXmlCatalogPath() {
        buildRule.executeTarget("xmlcatalogpath");
    }

}
