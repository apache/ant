/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.types.selectors;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Mapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


/**
 * Tests Depend Selectors
 *
 */
public class DependSelectorTest {

    private DependSelector s;

    @Rule
    public final BaseSelectorRule selectorRule = new BaseSelectorRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        s = new DependSelector();
    }

    @Test
    public void testValidateSingleMapper() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot define more than one mapper");
        s.createMapper();
        s.createMapper();
    }


    @Test
     public void testValidateRequiredFields() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("The targetdir attribute is required.");
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    @Test
    public void testNoMapper() {
        s.setTargetdir(selectorRule.getBeddir());
        assertEquals("FFFFFFFFFFFF", selectorRule.selectionString(s));
    }

    @Test
    public void testIdentityMapper() {
        s.setTargetdir(selectorRule.getBeddir());

        Mapper.MapperType identity = new Mapper.MapperType();
        identity.setValue("identity");

        Mapper m = s.createMapper();
        m.setType(identity);

        assertEquals("FFFFFFFFFFFF", selectorRule.selectionString(s));
    }

    @Test
    public void testMergeMapper() {
        s.setTargetdir(selectorRule.getBeddir());

        Mapper.MapperType merge = new Mapper.MapperType();
        merge.setValue("merge");

        Mapper m = s.createMapper();
        m.setType(merge);
        m.setTo("asf-logo.gif.gz");

        assertEquals("TFFFFTTTFFF", selectorRule.selectionString(s).substring(0, 11));
    }

    @Test
    public void testMergeMapper2() {
        s.setTargetdir(selectorRule.getBeddir());

        Mapper.MapperType merge = new Mapper.MapperType();
        merge.setValue("merge");

        Mapper m = s.createMapper();
        m.setType(merge);
        m.setTo("asf-logo.gif.bz2");

        assertEquals("TTFTTTTTTTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testGlobMapperRelativePath() {
        File subdir = new File("selectortest/tar/bz2");
        s.setTargetdir(subdir);

        Mapper.MapperType glob = new Mapper.MapperType();
        glob.setValue("glob");

        Mapper m = s.createMapper();
        m.setType(glob);
        m.setFrom("*.bz2");
        m.setTo("*.tar.bz2");

        assertEquals("FFTFFFFFFTTF", selectorRule.selectionString(s));
    }

    @Test
    public void testRestrictedGlobMapper() {
        File subdir = new File(selectorRule.getBeddir(), "tar/bz2");
        s.setTargetdir(subdir);

        Mapper.MapperType glob = new Mapper.MapperType();
        glob.setValue("glob");

        Mapper m = s.createMapper();
        m.setType(glob);
        m.setFrom("*.bz2");
        m.setTo("*.tar.bz2");

        assertEquals("FFFFFFFFFTTF", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionNoMapper() {
        s.setTargetdir(new File(selectorRule.getOutputDir(), "selectortest2"));
        assertEquals("FFFTTFFFFFFF", selectorRule.selectionString(s));
    }


    @Test
    public void testMirroredSelection() {
        s.setTargetdir(new File(selectorRule.getOutputDir(), "selectortest2/tar/bz2"));

        Mapper.MapperType glob = new Mapper.MapperType();
        glob.setValue("glob");

        Mapper m = s.createMapper();
        m.setType(glob);
        m.setFrom("*.bz2");
        m.setTo("*.tar.bz2");

        assertEquals("FFFFFFFFFTTF", selectorRule.mirrorSelectionString(s));
        assertEquals("FFFFFFFFFTTF", selectorRule.selectionString(s));
    }

}
