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

package org.apache.tools.ant;

import org.apache.tools.ant.taskdefs.ConditionTask;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.types.FileSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;

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
        Echo e = buildRule.getProject().getReference("echo");
        assertNotSame(e.getLocation(), Location.UNKNOWN_LOCATION);
        assertNotEquals(0, e.getLocation().getLineNumber());
    }

    @Test
    public void testStandaloneType() {
        buildRule.executeTarget("testStandaloneType");
        Echo e = buildRule.getProject().getReference("echo2");
        FileSet f = buildRule.getProject().getReference("fs");
        assertNotSame(f.getLocation(), Location.UNKNOWN_LOCATION);
        assertEquals(e.getLocation().getLineNumber() + 1,
                     f.getLocation().getLineNumber());
    }

    @Test
    public void testConditionTask() {
        buildRule.executeTarget("testConditionTask");
        TaskAdapter ta = buildRule.getProject().getReference("cond");
        ConditionTask c = (ConditionTask) ta.getProxy();
        assertNotSame(c.getLocation(), Location.UNKNOWN_LOCATION);
        assertNotEquals(0, c.getLocation().getLineNumber());
    }

    @Test
    public void testMacrodefWrappedTask() {
        buildRule.executeTarget("testMacrodefWrappedTask");
        Echo e = buildRule.getProject().getReference("echo3");
        assertThat(buildRule.getLog(),
                containsString("Line: " + (e.getLocation().getLineNumber() + 1)));
    }

    @Test
    public void testPresetdefWrappedTask() {
        buildRule.executeTarget("testPresetdefWrappedTask");
        Echo e = buildRule.getProject().getReference("echo4");
        assertThat(buildRule.getLog(),
                containsString("Line: " + (e.getLocation().getLineNumber() + 1)));
    }

    public static class EchoLocation extends Task {
        public void execute() {
            log("Line: " + getLocation().getLineNumber(), Project.MSG_INFO);
        }
    }
}

