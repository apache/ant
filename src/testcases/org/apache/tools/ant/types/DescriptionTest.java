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

import org.apache.tools.ant.BuildFileTest;

/**
 * FilterSet testing
 *
 */
public class DescriptionTest extends BuildFileTest {

    public DescriptionTest(String name) {
        super(name);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void test1() {
        configureProject("src/etc/testcases/types/description1.xml");
        assertEquals("Single description failed", "Test Project Description", project.getDescription());
    }

    public void test2() {
        configureProject("src/etc/testcases/types/description2.xml");
        assertEquals("Multi line description failed", "Multi Line\nProject Description", project.getDescription());
    }

    public void test3() {
        configureProject("src/etc/testcases/types/description3.xml");
        assertEquals("Multi instance description failed", "Multi Instance Project Description", project.getDescription());
    }

    public void test4() {
        configureProject("src/etc/testcases/types/description4.xml");
        assertEquals("Multi instance nested description failed", "Multi Instance Nested Project Description", project.getDescription());
    }
}
