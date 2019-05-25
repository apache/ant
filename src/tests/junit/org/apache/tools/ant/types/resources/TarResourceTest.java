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
package org.apache.tools.ant.types.resources;

import org.apache.tools.ant.BuildFileRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.apache.tools.ant.FileUtilities.getFileContents;
import static org.junit.Assert.assertEquals;


public class TarResourceTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/resources/tarentry.xml");
    }


    @After
    public void tearDown() {
        buildRule.executeTarget("tearDown");
    }

    @Test
    public void testUncompressSource() throws IOException {
        buildRule.executeTarget("uncompressSource");
        assertEquals(getFileContents(buildRule.getProject().resolveFile("../../asf-logo.gif")),
                getFileContents(new File(buildRule.getProject().getProperty("output"), "asf-logo.gif")));
    }
}
