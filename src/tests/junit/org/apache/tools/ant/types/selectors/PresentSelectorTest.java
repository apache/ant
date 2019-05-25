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
 * Tests Present Selectors
 *
 */
public class PresentSelectorTest {

    private PresentSelector s;

    private File beddir;

    @Rule
    public final BaseSelectorRule selectorRule = new BaseSelectorRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        s = new PresentSelector();
        beddir = selectorRule.getBeddir();
    }

    /**
     * Test the code that validates the selector.
     */
    @Test
    public void testValidate() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot define more than one mapper");
        s.createMapper();
        s.createMapper();
    }

    @Test
    public void testValidateAttributes() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("The targetdir attribute is required.");
        s.isSelected(selectorRule.getProject().getBaseDir(), selectorRule.getFilenames()[0],
                selectorRule.getFiles()[0]);
    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    @Test
    public void testSelectionBehaviour() {
        s.setTargetdir(beddir);
        assertEquals("TTTTTTTTTTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviour1() {
        s.setTargetdir(beddir);
        Mapper m = s.createMapper();
        Mapper.MapperType identity = new Mapper.MapperType();
        identity.setValue("identity");
        m.setType(identity);
        assertEquals("TTTTTTTTTTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviour2() {
        File subdir = selectorRule.getProject().resolveFile("../taskdefs/expected");
        s.setTargetdir(subdir);
        Mapper m = s.createMapper();
        Mapper.MapperType flatten = new Mapper.MapperType();
        flatten.setValue("flatten");
        m.setType(flatten);
        assertEquals("TTTTTTTTTTTF", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviour3() {
        s.setTargetdir(beddir);
        Mapper m = s.createMapper();
        Mapper.MapperType merge = new Mapper.MapperType();
        merge.setValue("merge");
        m.setType(merge);
        m.setTo("asf-logo.gif.gz");
        assertEquals("TTTTTTTTTTTT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviour4() {
        File subdir = new File(beddir, "tar/bz2");
        s.setTargetdir(subdir);
        Mapper m = s.createMapper();
        Mapper.MapperType glob = new Mapper.MapperType();
        glob.setValue("glob");
        m.setType(glob);
        m.setFrom("*.bz2");
        m.setTo("*.tar.bz2");
        assertEquals("FFTFFFFFFFFF", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviour5() {
        File subdir = new File(selectorRule.getOutputDir(), "selectortest2");
        s.setTargetdir(subdir);
        assertEquals("TTTFFTTTTTTT", selectorRule.selectionString(s));
        assertEquals("TTTFFTTTTTTT", selectorRule.selectionString(s));
    }

}
