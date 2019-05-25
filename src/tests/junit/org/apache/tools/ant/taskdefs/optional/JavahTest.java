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
package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

public class JavahTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/javah/build.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("tearDown");
    }

    @Test
    public void testSimpleCompile() {
        assumeFalse(JavaEnvUtils.isAtLeastJavaVersion("10"));
        buildRule.executeTarget("simple-compile");
        assertTrue(new File(buildRule.getProject().getProperty("output"), "org_example_Foo.h")
                .exists());
    }

    @Test
    public void testCompileFileset() {
        assumeFalse(JavaEnvUtils.isAtLeastJavaVersion("10"));
        buildRule.executeTarget("test-fileset");
        assertTrue(new File(buildRule.getProject().getProperty("output"), "org_example_Foo.h")
                .exists());
    }
}
