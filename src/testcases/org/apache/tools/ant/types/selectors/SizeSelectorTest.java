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
import org.apache.tools.ant.util.*;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.types.Parameter;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

/**
 * Tests Size Selectors
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 */
public class SizeSelectorTest extends BaseSelectorTest {

    private Project project;

    public SizeSelectorTest(String name) {
        super(name);
    }

    /**
     * Factory method from base class. This is overriden in child
     * classes to return a specific Selector class.
     */
    public BaseSelector getInstance() {
        return new SizeSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    public void testValidate() {
        SizeSelector s = (SizeSelector)getInstance();
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("SizeSelector did not check for required fields");
        } catch (BuildException be1) {
            assertEquals("The value attribute is required, and must "
                    + "be positive", be1.getMessage());
        }

        s = (SizeSelector)getInstance();
        s.setValue(-10);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("SizeSelector did not check for value being in the "
                    + "allowable range");
        } catch (BuildException be2) {
            assertEquals("The value attribute is required, and must "
                    + "be positive", be2.getMessage());
        }

        s = (SizeSelector)getInstance();
        Parameter param = new Parameter();
        param.setName("garbage in");
        param.setValue("garbage out");
        Parameter[] params = {param};
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("SizeSelector did not check for valid parameter element");
        } catch (BuildException be3) {
            assertEquals("Invalid parameter garbage in", be3.getMessage());
        }

        s = (SizeSelector)getInstance();
        param = new Parameter();
        param.setName("value");
        param.setValue("garbage out");
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("SizeSelector accepted bad value as parameter");
        } catch (BuildException be4) {
            assertEquals("Invalid size setting garbage out",
                    be4.getMessage());
        }

        s = (SizeSelector)getInstance();
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
            s.isSelected(basedir,filenames[0],files[0]);
            fail("SizeSelector accepted bad units as parameter");
        } catch (BuildException be5) {
            assertEquals("garbage out is not a legal value for this attribute",
                    be5.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
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


        try {
            makeBed();

            s = (SizeSelector)getInstance();
            s.setValue(10);
            s.setWhen(less);
            results = selectionString(s);
            assertEquals("TFFFFFFFFFFT", results);

            s = (SizeSelector)getInstance();
            s.setValue(10);
            s.setWhen(more);
            results = selectionString(s);
            assertEquals("TTTTTTTTTTTT", results);

            s = (SizeSelector)getInstance();
            s.setValue(32);
            s.setWhen(equal);
            results = selectionString(s);
            assertEquals("TTFFTFFFFFFT", results);

            s = (SizeSelector)getInstance();
            s.setValue(7);
            s.setWhen(more);
            s.setUnits(kilo);
            results = selectionString(s);
            assertEquals("TFTFFTTTTTTT", results);

            s = (SizeSelector)getInstance();
            s.setValue(7);
            s.setWhen(more);
            s.setUnits(kibi);
            results = selectionString(s);
            assertEquals("TFTFFFTTFTTT", results);

            s = (SizeSelector)getInstance();
            s.setValue(99999);
            s.setWhen(more);
            s.setUnits(tibi);
            results = selectionString(s);
            assertEquals("TFFFFFFFFFFT", results);

            s = (SizeSelector)getInstance();
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
            results = selectionString(s);
            assertEquals("TFFFFFFTFFTT", results);
        }
        finally {
            cleanupBed();
        }

    }

}
