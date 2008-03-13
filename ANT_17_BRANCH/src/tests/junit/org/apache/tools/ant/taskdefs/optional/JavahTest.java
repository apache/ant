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
package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildFileTest;

public class JavahTest extends BuildFileTest {

    private final static String BUILD_XML = 
        "src/etc/testcases/taskdefs/optional/javah/build.xml";

    public JavahTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject(BUILD_XML);
    }

    public void tearDown() {
        executeTarget("tearDown");
    }

    public void testSimpleCompile() {
        executeTarget("simple-compile");
        assertTrue(getProject().resolveFile("output/org_example_Foo.h")
                   .exists());
    }

}
