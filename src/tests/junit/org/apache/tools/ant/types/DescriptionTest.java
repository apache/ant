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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * Description tests
 */
@RunWith(Parameterized.class)
public class DescriptionTest {
    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> targets() {
        return Arrays.asList(new Object[][]{
                {"description1", "Single", "Test Project Description"},
                {"description2", "Multi line", "Multi Line\nProject Description"},
                {"description3", "Multi instance", "Multi Instance Project Description"},
                {"description4", "Multi instance nested", "Multi Instance Nested Project Description"}
        });
    }

    @Parameterized.Parameter
    public String fileName;

    @Parameterized.Parameter(1)
    public String description;

    @Parameterized.Parameter(2)
    public String outcome;

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Test
    public void test() {
        buildRule.configureProject("src/etc/testcases/types/" + fileName + ".xml");
        assertEquals(description + " description failed", outcome,
                buildRule.getProject().getDescription());
    }
}
