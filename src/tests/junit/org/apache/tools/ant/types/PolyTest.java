/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class PolyTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> targets() {
        return Arrays.asList(new Object[][]{
                {"fileset", "types.FileSet"},
                {"fileset-ant-type", "types.PolyTest$MyFileSet"},
                {"path", "types.Path"},
                {"path-ant-type", "types.PolyTest$MyPath"}
        });
    }

    @Parameterized.Parameter
    public String targetName;

    @Parameterized.Parameter(1)
    public String outcome;

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/poly.xml");
    }

    @Test
    public void test() {
        buildRule.executeTarget(targetName);
        assertThat(buildRule.getLog(), containsString(outcome));
    }

    public static class MyFileSet extends FileSet {
    }

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
