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

package org.apache.tools.ant.taskdefs;

import java.io.File;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.BuildListener;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SubAntTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/subant.xml");
    }

    @Test
    public void testnodirs() {
        buildRule.executeTarget("testnodirs");
        assertEquals("No sub-builds to iterate on", buildRule.getLog());
    }

    // target must be specified
    @Test
    public void testgenericantfile() {
        File dir1 = buildRule.getProject().resolveFile(".");
        File dir2 = buildRule.getProject().resolveFile("subant/subant-test1");
        File dir3 = buildRule.getProject().resolveFile("subant/subant-test2");

        testBaseDirs("testgenericantfile",
                new String[] {dir1.getAbsolutePath(),
                        dir2.getAbsolutePath(),
                        dir3.getAbsolutePath()});
    }

    @Test
    public void testantfile() {
        File dir1 = buildRule.getProject().resolveFile(".");
        // basedir of subant/subant-test1/subant.xml is ..
        // therefore we expect here the subant/subant-test1 subdirectory
        File dir2 = buildRule.getProject().resolveFile("subant/subant-test1");
        // basedir of subant/subant-test2/subant.xml is ..
        // therefore we expect here the subant subdirectory
        File dir3 = buildRule.getProject().resolveFile("subant");

        testBaseDirs("testantfile",
                new String[] {dir1.getAbsolutePath(),
                        dir2.getAbsolutePath(),
                        dir3.getAbsolutePath()});

    }

    @Test
    public void testMultipleTargets() {
        buildRule.executeTarget("multipleTargets");
        assertThat(buildRule.getLog(), containsString("test1-one"));
        assertThat(buildRule.getLog(), containsString("test1-two"));
        assertThat(buildRule.getLog(), containsString("test2-one"));
        assertThat(buildRule.getLog(), containsString("test2-two"));
    }

    @Test
    public void testMultipleTargetsOneDoesntExist_FOEfalse() {
        buildRule.executeTarget("multipleTargetsOneDoesntExist_FOEfalse");
        assertThat(buildRule.getLog(),
                containsString("Target \"three\" does not exist in the project \"subant\""));
    }

    @Test
    public void testMultipleTargetsOneDoesntExist_FOEtrue() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Target \"three\" does not exist in the project \"subant\"");
        buildRule.executeTarget("multipleTargetsOneDoesntExist_FOEtrue");
    }

    protected void testBaseDirs(String target, String[] dirs) {
        SubAntTest.BasedirChecker bc = new SubAntTest.BasedirChecker(dirs);
        buildRule.getProject().addBuildListener(bc);
        buildRule.executeTarget(target);
        AssertionError ae = bc.getError();
        if (ae != null) {
            throw ae;
        }
        buildRule.getProject().removeBuildListener(bc);
    }

    private class BasedirChecker implements BuildListener {
        private String[] expectedBasedirs;
        private int calls = 0;
        private AssertionError error;

        BasedirChecker(String[] dirs) {
            expectedBasedirs = dirs;
        }

        public void buildStarted(BuildEvent event) {
        }

        public void buildFinished(BuildEvent event) {
        }

        public void targetFinished(BuildEvent event) {
        }

        public void taskStarted(BuildEvent event) {
        }

        public void taskFinished(BuildEvent event) {
        }

        public void messageLogged(BuildEvent event) {
        }

        public void targetStarted(BuildEvent event) {
            if (event.getTarget().getName().isEmpty()) {
                return;
            }
            if (error == null) {
                try {
                    assertEquals(expectedBasedirs[calls++],
                            event.getProject().getBaseDir().getAbsolutePath());
                } catch (AssertionError e) {
                    error = e;
                }
            }
        }

        AssertionError getError() {
            return error;
        }
    }

}
