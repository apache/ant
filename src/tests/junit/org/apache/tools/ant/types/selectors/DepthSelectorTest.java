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
 * Tests Depth Selectors
 *
 */
public class DepthSelectorTest {

    @Rule
    public final BaseSelectorRule selectorRule = new BaseSelectorRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private DepthSelector s;

    @Before
    public void setUp() {
        s = new DepthSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    @Test
    public void testValidateRequiredFields() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must set at least one of the min or the max levels.");
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateDepth() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("The maximum depth is lower than the minimum.");
        s.setMin(5);
        s.setMax(2);
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateParameterName() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Invalid parameter garbage in");
        Parameter param = new Parameter();
        param.setName("garbage in");
        param.setValue("garbage out");
        Parameter[] params = new Parameter[1];
        params[0] = param;
        s.setParameters(params);
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateMinValue() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Invalid minimum value garbage out");
        Parameter param = new Parameter();
        param.setName("min");
        param.setValue("garbage out");
        Parameter[] params = new Parameter[1];
        params[0] = param;
        s.setParameters(params);
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateMaxValue() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Invalid maximum value garbage out");
        Parameter param = new Parameter();
        param.setName("max");
        param.setValue("garbage out");
        Parameter[] params = new Parameter[1];
        params[0] = param;
        s.setParameters(params);
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    @Test
    public void testSelectionBehaviourMinMax() {
        s.setMin(20);
        s.setMax(25);
        assertEquals("FFFFFFFFFFFF", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourMin0() {
        s.setMin(0);
        assertEquals("TTTTTTTTTTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourMin1() {
        s.setMin(1);
        assertEquals("FFFFFTTTTTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourMax0() {
        s.setMax(0);
        assertEquals("TTTTTFFFFFFF", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourMin1Max1() {
        s.setMin(1);
        s.setMax(1);
        assertEquals("FFFFFTTTFFFT", selectorRule.selectionString(s));
    }

}
