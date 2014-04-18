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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * FilterSet testing
 *
 */
public class DescriptionTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();
    
    @Test
    public void test1() {
        buildRule.configureProject("src/etc/testcases/types/description1.xml");
        assertEquals("Single description failed", "Test Project Description", buildRule.getProject().getDescription());
    }

    @Test
    public void test2() {
        buildRule.configureProject("src/etc/testcases/types/description2.xml");
        assertEquals("Multi line description failed", "Multi Line\nProject Description", buildRule.getProject().getDescription());
    }

    @Test
    public void test3() {
        buildRule.configureProject("src/etc/testcases/types/description3.xml");
        assertEquals("Multi instance description failed", "Multi Instance Project Description", buildRule.getProject().getDescription());
    }

    @Test
    public void test4() {
        buildRule.configureProject("src/etc/testcases/types/description4.xml");
        assertEquals("Multi instance nested description failed", "Multi Instance Nested Project Description", buildRule.getProject().getDescription());
    }
}
