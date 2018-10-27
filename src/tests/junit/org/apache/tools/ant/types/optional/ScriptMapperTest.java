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

package org.apache.tools.ant.types.optional;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test our script mapping
 */
public class ScriptMapperTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/mappers/scriptmapper.xml");
    }

    @Test
    public void testClear() {
        buildRule.executeTarget("testClear");
    }

    @Test
    public void testSetMultiple() {
        buildRule.executeTarget("testSetMultiple");
    }

    @Test
    public void testPassthrough() {
        buildRule.executeTarget("testPassthrough");
    }
}
