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
 * Tests Contains Selectors.
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 */
public class ContainsSelectorTest extends BaseSelectorTest {

    private Project project;

    public ContainsSelectorTest(String name) {
        super(name);
    }

    /**
     * Factory method from base class. This is overriden in child
     * classes to return a specific Selector class.
     */
    public BaseSelector getInstance() {
        return new ContainsSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    public void testValidate() {
        ContainsSelector s = (ContainsSelector)getInstance();
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("ContainsSelector did not check for required field 'text'");
        } catch (BuildException be1) {
            assertEquals("The text attribute is required", be1.getMessage());
        }

        s = (ContainsSelector)getInstance();
        Parameter param = new Parameter();
        param.setName("garbage in");
        param.setValue("garbage out");
        Parameter[] params = {param};
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("ContainsSelector did not check for valid parameter element");
        } catch (BuildException be2) {
            assertEquals("Invalid parameter garbage in", be2.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    public void testSelectionBehaviour() {
        ContainsSelector s;
        String results;

        try {
            makeBed();

            s = (ContainsSelector)getInstance();
            s.setText("no such string in test files");
            results = selectionString(s);
            assertEquals("TFFFFFFFFFFT", results);

            s = (ContainsSelector)getInstance();
            s.setText("Apache Ant");
            results = selectionString(s);
            assertEquals("TFFFTFFFFFFT", results);

            s = (ContainsSelector)getInstance();
            s.setText("apache ant");
            s.setCasesensitive(true);
            results = selectionString(s);
            assertEquals("TFFFFFFFFFFT", results);

            s = (ContainsSelector)getInstance();
            s.setText("apache ant");
            s.setCasesensitive(false);
            results = selectionString(s);
            assertEquals("TFFFTFFFFFFT", results);

        }
        finally {
            cleanupBed();
        }

    }

}
