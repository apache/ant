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
 * test nice
 */
public class NiceTest extends BuildFileTest {

    public NiceTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/nice.xml");
    }

    public void testNoop() {
        executeTarget("noop");
    }

    public void testCurrent() {
        executeTarget("current");
    }

    public void testFaster() {
        executeTarget("faster");
    }

    public void testSlower() {
        executeTarget("slower");
    }

    public void testTooSlow() {
        expectBuildExceptionContaining(
                "too_slow","out of range","out of the range 1-10");
    }

    public void testTooFast() {
        expectBuildExceptionContaining(
                "too_fast", "out of range", "out of the range 1-10");
    }

}
