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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Parameter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * Tests Date Selectors.
 *
 */
public class DateSelectorTest {

    @Rule
    public final BaseSelectorRule selectorRule = new BaseSelectorRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private DateSelector s;

    private DateSelector.TimeComparisons before;

    private DateSelector.TimeComparisons equal;

    private DateSelector.TimeComparisons after;

    @Before
    public void setUp() {
        s = new DateSelector();

        before = new DateSelector.TimeComparisons();
        before.setValue("before");

        equal = new DateSelector.TimeComparisons();
        equal.setValue("equal");

        after = new DateSelector.TimeComparisons();
        after.setValue("after");
    }

    /**
     * Test the code that validates the selector.
     */
    @Test
    public void testValidateFields() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must provide a datetime or the number of milliseconds.");
        s.isSelected(selectorRule.getProject().getBaseDir(),
                selectorRule.getFilenames()[0], selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateRange() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Date of 01/01/1969 01:01 AM results in negative milliseconds value"
                + " relative to epoch (January 1, 1970, 00:00:00 GMT).");
        s.setDatetime("01/01/1969 01:01 AM");
        s.isSelected(selectorRule.getProject().getBaseDir(),
                selectorRule.getFilenames()[0], selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateFormat() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Date of this is not a date Cannot be parsed correctly."
                + " It should be in 'MM/dd/yyyy hh:mm a' format.");
        s.setDatetime("this is not a date");
        s.isSelected(selectorRule.getProject().getBaseDir(),
                selectorRule.getFilenames()[0], selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateParameterIn() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Invalid parameter garbage in");
        Parameter param = new Parameter();
        param.setName("garbage in");
        param.setValue("garbage out");
        Parameter[] params = new Parameter[1];
        params[0] = param;
        s.setParameters(params);
        s.isSelected(selectorRule.getProject().getBaseDir(),
                selectorRule.getFilenames()[0], selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateParameterMillis() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Invalid millisecond setting garbage out");
        Parameter param = new Parameter();
        param.setName("millis");
        param.setValue("garbage out");
        Parameter[] params = new Parameter[1];
        params[0] = param;
        s.setParameters(params);
        s.isSelected(selectorRule.getProject().getBaseDir(),
                selectorRule.getFilenames()[0], selectorRule.getFiles()[0]);
    }

    @Test
    public void testValidateParameterGranularity() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Invalid granularity setting garbage out");
        Parameter param = new Parameter();
        param.setName("granularity");
        param.setValue("garbage out");
        Parameter[] params = new Parameter[1];
        params[0] = param;
        s.setParameters(params);
        s.isSelected(selectorRule.getProject().getBaseDir(),
                selectorRule.getFilenames()[0], selectorRule.getFiles()[0]);
    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    @Test
    public void testSelectionBehaviourBefore() {
        s.setDatetime("10/10/1999 1:45 PM");
        s.setWhen(before);
        assertEquals("TFFFFFFFFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourBeforeCheckdirs() {
        s.setDatetime("10/10/1999 1:45 PM");
        s.setWhen(before);
        s.setCheckdirs(true);
        assertEquals("FFFFFFFFFFFF", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourAfter() {
        s.setDatetime("10/10/1999 1:45 PM");
        s.setWhen(after);
        assertEquals("TTTTTTTTTTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourBefore2K() {
        s.setDatetime("11/21/2001 4:54 AM");
        s.setWhen(before);
        assertEquals("TFTFFFFFFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourEqual() {
        s.setDatetime("11/21/2001 4:55 AM");
        s.setWhen(equal);
        assertEquals("TTFFTFFFTTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourEqualMillis() {
        s.setDatetime("11/21/2001 4:55 AM");
        s.setMillis(s.getMillis());
        s.setWhen(equal);
        assertEquals("TTFFTFFFTTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourAfter2K() {
        s.setDatetime("11/21/2001 4:56 AM");
        s.setWhen(after);
        assertEquals("TFFTFTTTFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourAfterNamed() {
        Parameter param1 = new Parameter();
        Parameter param2 = new Parameter();
        param1.setName("datetime");
        param1.setValue("11/21/2001 4:56 AM");
        param2.setName("when");
        param2.setValue("after");
        Parameter[] params = {param1, param2};
        s.setParameters(params);
        assertEquals("TFFTFTTTFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourAfterWithGranularity() {
        long testtime = selectorRule.getMirrorFiles()[5].lastModified();
        s.setMillis(testtime);
        s.setWhen(after);
        s.setGranularity(2);

        // setup the modified timestamp to match what the test needs, although be aware that
        // the 3rd and 4th files don't exist so can't be changed, so don't try and loop over them
        for (int i = 1; i <= 2; i++) {
            assumeTrue("Cannot setup file times for test",
                    selectorRule.getMirrorFiles()[i].setLastModified(testtime - (3 * 60 * 60 * 100)));
        }

        assertEquals("TFFFFTTTTTTT", selectorRule.mirrorSelectionString(s));
    }

    @Test
    public void testSelectionBehaviourBeforeWithGranularity() {
        long testtime = selectorRule.getMirrorFiles()[6].lastModified();
        s.setMillis(testtime);
        s.setWhen(before);
        s.setGranularity(2);
        for (int i = 7; i <= 10; i++) {
            assumeTrue("Cannot setup file times for test",
                    selectorRule.getMirrorFiles()[i].setLastModified(testtime + (3 * 60 * 60 * 100)));
        }

        assertEquals("TTTTTTTFFFFT", selectorRule.mirrorSelectionString(s));
    }

}
