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

package org.apache.tools.ant;

import static org.junit.Assert.assertEquals;

import java.util.Vector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Executor tests
 */
public class ExecutorTest implements BuildListener  {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String SINGLE_CHECK
            = "org.apache.tools.ant.helper.SingleCheckExecutor";
    private static final String IGNORE_DEPS
            = "org.apache.tools.ant.helper.IgnoreDependenciesExecutor";

    private static final Vector<String> TARGET_NAMES;
    static {
        TARGET_NAMES = new Vector<>();
        TARGET_NAMES.add("a");
        TARGET_NAMES.add("b");
    }

    private int targetCount;

    /* BuildListener stuff */
    public void targetStarted(BuildEvent event) {
        targetCount++;
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
        p.setNewProperty(MagicNames.ANT_EXECUTOR_CLASSNAME, e);
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
        thrown.expect(BuildException.class);
        thrown.expectMessage("failfoo");
        try {
            getProject(null, true).executeTargets(TARGET_NAMES);
        } finally {
            assertEquals(1, targetCount);
        }
    }

    @Test
    public void testSingleCheckFailure() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("failfoo");
        try {
            getProject(SINGLE_CHECK, true).executeTargets(TARGET_NAMES);
        } finally {
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
        thrown.expect(BuildException.class);
        thrown.expectMessage("failfoo");
        try {
            getProject(null, true, true).executeTargets(TARGET_NAMES);
        } finally {
            assertEquals(2, targetCount);
        }
    }

    @Test
    public void testKeepGoingSingleCheck() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("failfoo");
        try {
            getProject(SINGLE_CHECK, true, true).executeTargets(TARGET_NAMES);
        } finally {
            assertEquals(1, targetCount);
        }
    }

    @Test
    public void testKeepGoingIgnoreDependencies() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("failfoo");
        Vector<String> targetNames = new Vector<>(TARGET_NAMES);
        // explicitly add foo for failure
        targetNames.add(0, "foo");
        try {
            getProject(IGNORE_DEPS, true, true).executeTargets(targetNames);
        } finally {
            assertEquals(3, targetCount);
        }
    }

}
