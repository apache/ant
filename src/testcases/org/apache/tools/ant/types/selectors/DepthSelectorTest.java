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
 * Tests Depth Selectors
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 */
public class DepthSelectorTest extends BaseSelectorTest {

    private Project project;

    public DepthSelectorTest(String name) {
        super(name);
    }

    /**
     * Factory method from base class. This is overriden in child
     * classes to return a specific Selector class.
     */
    public BaseSelector getInstance() {
        return new DepthSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    public void testValidate() {
        DepthSelector s = (DepthSelector)getInstance();
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DepthSelector did not check for required fields");
        } catch (BuildException be1) {
            assertEquals("You must set at least one of the min or the " +
                    "max levels.", be1.getMessage());
        }

        s = (DepthSelector)getInstance();
        s.setMin(5);
        s.setMax(2);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DepthSelector did not check for maximum being higher "
                    + "than minimum");
        } catch (BuildException be2) {
            assertEquals("The maximum depth is lower than the minimum.",
                    be2.getMessage());
        }

        s = (DepthSelector)getInstance();
        Parameter param = new Parameter();
        param.setName("garbage in");
        param.setValue("garbage out");
        Parameter[] params = new Parameter[1];
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DepthSelector did not check for valid parameter element");
        } catch (BuildException be3) {
            assertEquals("Invalid parameter garbage in", be3.getMessage());
        }

        s = (DepthSelector)getInstance();
        param = new Parameter();
        param.setName("min");
        param.setValue("garbage out");
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DepthSelector accepted bad minimum as parameter");
        } catch (BuildException be4) {
            assertEquals("Invalid minimum value garbage out",
                    be4.getMessage());
        }

        s = (DepthSelector)getInstance();
        param = new Parameter();
        param.setName("max");
        param.setValue("garbage out");
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DepthSelector accepted bad maximum as parameter");
        } catch (BuildException be5) {
            assertEquals("Invalid maximum value garbage out",
                    be5.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    public void testSelectionBehaviour() {
        DepthSelector s;
        String results;

        try {
            makeBed();

            s = (DepthSelector)getInstance();
            s.setMin(20);
            s.setMax(25);
            results = selectionString(s);
            assertEquals("FFFFFFFFFFFF", results);

            s = (DepthSelector)getInstance();
            s.setMin(0);
            results = selectionString(s);
            assertEquals("TTTTTTTTTTTT", results);

            s = (DepthSelector)getInstance();
            s.setMin(1);
            results = selectionString(s);
            assertEquals("FFFFFTTTTTTT", results);

            s = (DepthSelector)getInstance();
            s.setMax(0);
            results = selectionString(s);
            assertEquals("TTTTTFFFFFFF", results);

            s = (DepthSelector)getInstance();
            s.setMin(1);
            s.setMax(1);
            results = selectionString(s);
            assertEquals("FFFFFTTTFFFT", results);

        }
        finally {
            cleanupBed();
        }

    }

}
