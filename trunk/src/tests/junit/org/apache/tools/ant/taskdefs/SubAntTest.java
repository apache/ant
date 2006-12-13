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

import java.io.File;

import junit.framework.AssertionFailedError;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildListener;


public class SubAntTest extends BuildFileTest {

    public SubAntTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/subant.xml");
    }

    public void testnodirs() {
        project.executeTarget("testnodirs");
        expectLog("testnodirs", "No sub-builds to iterate on");
    }

    // target must be specified
    public void testgenericantfile() {
        File dir1 = project.resolveFile(".");
        File dir2 = project.resolveFile("subant/subant-test1");
        File dir3 = project.resolveFile("subant/subant-test2");

        testBaseDirs("testgenericantfile",
                     new String[] { dir1.getAbsolutePath(),
                         dir2.getAbsolutePath(),
                         dir3.getAbsolutePath()

                     });
    }

    public void testantfile() {
        File dir1 = project.resolveFile(".");
        // basedir of subant/subant-test1/subant.xml is ..
        // therefore we expect here the subant/subant-test1 subdirectory
        File dir2 = project.resolveFile("subant/subant-test1");
        // basedir of subant/subant-test2/subant.xml is ..
        // therefore we expect here the subant subdirectory
        File dir3 = project.resolveFile("subant");

        testBaseDirs("testantfile",
                     new String[] { dir1.getAbsolutePath(),
                         dir2.getAbsolutePath(),
                         dir3.getAbsolutePath()

                     });

    }
    
    public void testMultipleTargets() {
        executeTarget("multipleTargets");
        assertLogContaining("test1-one");
        assertLogContaining("test1-two");
        assertLogContaining("test2-one");
        assertLogContaining("test2-two");
    }
    
    public void testMultipleTargetsOneDoesntExist_FOEfalse() {
        executeTarget("multipleTargetsOneDoesntExist_FOEfalse");
        assertLogContaining("Target \"three\" does not exist in the project \"subant\"");
    }
    
    public void testMultipleTargetsOneDoesntExist_FOEtrue() {
        expectBuildExceptionContaining("multipleTargetsOneDoesntExist_FOEtrue", 
                                       "Calling not existent target", 
                                       "Target \"three\" does not exist in the project \"subant\"");
    }

    protected void testBaseDirs(String target, String[] dirs) {
        SubAntTest.BasedirChecker bc = new SubAntTest.BasedirChecker(dirs);
        project.addBuildListener(bc);
        executeTarget(target);
        AssertionFailedError ae = bc.getError();
        if (ae != null) {
            throw ae;
        }
        project.removeBuildListener(bc);
    }

    private class BasedirChecker implements BuildListener {
        private String[] expectedBasedirs;
        private int calls = 0;
        private AssertionFailedError error;

        BasedirChecker(String[] dirs) {
            expectedBasedirs = dirs;
        }

        public void buildStarted(BuildEvent event) {}
        public void buildFinished(BuildEvent event) {}
        public void targetFinished(BuildEvent event){}
        public void taskStarted(BuildEvent event) {}
        public void taskFinished(BuildEvent event) {}
        public void messageLogged(BuildEvent event) {}

        public void targetStarted(BuildEvent event) {
            if (event.getTarget().getName().equals("")) {
                return;
            }
            if (error == null) {
                try {
                    assertEquals(expectedBasedirs[calls++],
                            event.getProject().getBaseDir().getAbsolutePath());
                } catch (AssertionFailedError e) {
                    error = e;
                }
            }
        }

        AssertionFailedError getError() {
            return error;
        }

    }


}
