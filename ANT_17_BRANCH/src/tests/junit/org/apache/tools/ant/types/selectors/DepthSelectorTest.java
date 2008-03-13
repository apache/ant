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
 * Tests Depth Selectors
 *
 */
public class DepthSelectorTest extends BaseSelectorTest {

    private Project project;

    public DepthSelectorTest(String name) {
        super(name);
    }

    /**
     * Factory method from base class. This is overriden in child
     * classes to return a specific Selector class.
     */
    public BaseSelector getInstance() {
        return new DepthSelector();
    }

    /**
     * Test the code that validates the selector.
     */
    public void testValidate() {
        DepthSelector s = (DepthSelector)getInstance();
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DepthSelector did not check for required fields");
        } catch (BuildException be1) {
            assertEquals("You must set at least one of the min or the " +
                    "max levels.", be1.getMessage());
        }

        s = (DepthSelector)getInstance();
        s.setMin(5);
        s.setMax(2);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DepthSelector did not check for maximum being higher "
                    + "than minimum");
        } catch (BuildException be2) {
            assertEquals("The maximum depth is lower than the minimum.",
                    be2.getMessage());
        }

        s = (DepthSelector)getInstance();
        Parameter param = new Parameter();
        param.setName("garbage in");
        param.setValue("garbage out");
        Parameter[] params = new Parameter[1];
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DepthSelector did not check for valid parameter element");
        } catch (BuildException be3) {
            assertEquals("Invalid parameter garbage in", be3.getMessage());
        }

        s = (DepthSelector)getInstance();
        param = new Parameter();
        param.setName("min");
        param.setValue("garbage out");
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DepthSelector accepted bad minimum as parameter");
        } catch (BuildException be4) {
            assertEquals("Invalid minimum value garbage out",
                    be4.getMessage());
        }

        s = (DepthSelector)getInstance();
        param = new Parameter();
        param.setName("max");
        param.setValue("garbage out");
        params[0] = param;
        s.setParameters(params);
        try {
            s.isSelected(basedir,filenames[0],files[0]);
            fail("DepthSelector accepted bad maximum as parameter");
        } catch (BuildException be5) {
            assertEquals("Invalid maximum value garbage out",
                    be5.getMessage());
        }

    }

    /**
     * Tests to make sure that the selector is selecting files correctly.
     */
    public void testSelectionBehaviour() {
        DepthSelector s;
        String results;

        try {
            makeBed();

            s = (DepthSelector)getInstance();
            s.setMin(20);
            s.setMax(25);
            results = selectionString(s);
            assertEquals("FFFFFFFFFFFF", results);

            s = (DepthSelector)getInstance();
            s.setMin(0);
            results = selectionString(s);
            assertEquals("TTTTTTTTTTTT", results);

            s = (DepthSelector)getInstance();
            s.setMin(1);
            results = selectionString(s);
            assertEquals("FFFFFTTTTTTT", results);

            s = (DepthSelector)getInstance();
            s.setMax(0);
            results = selectionString(s);
            assertEquals("TTTTTFFFFFFF", results);

            s = (DepthSelector)getInstance();
            s.setMin(1);
            s.setMax(1);
            results = selectionString(s);
            assertEquals("FFFFFTTTFFFT", results);

        }
        finally {
            cleanupBed();
        }

    }

}
