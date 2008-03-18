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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class PolyTest extends BuildFileTest {

    public PolyTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/types/poly.xml");
    }

    public void testFileSet() {
        expectLogContaining("fileset", "types.FileSet");
    }

    public void testFileSetAntType() {
        expectLogContaining("fileset-ant-type", "types.PolyTest$MyFileSet");
    }

    public void testPath() {
        expectLogContaining("path", "types.Path");
    }

    public void testPathAntType() {
        expectLogContaining("path-ant-type", "types.PolyTest$MyPath");
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
