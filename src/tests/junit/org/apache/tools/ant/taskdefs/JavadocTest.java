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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;

public class JavadocTest extends BuildFileTest {

    public JavadocTest(String name) {
        super(name);
    }

    private static final String BUILD_PATH = "src/etc/testcases/taskdefs/javadoc/";
    private static final String BUILD_FILENAME = "javadoc.xml";
    private static final String BUILD_FILE = BUILD_PATH + BUILD_FILENAME;

    protected void setUp() throws Exception {
        super.setUp();
        configureProject(BUILD_FILE);
    }

    // PR 38370
    public void testDirsetPath() throws Exception {
        executeTarget("dirsetPath");
    }

    // PR 38370
    public void testDirsetPathWithoutPackagenames() throws Exception {
        try {
            executeTarget("dirsetPathWithoutPackagenames");
        } catch (BuildException e) {
            fail("Contents of path should be picked up without specifying package names: " + e);
        }
    }

    // PR 38370
    public void testNestedDirsetPath() throws Exception {
        executeTarget("nestedDirsetPath");
    }

    // PR 38370
    public void testFilesetPath() throws Exception {
        try {
            executeTarget("filesetPath");
        } catch (BuildException e) {
            fail("A path can contain filesets: " + e);
        }
    }

    // PR 38370
    public void testNestedFilesetPath() throws Exception {
        try {
            executeTarget("nestedFilesetPath");
        } catch (BuildException e) {
            fail("A path can contain nested filesets: " + e);
        }
    }

    // PR 38370
    public void testFilelistPath() throws Exception {
        try {
            executeTarget("filelistPath");
        } catch (BuildException e) {
            fail("A path can contain filelists: " + e);
        }
    }

    // PR 38370
    public void testNestedFilelistPath() throws Exception {
        try {
            executeTarget("nestedFilelistPath");
        } catch (BuildException e) {
            fail("A path can contain nested filelists: " + e);
        }
    }

    // PR 38370
    public void testPathelementPath() throws Exception {
        executeTarget("pathelementPath");
    }

    // PR 38370
    public void testPathelementLocationPath() throws Exception {
        try {
            executeTarget("pathelementLocationPath");
        } catch (BuildException e) {
            fail("A path can contain pathelements pointing to a file: " + e);
        }
    }

    // PR 38370
    public void testNestedSource() throws Exception {
        executeTarget("nestedSource");
    }

    // PR 38370
    public void testNestedFilesetRef() throws Exception {
        executeTarget("nestedFilesetRef");
    }

    // PR 38370
    public void testNestedFilesetRefInPath() throws Exception {
        executeTarget("nestedFilesetRefInPath");
    }

    public void testNestedFilesetNoPatterns() throws Exception {
        executeTarget("nestedFilesetNoPatterns");
    }

    public void testDoublyNestedFileset() throws Exception {
        executeTarget("doublyNestedFileset");
    }

    public void testDoublyNestedFilesetNoPatterns() throws Exception {
        executeTarget("doublyNestedFilesetNoPatterns");
    }
}
