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

package org.apache.tools.ant.filters;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

/**
 */
public class StripJavaCommentsTest extends BuildFileTest {
    
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    public StripJavaCommentsTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/filters/build.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testStripJavaComments() throws IOException {
        executeTarget("testStripJavaComments");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(),"expected/stripjavacomments.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(),"result/stripjavacomments.test");
        assertTrue(FILE_UTILS.contentEquals(expected, result));
    }

}
