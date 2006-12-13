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

/**
 * created 16-Mar-2006 12:25:12
 */

public class ExtendedTaskdefTest extends BuildFileTest {

    /**
     * Constructor for the BuildFileTest object.
     *
     * @param name string to pass up to TestCase constructor
     */
    public ExtendedTaskdefTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/core/extended-taskdef.xml");
    }

    /**
     * Automatically calls the target called "tearDown"
     * from the build file tested if it exits.
     * <p/>
     * This allows to use Ant tasks directly in the build file
     * to clean up after each test. Note that no "setUp" target
     * is automatically called, since it's trivial to have a
     * test target depend on it.
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        executeTarget("teardown");
    }

    public void testRun() throws Exception {
        expectBuildExceptionContaining("testRun",
                "exception thrown by the subclass",
                "executing the Foo task");
    }

    public void testRun2() throws Exception {
        expectBuildExceptionContaining("testRun2",
                "exception thrown by the subclass",
                "executing the Foo task");
    }

}
