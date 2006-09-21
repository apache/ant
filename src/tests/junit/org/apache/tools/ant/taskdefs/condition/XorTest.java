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
package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildFileTest;

/**
 * Test that Xor follows the conventional boolean logic semantics
 * (a ^ b) === (a||b)&!(a&&b)
 */
public class XorTest extends BuildFileTest {

    public XorTest(String name) {
        super(name);
    }


    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/conditions/xor.xml");
    }

    public void testEmpty() {
        executeTarget("testEmpty");
    }

    public void test0() {
        executeTarget("test0");
    }

    public void test1() {
        executeTarget("test1");
    }

    public void test00() {
        executeTarget("test00");
    }

    public void test10() {
        executeTarget("test10");
    }

    public void test01() {
        executeTarget("test01");
    }

    public void test11() {
        executeTarget("test11");
    }

}
