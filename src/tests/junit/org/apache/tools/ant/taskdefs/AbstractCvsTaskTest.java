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

import org.apache.tools.ant.BuildFileRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 */
public class AbstractCvsTaskTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/abstractcvstask.xml");
        buildRule.executeTarget("setUp");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    @Test
    public void testAbstractCvsTask() {
        buildRule.executeTarget("all");
    }

    @Test
    public void testPackageAttribute() {
        File f = new File(buildRule.getProject().getProperty("output") + "/src/Makefile");
        assertFalse("starting empty", f.exists());
        buildRule.executeTarget("package-attribute");
        assertThat(buildRule.getLog(), containsString("U src/Makefile"));
        assertTrue("now it is there", f.exists());
    }

    @Test
    public void testTagAttribute() {
        File f = new File(buildRule.getProject().getProperty("output") + "/src/Makefile");
        assertFalse("starting empty", f.exists());
        buildRule.executeTarget("tag-attribute");
        assertThat(buildRule.getLog(), containsString("OPENBSD_5_3"));
        assertTrue("now it is there", f.exists());
    }
}
