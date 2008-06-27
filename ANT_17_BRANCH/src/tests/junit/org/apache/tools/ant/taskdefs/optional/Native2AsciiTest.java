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

import java.io.File;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

public class Native2AsciiTest extends BuildFileTest {

    private final static String BUILD_XML = 
        "src/etc/testcases/taskdefs/optional/native2ascii/build.xml";

    public Native2AsciiTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject(BUILD_XML);
    }

    public void tearDown() {
        executeTarget("tearDown");
    }

    public void testIso8859_1() throws java.io.IOException {
        executeTarget("testIso8859-1");
        File in = getProject().resolveFile("expected/iso8859-1.test");
        File out = getProject().resolveFile("output/iso8859-1.test");
        assertTrue(FileUtils.getFileUtils().contentEquals(in, out, true));
    }
}
