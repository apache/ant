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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Parameter;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Tests Date Selectors.
 *
 */
public class DateSelectorTest {

    @Rule
    public final BaseSelectorRule selectorRule = new BaseSelectorRule();


    /**
     * Test the code that validates the selector.
     */
    @Test
    public void testValidate() {
        DateSelector s = new DateSelector();
        try {
            s.isSelected(selectorRule.getProject().getBaseDir(),selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("DateSelector did not check for required fields");
        } catch (BuildException be1) {
            assertEquals("You must provide a datetime or the number of "
                    + "milliseconds.", be1.getMessage());
        }

        s = new DateSelector();
        s.setDatetime("01/01/1969 01:01 AM");
        try {
            s.isSelected(selectorRule.getProject().getBaseDir(),selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("DateSelector did not check for Datetime being in the "
                    + "allowable range");
        } catch (BuildException be2) {
            assertEquals("Date of 01/01/1969 01:01 AM results in negative "
                    + "milliseconds value relative to epoch (January 1, "
                    + "1970, 00:00:00 GMT).", be2.getMessage());
        }

        s = new DateSelector();
        s.setDatetime("this is not a date");
        try {
            s.isSelected(selectorRule.getProject().getBaseDir(),selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("DateSelector did not check for Datetime being in a "
                    + "valid format");
        } catch (BuildException be3) {
            assertEquals("Date of this is not a date"
                        + " Cannot be parsed correctly. It should be in"
                        + " MM/DD/YYYY HH:MM AM_PM format.", be3.getMessage());
        }

        s = new DateSelector();
        Parameter param = new Parameter();
        param.setName("garbage in");
        param.setValue("garbage out");
        Parameter[] params = new Parameter[1];
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(selectorRule.getProject().getBaseDir(),selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("DateSelector did not check for valid parameter element");
        } catch (BuildException be4) {
            assertEquals("Invalid parameter garbage in", be4.getMessage());
        }

        s = new DateSelector();
        param = new Parameter();
        param.setName("millis");
        param.setValue("garbage out");
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(selectorRule.getProject().getBaseDir(),selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("DateSelector did not check for valid millis parameter");
        } catch (BuildException be5) {
            assertEquals("Invalid millisecond setting garbage out",
                    be5.getMessage());
        }

        s = new DateSelector();
        param = new Parameter();
        param.setName("granularity");
        param.setValue("garbage out");
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(selectorRule.getProject().getBaseDir(),selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("DateSelector did not check for valid granularity parameter");
        } catch (BuildException be6) {
            assertEquals("Invalid granularity setting garbage out",
                    be6.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    @Test
    public void testSelectionBehaviour() {
        DateSelector s;
        String results;

        DateSelector.TimeComparisons before = new
                DateSelector.TimeComparisons();
        before.setValue("before");
        DateSelector.TimeComparisons equal = new
                DateSelector.TimeComparisons();
        equal.setValue("equal");
        DateSelector.TimeComparisons after = new
                DateSelector.TimeComparisons();
        after.setValue("after");


        s = new DateSelector();
        s.setDatetime("10/10/1999 1:45 PM");
        s.setWhen(before);
        results = selectorRule.selectionString(s);
        assertEquals("TFFFFFFFFFFT", results);

        s = new DateSelector();
        s.setDatetime("10/10/1999 1:45 PM");
        s.setWhen(before);
        s.setCheckdirs(true);
        results = selectorRule.selectionString(s);
        assertEquals("FFFFFFFFFFFF", results);

        s = new DateSelector();
        s.setDatetime("10/10/1999 1:45 PM");
        s.setWhen(after);
        results = selectorRule.selectionString(s);
        assertEquals("TTTTTTTTTTTT", results);

        s = new DateSelector();
        s.setDatetime("11/21/2001 4:54 AM");
        s.setWhen(before);
        results = selectorRule.selectionString(s);
        assertEquals("TFTFFFFFFFFT", results);

        s = new DateSelector();
        s.setDatetime("11/21/2001 4:55 AM");

        long milliseconds = s.getMillis();
        s.setWhen(equal);
        results = selectorRule.selectionString(s);
        assertEquals("TTFFTFFFTTTT", results);

        s = new DateSelector();
        s.setMillis(milliseconds);
        s.setWhen(equal);
        results = selectorRule.selectionString(s);
        assertEquals("TTFFTFFFTTTT", results);

        s = new DateSelector();
        s.setDatetime("11/21/2001 4:56 AM");
        s.setWhen(after);
        results = selectorRule.selectionString(s);
        assertEquals("TFFTFTTTFFFT", results);

        s = new DateSelector();
        Parameter param1 = new Parameter();
        Parameter param2 = new Parameter();
        param1.setName("datetime");
        param1.setValue("11/21/2001 4:56 AM");
        param2.setName("when");
        param2.setValue("after");
        Parameter[] params = {param1,param2};
        s.setParameters(params);
        results = selectorRule.selectionString(s);
        assertEquals("TFFTFTTTFFFT", results);

        s = new DateSelector();
        long testtime = selectorRule.getMirrorFiles()[5].lastModified();
        s.setMillis(testtime);
        s.setWhen(after);
        s.setGranularity(2);

        // setup the modified timestamp to match what the test needs, although be aware that the 3rd and 4th
        // files don't exist so can't be changed, so don't try and loop over them
        for (int i = 1; i <=2; i++) {
            Assume.assumeTrue("Cannot setup file times for test", selectorRule.getMirrorFiles()[i].setLastModified(testtime - (3*60*60*100)));
        }


        results = selectorRule.mirrorSelectionString(s);
        assertEquals("TFFFFTTTTTTT", results);

        s = new DateSelector();
        testtime = selectorRule.getMirrorFiles()[6].lastModified();
        s.setMillis(testtime);
        s.setWhen(before);
        s.setGranularity(2);
        for (int i = 7; i <= 10; i++) {
            Assume.assumeTrue("Cannot setup file times for test", selectorRule.getMirrorFiles()[i].setLastModified(testtime + (3*60*60*100)));
        }

        results = selectorRule.mirrorSelectionString(s);
        assertEquals("TTTTTTTFFFFT", results);

    }

}
