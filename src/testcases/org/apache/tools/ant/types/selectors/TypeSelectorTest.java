/*
 * Copyright  2003-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.Date;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

/**
 * Tests Type Selectors.
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @author <a href="mailto:levylambert@tiscali-dsl.de">Antoine Levy-Lambert</a>
 */
public class TypeSelectorTest extends BaseSelectorTest {

    public TypeSelectorTest(String name) {
        super(name);
    }

    /**
     * Factory method from base class. This is overriden in child
     * classes to return a specific Selector class.
     */
    public BaseSelector getInstance() {
        return new TypeSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    public void testValidate() {
        TypeSelector s = (TypeSelector)getInstance();
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("TypeSelector did not check for required fields");
        } catch (BuildException be1) {
            assertEquals("The type attribute is required"
                    , be1.getMessage());
        }
    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    public void testSelectionBehaviour() {
        TypeSelector s;
        String results;

        TypeSelector.FileType directory = new
                TypeSelector.FileType();
        directory.setValue("dir");
        TypeSelector.FileType file = new
                TypeSelector.FileType();
        file.setValue("file");

        try {
            makeBed();

            s = (TypeSelector)getInstance();
            s.setType(directory);
            results = selectionString(s);
            assertEquals("TFFFFFFFFFFT", results);

            s = (TypeSelector)getInstance();
            s.setType(file);
            results = selectionString(s);
            assertEquals("FTTTTTTTTTTF", results);


        }
        finally {
            cleanupBed();
        }

    }

}
