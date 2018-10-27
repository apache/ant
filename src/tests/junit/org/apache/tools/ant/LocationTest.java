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

package org.apache.tools.ant;

import org.apache.tools.ant.taskdefs.ConditionTask;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.types.FileSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocationTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/core/location.xml");
    }

    @Test
    public void testPlainTask() {
        buildRule.executeTarget("testPlainTask");
        Echo e = (Echo) buildRule.getProject().getReference("echo");
        assertFalse(e.getLocation() == Location.UNKNOWN_LOCATION);
        assertFalse(e.getLocation().getLineNumber() == 0);
    }

    @Test
    public void testStandaloneType() {
        buildRule.executeTarget("testStandaloneType");
        Echo e = (Echo) buildRule.getProject().getReference("echo2");
        FileSet f = (FileSet) buildRule.getProject().getReference("fs");
        assertFalse(f.getLocation() == Location.UNKNOWN_LOCATION);
        assertEquals(e.getLocation().getLineNumber() + 1,
                     f.getLocation().getLineNumber());
    }

    @Test
    public void testConditionTask() {
        buildRule.executeTarget("testConditionTask");
        TaskAdapter ta = (TaskAdapter) buildRule.getProject().getReference("cond");
        ConditionTask c = (ConditionTask) ta.getProxy();
        assertFalse(c.getLocation() == Location.UNKNOWN_LOCATION);
        assertFalse(c.getLocation().getLineNumber() == 0);
    }

    @Test
    public void testMacrodefWrappedTask() {
        buildRule.executeTarget("testMacrodefWrappedTask");
        Echo e = (Echo) buildRule.getProject().getReference("echo3");
        assertTrue(buildRule.getLog().indexOf("Line: "
                                    + (e.getLocation().getLineNumber() + 1))
                   > -1);
    }

    @Test
    public void testPresetdefWrappedTask() {
        buildRule.executeTarget("testPresetdefWrappedTask");
        Echo e = (Echo) buildRule.getProject().getReference("echo4");
        assertTrue(buildRule.getLog().indexOf("Line: "
                                    + (e.getLocation().getLineNumber() + 1))
                   > -1);
    }

    public static class EchoLocation extends Task {
        public void execute() {
            log("Line: " + getLocation().getLineNumber(), Project.MSG_INFO);
        }
    }
}

