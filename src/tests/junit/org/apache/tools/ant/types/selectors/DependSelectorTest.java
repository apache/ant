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
import org.apache.tools.ant.types.Mapper;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Tests Depend Selectors
 *
 */
public class DependSelectorTest {

    @Rule
    public final BaseSelectorRule selectorRule = new BaseSelectorRule();

    @Test
    public void testValidateSingleMapper() {
        try {
            DependSelector s = new DependSelector();
            s.createMapper();
            s.createMapper();
            fail("DependSelector allowed more than one nested mapper.");
        } catch (BuildException be1) {
            assertEquals("Cannot define more than one mapper",
                    be1.getMessage());
        }
    }


    @Test
     public void testValidateRequiredFields() {
        try {
            DependSelector s = new DependSelector();
            s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0], selectorRule.getFiles()[0]);
            fail("DependSelector did not check for required fields");
        } catch (BuildException be2) {
            assertEquals("The targetdir attribute is required.",
                    be2.getMessage());
        }

    }

    @Test
    public void testNoMapper() {
        DependSelector s = new DependSelector();
        s.setTargetdir(selectorRule.getBeddir());

        String results = selectorRule.selectionString(s);
        assertEquals("FFFFFFFFFFFF", results);
    }

    @Test
    public void testIdentityMapper() {
        DependSelector s = new DependSelector();
        s.setTargetdir(selectorRule.getBeddir());

        Mapper.MapperType identity = new Mapper.MapperType();
        identity.setValue("identity");

        Mapper m = s.createMapper();
        m.setType(identity);

        String results = selectorRule.selectionString(s);
        assertEquals("FFFFFFFFFFFF", results);
    }

    @Test
    public void testMergeMapper() {
        DependSelector s = new DependSelector();
        s.setTargetdir(selectorRule.getBeddir());

        Mapper.MapperType merge = new Mapper.MapperType();
        merge.setValue("merge");

        Mapper m = s.createMapper();
        m.setType(merge);
        m.setTo("asf-logo.gif.gz");

        String results = selectorRule.selectionString(s);
        assertEquals("TFFFFTTTFFF", results.substring(0,11));
    }

    @Test
    public void testMergeMapper2() {
        DependSelector s = new DependSelector();
        s.setTargetdir(selectorRule.getBeddir());

        Mapper.MapperType merge = new Mapper.MapperType();
        merge.setValue("merge");

        Mapper m = s.createMapper();
        m.setType(merge);
        m.setTo("asf-logo.gif.bz2");
        String results = selectorRule.selectionString(s);
        assertEquals("TTFTTTTTTTTT", results);
    }

    @Test
    public void testGlobMapperRelativePath() {
        DependSelector s = new DependSelector();
        File subdir = new File("selectortest/tar/bz2");
        s.setTargetdir(subdir);

        Mapper.MapperType glob = new Mapper.MapperType();
        glob.setValue("glob");

        Mapper m = s.createMapper();
        m.setType(glob);
        m.setFrom("*.bz2");
        m.setTo("*.tar.bz2");

        String results = selectorRule.selectionString(s);
        assertEquals("FFTFFFFFFTTF", results);
    }

    @Test
    public void testRestrictedGlobMapper() {
        DependSelector s = new DependSelector();
        File subdir = new File(selectorRule.getBeddir(), "tar/bz2");
        s.setTargetdir(subdir);

        Mapper.MapperType glob = new Mapper.MapperType();
        glob.setValue("glob");

        Mapper m = s.createMapper();
        m.setType(glob);
        m.setFrom("*.bz2");
        m.setTo("*.tar.bz2");
        String results = selectorRule.selectionString(s);
        assertEquals("FFFFFFFFFTTF", results);
    }

    @Test
    public void testSelectionNoMapper() {
        DependSelector s = new DependSelector();
        s.setTargetdir(new File(selectorRule.getOutputDir(), "selectortest2"));
        String results = selectorRule.selectionString(s);
        assertEquals("FFFTTFFFFFFF", results);
    }


    @Test
    public void testMirroredSelection() {
        DependSelector s = new DependSelector();
        s.setTargetdir(new File(selectorRule.getOutputDir(), "selectortest2/tar/bz2"));

        Mapper.MapperType glob = new Mapper.MapperType();
        glob.setValue("glob");

        Mapper m = s.createMapper();
        m.setType(glob);
        m.setFrom("*.bz2");
        m.setTo("*.tar.bz2");
        String results = selectorRule.mirrorSelectionString(s);
        assertEquals("FFFFFFFFFTTF", results);
        results = selectorRule.selectionString(s);
        assertEquals("FFFFFFFFFTTF", results);
    }

}
