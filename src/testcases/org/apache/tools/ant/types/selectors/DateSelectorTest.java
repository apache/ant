/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.types.selectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.Date;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

/**
 * Tests Date Selectors.
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 */
public class DateSelectorTest extends BaseSelectorTest {

    private Project project;

    public DateSelectorTest(String name) {
        super(name);
    }

    /**
     * Factory method from base class. This is overriden in child
     * classes to return a specific Selector class.
     */
    public BaseSelector getInstance() {
        return new DateSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    public void testValidate() {
        DateSelector s = (DateSelector)getInstance();
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DateSelector did not check for required fields");
        } catch (BuildException be1) {
            assertEquals("You must provide a datetime or the number of "
                    + "milliseconds.", be1.getMessage());
        }

        s = (DateSelector)getInstance();
        s.setDatetime("01/01/1969 01:01 AM");
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DateSelector did not check for Datetime being in the "
                    + "allowable range");
        } catch (BuildException be2) {
            assertEquals("Date of 01/01/1969 01:01 AM results in negative "
                    + "milliseconds value relative to epoch (January 1, "
                    + "1970, 00:00:00 GMT).", be2.getMessage());
        }

        s = (DateSelector)getInstance();
        s.setDatetime("this is not a date");
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DateSelector did not check for Datetime being in a "
                    + "valid format");
        } catch (BuildException be3) {
            assertEquals("Date of this is not a date"
                        + " Cannot be parsed correctly. It should be in"
                        + " MM/DD/YYYY HH:MM AM_PM format.", be3.getMessage());
        }

        s = (DateSelector)getInstance();
        Parameter param = new Parameter();
        param.setName("garbage in");
        param.setValue("garbage out");
        Parameter[] params = new Parameter[1];
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DateSelector did not check for valid parameter element");
        } catch (BuildException be4) {
            assertEquals("Invalid parameter garbage in", be4.getMessage());
        }

        s = (DateSelector)getInstance();
        param = new Parameter();
        param.setName("millis");
        param.setValue("garbage out");
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DateSelector did not check for valid millis parameter");
        } catch (BuildException be5) {
            assertEquals("Invalid millisecond setting garbage out",
                    be5.getMessage());
        }

        s = (DateSelector)getInstance();
        param = new Parameter();
        param.setName("granularity");
        param.setValue("garbage out");
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DateSelector did not check for valid granularity parameter");
        } catch (BuildException be6) {
            assertEquals("Invalid granularity setting garbage out",
                    be6.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
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

        try {
            makeBed();

            s = (DateSelector)getInstance();
            s.setDatetime("10/10/1999 1:45 PM");
            s.setWhen(before);
            results = selectionString(s);
            assertEquals("TFFFFFFFFFFT", results);

            s = (DateSelector)getInstance();
            s.setDatetime("10/10/1999 1:45 PM");
            s.setWhen(before);
            s.setCheckdirs(true);
            results = selectionString(s);
            assertEquals("FFFFFFFFFFFF", results);

            s = (DateSelector)getInstance();
            s.setDatetime("10/10/1999 1:45 PM");
            s.setWhen(after);
            results = selectionString(s);
            assertEquals("TTTTTTTTTTTT", results);

            if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
                s = (DateSelector)getInstance();
                s.setDatetime("11/21/2001 4:54 AM");
                s.setWhen(before);
                results = selectionString(s);
                assertEquals("TFTFFFFFFFFT", results);

                s = (DateSelector)getInstance();
                s.setDatetime("11/21/2001 4:55 AM");
                SimpleDateFormat formatter = new SimpleDateFormat();
                Date d = formatter.parse("11/21/2001 4:55 AM",new ParsePosition(0));

                long milliseconds = s.getMillis();
                s.setWhen(equal);
                results = selectionString(s);
                assertEquals("TTFFTFFFTTTT", results);

                s = (DateSelector)getInstance();
                s.setMillis(milliseconds);
                s.setWhen(equal);
                results = selectionString(s);
                assertEquals("TTFFTFFFTTTT", results);

                s = (DateSelector)getInstance();
                s.setDatetime("11/21/2001 4:56 AM");
                s.setWhen(after);
                results = selectionString(s);
                assertEquals("TFFTFTTTFFFT", results);

                s = (DateSelector)getInstance();
                Parameter param1 = new Parameter();
                Parameter param2 = new Parameter();
                param1.setName("datetime");
                param1.setValue("11/21/2001 4:56 AM");
                param2.setName("when");
                param2.setValue("after");
                Parameter[] params = {param1,param2};
                s.setParameters(params);
                results = selectionString(s);
                assertEquals("TFFTFTTTFFFT", results);
            }
            try {
                makeMirror();

                s = (DateSelector)getInstance();
                long testtime = mirrorfiles[5].lastModified();
                s.setMillis(testtime);
                s.setWhen(after);
                s.setGranularity(2);
                results = mirrorSelectionString(s);
                assertEquals("TFFFFTTTTTTT", results);

                s = (DateSelector)getInstance();
                testtime = mirrorfiles[6].lastModified();
                s.setMillis(testtime);
                s.setWhen(before);
                s.setGranularity(2);
                results = mirrorSelectionString(s);
                assertEquals("TTTTTTTFFFFT", results);
            }
            finally {
                cleanupMirror();
            }

        }
        finally {
            cleanupBed();
        }

    }

}
