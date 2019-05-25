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

import java.util.Locale;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Parameter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests Size Selectors
 *
 */
public class SizeSelectorTest {

    @Rule
    public final BaseSelectorRule selectorRule = new BaseSelectorRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private SizeSelector s;

    private SizeSelector.SizeComparisons less;

    private SizeSelector.SizeComparisons equal;

    private SizeSelector.SizeComparisons more;

    @Before
    public void setUp() {
        s = new SizeSelector();

        less = new SizeSelector.SizeComparisons();
        less.setValue("less");

        equal = new SizeSelector.SizeComparisons();
        equal.setValue("equal");

        more = new SizeSelector.SizeComparisons();
        more.setValue("more");
    }

    /**
     * Test the code that validates the selector.
     */
    @Test
    public void testValidateAttribute() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("The value attribute is required, and must be positive");
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateValue() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("The value attribute is required, and must be positive");
        s.setValue(-10);
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateAttributeName() {
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

    @Test
    public void testValidateAttributeValue() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Invalid size setting garbage out");
        Parameter param = new Parameter();
        param.setName("value");
        param.setValue("garbage out");
        Parameter[] params = {param};
        s.setParameters(params);
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateAttributeUnits() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("garbage out is not a legal value for this attribute");
        Parameter param1 = new Parameter();
        Parameter param2 = new Parameter();
        param1.setName("value");
        param1.setValue("5");
        param2.setName("units");
        param2.setValue("garbage out");
        Parameter[] params = new Parameter[2];
        params[0] = param1;
        params[1] = param2;
        s.setParameters(params);
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    @Test
    public void testSelectionBehaviourLess() {
        s.setValue(10);
        s.setWhen(less);
        assertEquals("TFFFFFFFFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourMore() {
        s.setValue(10);
        s.setWhen(more);
        assertEquals("TTTTTTTTTTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourEqual() {
        s.setValue(32);
        s.setWhen(equal);
        assertEquals("TFFFTFFFFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourKilo() {
        SizeSelector.ByteUnits kilo = new SizeSelector.ByteUnits();
        kilo.setValue("K");
        s.setValue(7);
        s.setWhen(more);
        s.setUnits(kilo);
        assertEquals("TFTFFTTTTTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourKibi() {
        SizeSelector.ByteUnits kibi = new SizeSelector.ByteUnits();
        kibi.setValue("Ki");
        s.setValue(7);
        s.setWhen(more);
        s.setUnits(kibi);
        assertEquals("TFTFFFTTFTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourTibi() {
        SizeSelector.ByteUnits tibi = new SizeSelector.ByteUnits();
        tibi.setValue("Ti");
        s.setValue(99999);
        s.setWhen(more);
        s.setUnits(tibi);
        assertEquals("TFFFFFFFFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviour() {
        Parameter param1 = new Parameter();
        Parameter param2 = new Parameter();
        Parameter param3 = new Parameter();
        param1.setName("value");
        param1.setValue("20");
        param2.setName("units");
        param2.setValue("Ki");
        param3.setName("when");
        param3.setValue("more");
        Parameter[] params = {param1, param2, param3};
        s.setParameters(params);
        assertEquals("TFFFFFFTFFTT", selectorRule.selectionString(s));
    }

    @Test
    public void testParameterParsingLowerCase() {
        testCaseInsensitiveParameterParsing("units");
    }

    @Test
    public void testParameterParsingUpperCase() {
        testCaseInsensitiveParameterParsing("UNITS");
    }

    @Test
    public void testParameterParsingLowerCaseTurkish() {
        Locale l = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr"));
            testCaseInsensitiveParameterParsing("units");
        } finally {
            Locale.setDefault(l);
        }
    }

    @Test
    public void testParameterParsingUpperCaseTurkish() {
        Locale l = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr"));
            testCaseInsensitiveParameterParsing("UNITS");
        } finally {
            Locale.setDefault(l);
        }
    }

    private void testCaseInsensitiveParameterParsing(String name) {
        thrown.expect(BuildException.class);
        thrown.expectMessage("foo is not a legal value for this attribute");
        Parameter p = new Parameter();
        p.setName(name);
        p.setValue("foo");
        s.setParameters(p);
    }
}
