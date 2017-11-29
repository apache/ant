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
package org.apache.tools.ant.types.optional;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.fail;

/**
 * Test that scripting selection works. Needs scripting support to work
 */
public class ScriptSelectorTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/selectors/scriptselector.xml");
    }

    @Test
    public void testNolanguage() {
        try {
            buildRule.executeTarget("testNolanguage");
            fail("Absence of language attribute not detected");
        } catch(BuildException ex) {
            assertContains("script language must be specified", ex.getMessage());

        }
    }

    @Test
    public void testSelectionSetByDefault() {
        buildRule.executeTarget("testSelectionSetByDefault");
    }

    @Test
    public void testSelectionSetWorks() {
        buildRule.executeTarget("testSelectionSetWorks");
    }

    @Test
    public void testSelectionClearWorks() {
        buildRule.executeTarget("testSelectionClearWorks");
    }

    @Test
    public void testFilenameAttribute() {
        buildRule.executeTarget("testFilenameAttribute");
    }

    @Test
    public void testFileAttribute() {
        buildRule.executeTarget("testFileAttribute");
    }

    @Test
    public void testBasedirAttribute() {
        buildRule.executeTarget("testBasedirAttribute");
    }

}
