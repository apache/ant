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

package org.apache.tools.ant;

import org.apache.tools.ant.BuildFileTest;

/**
 */
public class ImmutableTest extends BuildFileTest {

    public ImmutableTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/core/immutable.xml");
    }

    // override allowed on <available>
    public void test1() {
        executeTarget("test1");
        assertEquals("override", project.getProperty("test"));
    }

    // ensure <tstamp>'s new prefix attribute is working
    public void test2() {
        executeTarget("test2");
        assertNotNull(project.getProperty("DSTAMP"));
        assertNotNull(project.getProperty("start.DSTAMP"));
    }

    // ensure <tstamp> follows the immutability rule
    public void test3() {
        executeTarget("test3");
        assertEquals("original", project.getProperty("DSTAMP"));
    }

    // ensure <condition> follows the immutability rule
    public void test4() {
        executeTarget("test4");
        assertEquals("original", project.getProperty("test"));
    }
    // ensure <checksum> follows the immutability rule
    public void test5() {
        executeTarget("test5");
        assertEquals("original", project.getProperty("test"));
    }

    // ensure <exec> follows the immutability rule
    public void test6() {
        executeTarget("test6");
        assertEquals("original", project.getProperty("test1"));
        assertEquals("original", project.getProperty("test2"));
    }

    // ensure <pathconvert> follows the immutability rule
    public void test7() {
        executeTarget("test7");
        assertEquals("original", project.getProperty("test"));
    }
}

