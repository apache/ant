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

package org.apache.tools.ant.taskdefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

/**
 */
public class DirnameTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/dirname.xml");
    }

    @Test
    public void test1() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("property attribute required");
        buildRule.executeTarget("test1");
    }

    @Test
    public void test2() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("file attribute required");
        buildRule.executeTarget("test2");
    }

    @Test
    public void test3() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("property attribute required");
        buildRule.executeTarget("test3");
    }

    @Test
    public void test4() {
        assumeFalse("Skip on DOS or Netware", Os.isFamily("netware") || Os.isFamily("dos"));
        buildRule.executeTarget("test4");
        assertEquals("dirname failed", File.separator + "usr" + File.separator + "local",
                buildRule.getProject().getProperty("local.dir"));
    }

    @Test
    public void test5() {
        buildRule.executeTarget("test5");
        assertEquals("dirname failed",
                buildRule.getProject().getProperty(MagicNames.PROJECT_BASEDIR),
                buildRule.getProject().getProperty("base.dir"));
    }

}
