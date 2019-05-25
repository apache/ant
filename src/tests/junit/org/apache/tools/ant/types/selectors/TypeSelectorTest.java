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

import org.apache.tools.ant.BuildException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 * Tests Type Selectors.
 *
 */
public class TypeSelectorTest {

    @Rule
    public BaseSelectorRule selectorRule = new BaseSelectorRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private TypeSelector s;

    @Before
    public void setUp() {
        s = new TypeSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    @Test
    public void testValidate() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("The type attribute is required");
        s.isSelected(selectorRule.getProject().getBaseDir(),
                selectorRule.getFilenames()[0], selectorRule.getFiles()[0]);
    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    @Test
    public void testSelectionBehaviourDir() {
        TypeSelector.FileType directory = new TypeSelector.FileType();
        directory.setValue("dir");

        s.setType(directory);
        assertEquals("TFFFFFFFFFFT", selectorRule.selectionString(s));
    }

    @Test
    public void testSelectionBehaviourFile() {
        TypeSelector.FileType file = new TypeSelector.FileType();
        file.setValue("file");

        s.setType(file);
        assertEquals("FTTTTTTTTTTF", selectorRule.selectionString(s));
    }

}
