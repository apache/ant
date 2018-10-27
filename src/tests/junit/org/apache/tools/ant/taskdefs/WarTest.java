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

package org.apache.tools.ant.taskdefs;

import java.io.File;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Testcase for the war task
 *
 */
public class WarTest {
    public static final String TEST_BUILD_FILE
        = "src/etc/testcases/taskdefs/war.xml";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject(TEST_BUILD_FILE);
    }

    /**
     * Test direct dependency removal
     */
    @Test
    public void testLibRefs() {
        buildRule.executeTarget("testlibrefs");
        File f = new File(buildRule.getOutputDir(), "WEB-INF/lib/war.xml");
        assertTrue("File has been put into lib", f.exists());
    }
}
