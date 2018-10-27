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

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertTrue;

public class SyncTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/sync.xml");
    }

    @Test
    public void testSimpleCopy() {
        buildRule.executeTarget("simplecopy");
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsPresent(d);
        assertTrue(buildRule.getFullLog().indexOf("dangling") == -1);
    }

    @Test
    public void testEmptyCopy() {
        buildRule.executeTarget("emptycopy");
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsNotPresent(d);
        String c = buildRule.getProject().getProperty("dest") + "/a/b/c";
        assertFileIsNotPresent(c);
        assertTrue(buildRule.getFullLog().indexOf("dangling") == -1);
    }

    @Test
    public void testEmptyDirCopy() {
        buildRule.executeTarget("emptydircopy");
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsNotPresent(d);
        String c = buildRule.getProject().getProperty("dest") + "/a/b/c";
        assertFileIsPresent(c);
        assertTrue(buildRule.getFullLog().indexOf("dangling") == -1);
    }

    @Test
    public void testCopyAndRemove() {
        testCopyAndRemove("copyandremove");
    }

    @Test
    public void testCopyAndRemoveWithFileList() {
        testCopyAndRemove("copyandremove-with-filelist");
    }

    @Test
    public void testCopyAndRemoveWithZipfileset() {
        testCopyAndRemove("copyandremove-with-zipfileset");
    }

    private void testCopyAndRemove(String target) {
        buildRule.executeTarget(target);
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsPresent(d);
        String f = buildRule.getProject().getProperty("dest") + "/e/f";
        assertFileIsNotPresent(f);
        assertTrue(buildRule.getFullLog().indexOf("Removing orphan file:") > -1);
        assertContains("Removed 1 dangling file from", buildRule.getFullLog());
        assertContains("Removed 1 dangling directory from", buildRule.getFullLog());
    }

    @Test
    public void testCopyAndRemoveEmptyPreserve() {
        buildRule.executeTarget("copyandremove-emptypreserve");
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsPresent(d);
        String f = buildRule.getProject().getProperty("dest") + "/e/f";
        assertFileIsNotPresent(f);
        assertTrue(buildRule.getFullLog().indexOf("Removing orphan file:") > -1);
        assertContains("Removed 1 dangling file from", buildRule.getFullLog());
        assertContains("Removed 1 dangling directory from", buildRule.getFullLog());
    }

    @Test
    public void testEmptyDirCopyAndRemove() {
        buildRule.executeTarget("emptydircopyandremove");
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsNotPresent(d);
        String c = buildRule.getProject().getProperty("dest") + "/a/b/c";
        assertFileIsPresent(c);
        String f = buildRule.getProject().getProperty("dest") + "/e/f";
        assertFileIsNotPresent(f);
        assertTrue(buildRule.getFullLog().indexOf("Removing orphan directory:") > -1);
        assertContains("NO dangling file to remove from", buildRule.getFullLog());
        assertContains("Removed 2 dangling directories from", buildRule.getFullLog());
    }

    @Test
    public void testCopyNoRemove() {
        buildRule.executeTarget("copynoremove");
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsPresent(d);
        String f = buildRule.getProject().getProperty("dest") + "/e/f";
        assertFileIsPresent(f);
        assertTrue(buildRule.getFullLog().indexOf("Removing orphan file:") == -1);
    }

    @Test
    public void testCopyNoRemoveSelectors() {
        buildRule.executeTarget("copynoremove-selectors");
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsPresent(d);
        String f = buildRule.getProject().getProperty("dest") + "/e/f";
        assertFileIsPresent(f);
        assertTrue(buildRule.getFullLog().indexOf("Removing orphan file:") == -1);
    }

    public void assertFileIsPresent(String f) {
        assertTrue("Expected file " + f,
                buildRule.getProject().resolveFile(f).exists());
    }

    public void assertFileIsNotPresent(String f) {
        assertTrue("Didn't expect file " + f,
                   !buildRule.getProject().resolveFile(f).exists());
    }
}
