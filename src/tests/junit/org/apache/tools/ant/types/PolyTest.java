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

import org.apache.tools.ant.AntAssert;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PolyTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();
    
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/poly.xml");
    }

    @Test
    public void testFileSet() {
        buildRule.executeTarget("fileset");
        AntAssert.assertContains( "types.FileSet", buildRule.getLog());
    }

    @Test
    public void testFileSetAntType() {
        buildRule.executeTarget("fileset-ant-type");
        AntAssert.assertContains("types.PolyTest$MyFileSet", buildRule.getLog());
    }

    @Test
    public void testPath() {
        buildRule.executeTarget("path");
        AntAssert.assertContains( "types.Path", buildRule.getLog());
    }

    @Test
    public void testPathAntType() {
        buildRule.executeTarget("path-ant-type");
        AntAssert.assertContains( "types.PolyTest$MyPath", buildRule.getLog());
    }

    public static class MyFileSet extends FileSet {}

    public static class MyPath extends Path {
        public MyPath(Project project) {
            super(project);
        }
    }

    public static class MyTask extends Task {
        public void addPath(Path path) {
            log("class of path is " + path.getClass());
        }
        public void addFileset(FileSet fileset) {
            log("class of fileset is " + fileset.getClass());
        }
    }
}
