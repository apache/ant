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

package org.apache.tools.ant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Vector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Executor tests
 */
public class ExecutorTest implements BuildListener  {
    
	private static final String SINGLE_CHECK
        = "org.apache.tools.ant.helper.SingleCheckExecutor";
    private static final String IGNORE_DEPS
        = "org.apache.tools.ant.helper.IgnoreDependenciesExecutor";
    
    private static final Vector<String> TARGET_NAMES;
    static {
        TARGET_NAMES = new Vector<String>();
        TARGET_NAMES.add("a");
        TARGET_NAMES.add("b");
    }
    
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();
    
    private int targetCount;

    /* BuildListener stuff */
    public void targetStarted(BuildEvent event) {
        targetCount++;
    }
    public void buildStarted(BuildEvent event) {}
    public void buildFinished(BuildEvent event) {}
    public void targetFinished(BuildEvent event) {}
    public void taskStarted(BuildEvent event) {}
    public void taskFinished(BuildEvent event) {}
    public void messageLogged(BuildEvent event) {}

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/core/executor.xml");
        targetCount = 0;
        buildRule.getProject().addBuildListener(this);
    }

    private Project getProject(String e) {
        return getProject(e, false);
    }

    private Project getProject(String e, boolean f) {
        return getProject(e, f, false);
    }

    private Project getProject(String e, boolean f, boolean k) {
        Project p = buildRule.getProject();
        p.setNewProperty("ant.executor.class", e);
        p.setKeepGoingMode(k);
        if (f) {
            p.setNewProperty("failfoo", "foo");
        }
        return p;
    }

    @Test
    public void testDefaultExecutor() {
        buildRule.getProject().executeTargets(TARGET_NAMES);
        assertEquals(4, targetCount);
    }

    @Test
    public void testSingleCheckExecutor() {
        getProject(SINGLE_CHECK).executeTargets(TARGET_NAMES);
        assertEquals(3, targetCount);
    }

    @Test
    public void testIgnoreDependenciesExecutor() {
        getProject(IGNORE_DEPS).executeTargets(TARGET_NAMES);
        assertEquals(2, targetCount);
    }

    @Test
    public void testDefaultFailure() {
        try {
            getProject(null, true).executeTargets(TARGET_NAMES);
            fail("should fail");
        } catch (BuildException e) {
            assertEquals("failfoo", e.getMessage());
            assertEquals(1, targetCount);
        }
    }

    @Test
    public void testSingleCheckFailure() {
        try {
            getProject(SINGLE_CHECK, true).executeTargets(TARGET_NAMES);
            fail("should fail");
        } catch (BuildException e) {
            assertEquals("failfoo", e.getMessage());
            assertEquals(1, targetCount);
        }
    }

    @Test
    public void testIgnoreDependenciesFailure() {
        //no foo failure; foo is never executed as dependencies are ignored!
        getProject(IGNORE_DEPS, true).executeTargets(TARGET_NAMES);
    }

    @Test
    public void testKeepGoingDefault() {
        try {
            getProject(null, true, true).executeTargets(TARGET_NAMES);
            fail("should fail");
        } catch (BuildException e) {
            assertEquals("failfoo", e.getMessage());
            assertEquals(2, targetCount);
        }
    }

    @Test
    public void testKeepGoingSingleCheck() {
        try {
            getProject(SINGLE_CHECK, true, true).executeTargets(TARGET_NAMES);
            fail("should fail");
        } catch (BuildException e) {
            assertEquals("failfoo", e.getMessage());
            assertEquals(1, targetCount);
        }
    }

    @Test
    public void testKeepGoingIgnoreDependencies() {
        try {
            //explicitly add foo for failure
            Vector<String> targetNames = new Vector<String>(TARGET_NAMES);
            targetNames.add(0, "foo");
            getProject(IGNORE_DEPS, true, true).executeTargets(targetNames);
            fail("should fail");
        } catch (BuildException e) {
            assertEquals("failfoo", e.getMessage());
            assertEquals(3, targetCount);
        }
    }

}

