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
import org.apache.tools.ant.types.Mapper;

import java.io.File;


/**
 * Tests Depend Selectors
 *
 */
public class DependSelectorTest extends BaseSelectorTest {

    private Project project;

    public DependSelectorTest(String name) {
        super(name);
    }

    /**
     * Factory method from base class. This is overriden in child
     * classes to return a specific Selector class.
     */
    public BaseSelector getInstance() {
        return new DependSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    public void testValidate() {
        DependSelector s = (DependSelector)getInstance();
        try {
            s.createMapper();
            s.createMapper();
            fail("DependSelector allowed more than one nested mapper.");
        } catch (BuildException be1) {
            assertEquals("Cannot define more than one mapper",
                    be1.getMessage());
        }

        s = (DependSelector)getInstance();
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DependSelector did not check for required fields");
        } catch (BuildException be2) {
            assertEquals("The targetdir attribute is required.",
                    be2.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    public void testSelectionBehaviour() {
        DependSelector s;
        String results;
        File subdir;
        Mapper m;
        Mapper.MapperType identity = new Mapper.MapperType();
        identity.setValue("identity");
        Mapper.MapperType glob = new Mapper.MapperType();
        glob.setValue("glob");
        Mapper.MapperType merge = new Mapper.MapperType();
        merge.setValue("merge");

        try {
            makeBed();

            s = (DependSelector)getInstance();
            s.setTargetdir(beddir);
            results = selectionString(s);
            assertEquals("FFFFFFFFFFFF", results);

            s = (DependSelector)getInstance();
            s.setTargetdir(beddir);
            m = s.createMapper();
            m.setType(identity);
            results = selectionString(s);
            assertEquals("FFFFFFFFFFFF", results);

            s = (DependSelector)getInstance();
            s.setTargetdir(beddir);
            m = s.createMapper();
            m.setType(merge);
            m.setTo("asf-logo.gif.gz");
            results = selectionString(s);
            assertEquals("TFFFFTTTFFF", results.substring(0,11));

            s = (DependSelector)getInstance();
            s.setTargetdir(beddir);
            m = s.createMapper();
            m.setType(merge);
            m.setTo("asf-logo.gif.bz2");
            results = selectionString(s);
            assertEquals("TTFTTTTTTTTT", results);

            // Test for path relative to project base directory
            s = (DependSelector)getInstance();
            subdir = new File("selectortest/tar/bz2");
            s.setTargetdir(subdir);
            m = s.createMapper();
            m.setType(glob);
            m.setFrom("*.bz2");
            m.setTo("*.tar.bz2");
            results = selectionString(s);
            assertEquals("FFTFFFFFFTTF", results);

            s = (DependSelector)getInstance();
            subdir = new File(beddir,"tar/bz2");
            s.setTargetdir(subdir);
            m = s.createMapper();
            m.setType(glob);
            m.setFrom("*.bz2");
            m.setTo("*.tar.bz2");
            results = selectionString(s);
            assertEquals("FFFFFFFFFTTF", results);

            try {
                makeMirror();

                s = (DependSelector)getInstance();
                File testdir = getProject().resolveFile("selectortest2");
                s.setTargetdir(testdir);
                results = selectionString(s);
                assertEquals("FFFTTFFFFFFF", results);

                s = (DependSelector)getInstance();
                testdir = getProject().resolveFile("selectortest2/tar/bz2");
                s.setTargetdir(testdir);
                m = s.createMapper();
                m.setType(glob);
                m.setFrom("*.bz2");
                m.setTo("*.tar.bz2");
                results = mirrorSelectionString(s);
                assertEquals("FFFFFFFFFTTF", results);
                results = selectionString(s);
                assertEquals("FFFFFFFFFTTF", results);
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
