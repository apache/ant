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
 * Tests DependSet.
 *
 */
public class DependSetTest extends BuildFileTest {

    public DependSetTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/dependset.xml");
    }

    public void test1() {
       expectBuildException("test1","At least one <srcfileset> or <srcfilelist> element must be set");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void test2() {
       expectBuildException("test2","At least one <targetfileset> or <targetfilelist> element must be set");
    }

    public void test3() {
       expectBuildException("test1","At least one <srcfileset> or <srcfilelist> element must be set");
    }

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
}
