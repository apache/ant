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

public class ResourceComparatorsTest extends BuildFileTest {

    public ResourceComparatorsTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/types/resources/comparators/build.xml");
    }

    public void tearDown() {
        executeTarget("tearDown");
    }

    public void testcompoundsort1() {
        executeTarget("testcompoundsort1");
    }

    public void testcompoundsort2() {
        executeTarget("testcompoundsort2");
    }

    public void testcontent() {
        executeTarget("testcontent");
    }

    public void testexists() {
        executeTarget("testexists");
    }

    public void testdate() {
        executeTarget("testdate");
    }

    public void testname() {
        executeTarget("testname");
    }

    public void testrvcontent() {
        executeTarget("testrvcontent");
    }

    public void testrvdefault() {
        executeTarget("testrvdefault");
    }

    public void testrvexists() {
        executeTarget("testrvexists");
    }

    public void testrvdate() {
        executeTarget("testrvdate");
    }

    public void testrvname() {
        executeTarget("testrvname");
    }

    public void testrvsize() {
        executeTarget("testrvsize");
    }

    public void testrvtype() {
        executeTarget("testrvtype");
    }

    public void testsize() {
        executeTarget("testsize");
    }

    public void testsortdefault() {
        executeTarget("testsortdefault");
    }

    public void testtype() {
        executeTarget("testtype");
    }

}
