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

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

/**
 */
public class AntlibTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/antlib.xml");
    }

    /**
     * only do the antlib tests if we are in the same JVM as ant.
     * @return boolean
     */
    private boolean isSharedJVM() {
        String property = System.getProperty("tests.and.ant.share.classloader");
        return property != null && Project.toBoolean(property);
    }

    @Test
    public void testAntlibFile() {
        buildRule.executeTarget("antlib.file");
        assertEquals("MyTask called", buildRule.getLog());
    }

    /**
     * Confirms that all matching resources will be used, so that you
     * can collect several antlibs in one Definer call.
     * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=24024">bug 24024</a>
     */
    @Test
    public void testAntlibResource() {
        assertNotNull("build.tests.value not set", System.getProperty("build.tests.value"));
        buildRule.executeTarget("antlib.resource");
        assertEquals("MyTask called-and-then-MyTask2 called", buildRule.getLog());
    }

    @Test
    public void testNsCurrent() {
        buildRule.executeTarget("ns.current");
        assertEquals("Echo2 inside a macroHello from x:p", buildRule.getLog());
    }

    @Test
    public void testAntlib_uri() {
        assumeTrue("Test requires shared JVM", isSharedJVM());
        buildRule.executeTarget("antlib_uri");
    }

    @Test
    public void testAntlib_uri_auto() {
        assumeTrue("Test requires shared JVM", isSharedJVM());
        buildRule.executeTarget("antlib_uri_auto");
    }

    @Test
    public void testAntlib_uri_auto2() {
        assumeTrue("Test requires shared JVM", isSharedJVM());
        buildRule.executeTarget("antlib_uri_auto2");
    }

    public static class MyTask extends Task {
        public void execute() {
            log("MyTask called");
        }
    }

    public static class MyTask2 extends Task {
        public void execute() {
            log("MyTask2 called");
        }
    }

}
