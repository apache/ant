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
public class CopydirTest extends BuildFileTest {

    public CopydirTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/copydir.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void test1() {
        expectBuildException("test1", "required argument not specified");
    }

    public void test2() {
        expectBuildException("test2", "required argument not specified");
    }

    public void test3() {
        expectBuildException("test3", "required argument not specified");
    }

    public void test4() {
        expectLog("test4", "DEPRECATED - The copydir task is deprecated.  Use copy instead.Warning: src == dest");
    }

    public void test5() {
        executeTarget("test5");
        java.io.File f = new java.io.File(getProjectDir(), "../taskdefs.tmp");
        if (!f.exists() || !f.isDirectory()) {
            fail("Copy failed");
        }
        // We keep this, so we have something to delete in later tests :-)
    }

    public void test6() {
        expectBuildException("test6", "target is file");
    }

}
