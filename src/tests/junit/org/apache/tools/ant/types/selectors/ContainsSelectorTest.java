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

package org.apache.tools.ant.types.selectors;

import static org.junit.Assert.assertEquals;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Parameter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests Contains Selectors.
 *
 */
public class ContainsSelectorTest {

    @Rule
    public final BaseSelectorRule selectorRule = new BaseSelectorRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ContainsSelector s;

    @Before
    public void setUp() {
        s = new ContainsSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    @Test
    public void testValidateMissingParameter() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("The text attribute is required");
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateInvalidParameter() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Invalid parameter garbage in");
        Parameter param = new Parameter();
        param.setName("garbage in");
        param.setValue("garbage out");
        Parameter[] params = {param};
        s.setParameters(params);
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    @Test
    public void testSelectionBehaviourNoSuch() {
        s.setText("no such string in test files");
        assertEquals("TFFFFFFFFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourCase() {
        s.setText("Apache Ant");
        assertEquals("TFFFTFFFFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourCaseSensitive() {
        s.setText("apache ant");
        s.setCasesensitive(true);
        assertEquals("TFFFFFFFFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourCaseInsensitive() {
        s.setText("apache ant");
        s.setCasesensitive(false);
        assertEquals("TFFFTFFFFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourNoWhitespace() {
        s.setText("ApacheAnt");
        s.setIgnorewhitespace(true);
        assertEquals("TFFFTFFFFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourIgnoreWhitespace() {
        s.setText("A p a c h e    A n t");
        s.setIgnorewhitespace(true);
        assertEquals("TFFFTFFFFFFT", selectorRule.selectionString(s));
    }

}
