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

import java.io.File;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class ContainsRegexpTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/selectors.xml");
    }

    @Test
    public void testContainsRegexp() {
        buildRule.executeTarget("containsregexp");
        File dir = new File(buildRule.getOutputDir(), "regexpseltestdest");
        File[] files = dir.listFiles();
        int filecount = files.length;

        if (filecount != 1)  {
            assertEquals("ContainsRegexp test should have copied 1 file",
                         1, files.length);

        }
    }

}

