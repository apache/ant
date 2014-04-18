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
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests Filename Selectors
 *
 */
public class FilenameSelectorTest {

    @Rule
    public final BaseSelectorRule selectorRule = new BaseSelectorRule();

    /**
     * Test the code that validates the selector.
     */
    @Test
    public void testValidate() {
        FilenameSelector s = new FilenameSelector();
        try {
            s.isSelected(selectorRule.getProject().getBaseDir(),selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("FilenameSelector did not check for required fields");
        } catch (BuildException be1) {
            assertEquals("The name or regex attribute is required", be1.getMessage());
        }

        s = new FilenameSelector();
        Parameter param = new Parameter();
        param.setName("garbage in");
        param.setValue("garbage out");
        Parameter[] params = {param};
        s.setParameters(params);
        try {
            s.isSelected(selectorRule.getProject().getBaseDir(),selectorRule.getFilenames()[0],selectorRule.getFiles()[0]);
            fail("FilenameSelector did not check for valid parameter element");
        } catch (BuildException be2) {
            assertEquals("Invalid parameter garbage in", be2.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    @Test
    public void testSelectionBehaviour() {
        FilenameSelector s;
        String results;


            s = new FilenameSelector();
            s.setName("no match possible");
            results = selectorRule.selectionString(s);
            assertEquals("FFFFFFFFFFFF", results);

            s = new FilenameSelector();
            s.setName("*.gz");
            results = selectorRule.selectionString(s);
            // This is turned off temporarily. There appears to be a bug
            // in SelectorUtils.matchPattern() where it is recursive on
            // Windows even if no ** is in pattern.
            //assertEquals("FFFTFFFFFFFF", results); // Unix
            // vs
            //assertEquals("FFFTFFFFTFFF", results); // Windows

            s = new FilenameSelector();
            s.setName("**/*.gz");
            s.setNegate(true);
            results = selectorRule.selectionString(s);
            assertEquals("TTTFTTTFFTTT", results);

            s = new FilenameSelector();
            s.setName("**/*.GZ");
            s.setCasesensitive(false);
            results = selectorRule.selectionString(s);
            assertEquals("FFFTFFFTTFFF", results);

            s = new FilenameSelector();
            Parameter param1 = new Parameter();
            param1.setName("name");
            param1.setValue("**/*.bz2");
            Parameter[] params = {param1};
            s.setParameters(params);
            results = selectorRule.selectionString(s);
            assertEquals("FFTFFFFFFTTF", results);

        

    }

}
