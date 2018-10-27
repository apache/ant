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

package org.apache.tools.ant.types.selectors;

import java.util.Locale;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Parameter;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests Size Selectors
 *
 */
public class SizeSelectorTest {
    
    @Rule
    public final BaseSelectorRule selectorRule = new BaseSelectorRule();

    /**
     * Test the code that validates the selector.
     */
    @Test
    public void testValidate() {
        SizeSelector s = new SizeSelector();
        try {
            s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("SizeSelector did not check for required fields");
        } catch (BuildException be1) {
            assertEquals("The value attribute is required, and must "
                    + "be positive", be1.getMessage());
        }

        s = new SizeSelector();
        s.setValue(-10);
        try {
            s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("SizeSelector did not check for value being in the "
                    + "allowable range");
        } catch (BuildException be2) {
            assertEquals("The value attribute is required, and must "
                    + "be positive", be2.getMessage());
        }

        s = new SizeSelector();
        Parameter param = new Parameter();
        param.setName("garbage in");
        param.setValue("garbage out");
        Parameter[] params = {param};
        s.setParameters(params);
        try {
            s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("SizeSelector did not check for valid parameter element");
        } catch (BuildException be3) {
            assertEquals("Invalid parameter garbage in", be3.getMessage());
        }

        s = new SizeSelector();
        param = new Parameter();
        param.setName("value");
        param.setValue("garbage out");
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("SizeSelector accepted bad value as parameter");
        } catch (BuildException be4) {
            assertEquals("Invalid size setting garbage out",
                    be4.getMessage());
        }

        s = new SizeSelector();
        Parameter param1 = new Parameter();
        Parameter param2 = new Parameter();
        param1.setName("value");
        param1.setValue("5");
        param2.setName("units");
        param2.setValue("garbage out");
        params = new Parameter[2];
        params[0] = param1;
        params[1] = param2;
        try {
            s.setParameters(params);
            s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("SizeSelector accepted bad units as parameter");
        } catch (BuildException be5) {
            assertEquals("garbage out is not a legal value for this attribute",
                    be5.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    @Test
    public void testSelectionBehaviour() {
        SizeSelector s;
        String results;

        SizeSelector.ByteUnits kilo = new SizeSelector.ByteUnits();
        kilo.setValue("K");
        SizeSelector.ByteUnits kibi = new SizeSelector.ByteUnits();
        kibi.setValue("Ki");
        SizeSelector.ByteUnits tibi = new SizeSelector.ByteUnits();
        tibi.setValue("Ti");
        SizeSelector.SizeComparisons less = new SizeSelector.SizeComparisons();
        less.setValue("less");
        SizeSelector.SizeComparisons equal = new SizeSelector.SizeComparisons();
        equal.setValue("equal");
        SizeSelector.SizeComparisons more = new SizeSelector.SizeComparisons();
        more.setValue("more");


    
        s = new SizeSelector();
        s.setValue(10);
        s.setWhen(less);
        results = selectorRule.selectionString(s);
        assertEquals("TFFFFFFFFFFT", results);

        s = new SizeSelector();
        s.setValue(10);
        s.setWhen(more);
        results = selectorRule.selectionString(s);
        assertEquals("TTTTTTTTTTTT", results);

        s = new SizeSelector();
        s.setValue(32);
        s.setWhen(equal);
        results = selectorRule.selectionString(s);
        assertEquals("TFFFTFFFFFFT", results);

        s = new SizeSelector();
        s.setValue(7);
        s.setWhen(more);
        s.setUnits(kilo);
        results = selectorRule.selectionString(s);
        assertEquals("TFTFFTTTTTTT", results);

        s = new SizeSelector();
        s.setValue(7);
        s.setWhen(more);
        s.setUnits(kibi);
        results = selectorRule.selectionString(s);
        assertEquals("TFTFFFTTFTTT", results);

        s = new SizeSelector();
        s.setValue(99999);
        s.setWhen(more);
        s.setUnits(tibi);
        results = selectorRule.selectionString(s);
        assertEquals("TFFFFFFFFFFT", results);

        s = new SizeSelector();
        Parameter param1 = new Parameter();
        Parameter param2 = new Parameter();
        Parameter param3 = new Parameter();
        param1.setName("value");
        param1.setValue("20");
        param2.setName("units");
        param2.setValue("Ki");
        param3.setName("when");
        param3.setValue("more");
        Parameter[] params = {param1,param2,param3};
        s.setParameters(params);
        results = selectorRule.selectionString(s);
        assertEquals("TFFFFFFTFFTT", results);
    

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
        SizeSelector s = new SizeSelector();
        Parameter p = new Parameter();
        p.setName(name);
        p.setValue("foo");
        try {
            s.setParameters(new Parameter[] {p});
            fail("should have caused an exception");
        } catch (BuildException be) {
            assertEquals("foo is not a legal value for this attribute",
                         be.getMessage());
        }
    }
}
