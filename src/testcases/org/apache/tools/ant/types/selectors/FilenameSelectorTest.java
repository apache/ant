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
 * Tests Filename Selectors
 *
 */
public class FilenameSelectorTest extends BaseSelectorTest {

    private Project project;

    public FilenameSelectorTest(String name) {
        super(name);
    }

    /**
     * Factory method from base class. This is overriden in child
     * classes to return a specific Selector class.
     */
    public BaseSelector getInstance() {
        return new FilenameSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    public void testValidate() {
        FilenameSelector s = (FilenameSelector)getInstance();
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("FilenameSelector did not check for required fields");
        } catch (BuildException be1) {
            assertEquals("The name attribute is required", be1.getMessage());
        }

        s = (FilenameSelector)getInstance();
        Parameter param = new Parameter();
        param.setName("garbage in");
        param.setValue("garbage out");
        Parameter[] params = {param};
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("FilenameSelector did not check for valid parameter element");
        } catch (BuildException be2) {
            assertEquals("Invalid parameter garbage in", be2.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    public void testSelectionBehaviour() {
        FilenameSelector s;
        String results;

        try {
            makeBed();

            s = (FilenameSelector)getInstance();
            s.setName("no match possible");
            results = selectionString(s);
            assertEquals("FFFFFFFFFFFF", results);

            s = (FilenameSelector)getInstance();
            s.setName("*.gz");
            results = selectionString(s);
            // This is turned off temporarily. There appears to be a bug
            // in SelectorUtils.matchPattern() where it is recursive on
            // Windows even if no ** is in pattern.
            //assertEquals("FFFTFFFFFFFF", results); // Unix
            // vs
            //assertEquals("FFFTFFFFTFFF", results); // Windows

            s = (FilenameSelector)getInstance();
            s.setName("**/*.gz");
            s.setNegate(true);
            results = selectionString(s);
            assertEquals("TTTFTTTFFTTT", results);

            s = (FilenameSelector)getInstance();
            s.setName("**/*.GZ");
            s.setCasesensitive(false);
            results = selectionString(s);
            assertEquals("FFFTFFFTTFFF", results);

            s = (FilenameSelector)getInstance();
            Parameter param1 = new Parameter();
            param1.setName("name");
            param1.setValue("**/*.bz2");
            Parameter[] params = {param1};
            s.setParameters(params);
            results = selectionString(s);
            assertEquals("FFTFFFFFFTTF", results);

        }
        finally {
            cleanupBed();
        }

    }

}
