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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Parameter;

/**
 * Tests Contains Selectors.
 *
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

            s = (ContainsSelector)getInstance();
            s.setText("ApacheAnt");
            s.setIgnorewhitespace(true);
            results = selectionString(s);
            assertEquals("TFFFTFFFFFFT", results);

            s = (ContainsSelector)getInstance();
            s.setText("A p a c h e    A n t");
            s.setIgnorewhitespace(true);
            results = selectionString(s);
            assertEquals("TFFFTFFFFFFT", results);

        }
        finally {
            cleanupBed();
        }

    }

}
