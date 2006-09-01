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
 * Tests Present Selectors
 *
 */
public class PresentSelectorTest extends BaseSelectorTest {

    private Project project;

    public PresentSelectorTest(String name) {
        super(name);
    }

    /**
     * Factory method from base class. This is overriden in child
     * classes to return a specific Selector class.
     */
    public BaseSelector getInstance() {
        return new PresentSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    public void testValidate() {
        PresentSelector s = (PresentSelector)getInstance();
        try {
            s.createMapper();
            s.createMapper();
            fail("PresentSelector allowed more than one nested mapper.");
        } catch (BuildException be1) {
            assertEquals("Cannot define more than one mapper",
                    be1.getMessage());
        }

        s = (PresentSelector)getInstance();
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("PresentSelector did not check for required fields");
        } catch (BuildException be2) {
            assertEquals("The targetdir attribute is required.",
                    be2.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    public void testSelectionBehaviour() {
        PresentSelector s;
        String results;
        Mapper m;
        Mapper.MapperType identity = new Mapper.MapperType();
        identity.setValue("identity");
        Mapper.MapperType glob = new Mapper.MapperType();
        glob.setValue("glob");
        Mapper.MapperType merge = new Mapper.MapperType();
        merge.setValue("merge");
        Mapper.MapperType flatten = new Mapper.MapperType();
        flatten.setValue("flatten");

        try {
            makeBed();

            s = (PresentSelector)getInstance();
            s.setTargetdir(beddir);
            results = selectionString(s);
            assertEquals("TTTTTTTTTTTT", results);

            s = (PresentSelector)getInstance();
            s.setTargetdir(beddir);
            m = s.createMapper();
            m.setType(identity);
            results = selectionString(s);
            assertEquals("TTTTTTTTTTTT", results);

            s = (PresentSelector)getInstance();
            File subdir = new File(System.getProperty("root"), "src/etc/testcases/taskdefs/expected");
            s.setTargetdir(subdir);
            m = s.createMapper();
            m.setType(flatten);
            results = selectionString(s);
            assertEquals("TTTTTTTTTTTF", results);

            s = (PresentSelector)getInstance();
            s.setTargetdir(beddir);
            m = s.createMapper();
            m.setType(merge);
            m.setTo("asf-logo.gif.gz");
            results = selectionString(s);
            assertEquals("TTTTTTTTTTTT", results);

            s = (PresentSelector)getInstance();
            subdir = new File(beddir, "tar/bz2");
            s.setTargetdir(subdir);
            m = s.createMapper();
            m.setType(glob);
            m.setFrom("*.bz2");
            m.setTo("*.tar.bz2");
            results = selectionString(s);
            assertEquals("FFTFFFFFFFFF", results);

            try {
                makeMirror();

                s = (PresentSelector)getInstance();
                subdir = getProject().resolveFile("selectortest2");
                s.setTargetdir(subdir);
                results = mirrorSelectionString(s);
                assertEquals("TTTFFTTTTTTT", results);
                results = selectionString(s);
                assertEquals("TTTFFTTTTTTT", results);


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
