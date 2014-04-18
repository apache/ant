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

import static org.junit.Assert.fail;

/**
 * Tests the {@link org.apache.tools.ant.taskdefs.XSLTProcess} task.
 * TODO merge with {@link org.apache.tools.ant.taskdefs.StyleTest}?
 * @since Ant 1.5
 */
public class XsltTest {

    /**
     * where tasks run
     */
    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject(TASKDEFS_DIR + "xslt.xml");
    }


    @Test
    public void testCatchNoDtd() {
        try {
            buildRule.executeTarget("testCatchNoDtd");
            fail("Expected failure");
        } catch(BuildException ex) {
            //TODO assert exception message
        }
    }

    @Test
    public void testCatalog() throws Exception {
         buildRule.executeTarget("testCatalog");
    }

    @Test
    public void testOutputProperty() throws Exception {
      buildRule.executeTarget("testOutputProperty");
    }

    @Test
    public void testXMLWithEntitiesInNonAsciiPath() throws Exception {
        buildRule.executeTarget("testXMLWithEntitiesInNonAsciiPath");
    }

    /**
     * check that the system id gets set properly on stylesheets.
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testStyleSheetWithInclude() throws Exception {
        buildRule.executeTarget("testStyleSheetWithInclude");
        if (buildRule.getLog().indexOf("java.io.FileNotFoundException") != -1) {
            fail("xsl:include was not found");
        }
    }
}

