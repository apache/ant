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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests Filename Selectors
 *
 */
public class FilenameSelectorTest {

    @Rule
    public final BaseSelectorRule selectorRule = new BaseSelectorRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private FilenameSelector s;

    @Before
    public void setUp() {
        s = new FilenameSelector();
    }

    /**
     * Test the code that validates the selector: required attribute.
     */
    @Test
    public void testRequired() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("The name or regex attribute is required");
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    /**
     * Test the code that validates the selector: invalid parameter.
     */
    @Test
    public void testValidate() {
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
    public void testSelectionBehaviour() {
        s.setName("no match possible");
        assertEquals("FFFFFFFFFFFF", selectorRule.selectionString(s));
    }

    @Ignore("Turned off due to a bug: SelectorUtils.matchPattern() is recursive without '**' on Windows")
    @Test
    public void testSelectionBehaviourWildcard() {
        s.setName("*.gz");
        assertEquals("FFFTFFFFFFFF", selectorRule.selectionString(s)); // Unix
        assertEquals("FFFTFFFFTFFF", selectorRule.selectionString(s)); // Windows
    }

    @Test
    public void testSelectionBehaviourNegate() {
        s.setName("**/*.gz");
        s.setNegate(true);
        assertEquals("TTTFTTTFFTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourCaseInsensitive() {
        s.setName("**/*.GZ");
        s.setCasesensitive(false);
        assertEquals("FFFTFFFTTFFF", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourNamedParameter() {
        Parameter param1 = new Parameter();
        param1.setName("name");
        param1.setValue("**/*.bz2");
        Parameter[] params = {param1};
        s.setParameters(params);
        assertEquals("FFTFFFFFFTTF", selectorRule.selectionString(s));
    }

}
