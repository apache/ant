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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FlexIntegerTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/flexinteger.xml");
    }

    @Test
    public void testFlexInteger() {
        buildRule.executeTarget("test");
        assertEquals(buildRule.getProject().getProperty("flexint.value1"), "10");
        assertEquals(buildRule.getProject().getProperty("flexint.value2"), "8");
    }

    // This class acts as a custom Ant task also
    // and uses these variables/methods in that mode
    private Project taskProject;

    String propName;

    private FlexInteger value;

    public void setPropName(String propName) {
        this.propName = propName;
    }

    public void setValue(FlexInteger value) {
        this.value = value;
    }

    public void setProject(Project project) {
        taskProject = project;
    }

    public void execute() {
        if (propName == null || value == null) {
            throw new BuildException("name and value required");
        }

        taskProject.setNewProperty(propName, value.toString());
    }
}
