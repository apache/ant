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

import org.apache.tools.ant.BuildFileTest;

/**
 */
public class DeleteTest extends BuildFileTest {

    public DeleteTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/delete.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void test1() {
        expectBuildException("test1", "required argument not specified");
    }

    public void test2() {
        executeTarget("test2");
    }
//where oh where has my test case 3 gone?
    public void test4() {
        executeTarget("test4");
    }
    public void test5() {
        executeTarget("test5");
    }
    public void test6() {
        executeTarget("test6");
    }
    public void test7() {
        executeTarget("test7");
    }
    public void test8() {
        executeTarget("test8");
    }
    public void test9() {
        executeTarget("test9");
    }
    public void test10() {
        executeTarget("test10");
    }
    public void test11() {
        executeTarget("test11");
    }
    public void test12() {
        executeTarget("test12");
    }
    public void test13() {
        executeTarget("test13");
    }
    public void test14() {
        executeTarget("test14");
    }
    public void test15() {
        executeTarget("test15");
    }
    public void test16() {
        executeTarget("test16");
    }
    public void test17() {
        executeTarget("test17");
    }
}
