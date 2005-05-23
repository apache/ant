/*
 * Copyright 2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

public class ResourceSelectorsTest extends BuildFileTest {

    public ResourceSelectorsTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/types/resources/selectors/build.xml");
    }

    public void testname1() {
        executeTarget("testname1");
    }

    public void testname2() {
        executeTarget("testname2");
    }

    public void testexists() {
        executeTarget("testexists");
    }

    public void testinstanceoftype1() {
        executeTarget("testinstanceoftype1");
    }

    public void testinstanceoftype2() {
        executeTarget("testinstanceoftype2");
    }

    public void testinstanceofclass() {
        executeTarget("testinstanceofclass");
    }

    public void testtype() {
        executeTarget("testtype");
    }

    public void testdate() {
        executeTarget("testdate");
    }

    public void testsize() {
        executeTarget("testsize");
    }

    public void testand() {
        executeTarget("testand");
    }

    public void testor() {
        executeTarget("testor");
    }

    public void testnot() {
        executeTarget("testnot");
    }

    public void testnone() {
        executeTarget("testnone");
    }

    public void testmajority1() {
        executeTarget("testmajority1");
    }

    public void testmajority2() {
        executeTarget("testmajority2");
    }

    public void testmajority3() {
        executeTarget("testmajority3");
    }

    public void testmajority4() {
        executeTarget("testmajority4");
    }

}
