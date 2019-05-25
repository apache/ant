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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Tests the {@link org.apache.tools.ant.taskdefs.XSLTProcess} task.
 * TODO merge with {@link org.apache.tools.ant.taskdefs.StyleTest}?
 * @since Ant 1.5
 */
public class XsltTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/xslt.xml");
    }

    /**
     * Expected failure due to lacking DTD
     */
    @Test(expected = BuildException.class)
    public void testCatchNoDtd() {
        buildRule.executeTarget("testCatchNoDtd");
        // TODO assert exception message
    }

    @Test
    public void testCatalog() {
         buildRule.executeTarget("testCatalog");
    }

    @Test
    public void testOutputProperty() {
      buildRule.executeTarget("testOutputProperty");
    }

    @Test
    public void testXMLWithEntitiesInNonAsciiPath() {
        buildRule.executeTarget("testXMLWithEntitiesInNonAsciiPath");
    }

    /**
     * check that the system id gets set properly on stylesheets.
     */
    @Test
    public void testStyleSheetWithInclude() {
        buildRule.executeTarget("testStyleSheetWithInclude");
        assertThat("xsl:include was not found", buildRule.getLog(),
                not(containsString("java.io.FileNotFoundException")));
    }
}
