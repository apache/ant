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
import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
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
        assertThat(buildRule.getFullLog(), not(containsString("dangling")));
    }

    @Test
    public void testEmptyCopy() {
        buildRule.executeTarget("emptycopy");
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsNotPresent(d);
        String c = buildRule.getProject().getProperty("dest") + "/a/b/c";
        assertFileIsNotPresent(c);
        assertThat(buildRule.getFullLog(), not(containsString("dangling")));
    }

    @Test
    public void testEmptyDirCopy() {
        buildRule.executeTarget("emptydircopy");
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsNotPresent(d);
        String c = buildRule.getProject().getProperty("dest") + "/a/b/c";
        assertFileIsPresent(c);
        assertThat(buildRule.getFullLog(), not(containsString("dangling")));
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
        assertThat(buildRule.getFullLog(), containsString(("Removing orphan file:")));
        assertThat(buildRule.getFullLog(), containsString("Removed 1 dangling file from"));
        assertThat(buildRule.getFullLog(), containsString("Removed 1 dangling directory from"));
    }

    @Test
    public void testCopyAndRemoveEmptyPreserve() {
        buildRule.executeTarget("copyandremove-emptypreserve");
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsPresent(d);
        String f = buildRule.getProject().getProperty("dest") + "/e/f";
        assertFileIsNotPresent(f);
        assertThat(buildRule.getFullLog(), containsString(("Removing orphan file:")));
        assertThat(buildRule.getFullLog(), containsString("Removed 1 dangling file from"));
        assertThat(buildRule.getFullLog(), containsString("Removed 1 dangling directory from"));
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
        assertThat(buildRule.getFullLog(), containsString(("Removing orphan directory:")));
        assertThat(buildRule.getFullLog(), containsString("NO dangling file to remove from"));
        assertThat(buildRule.getFullLog(), containsString("Removed 2 dangling directories from"));
    }

    @Test
    public void testCopyNoRemove() {
        buildRule.executeTarget("copynoremove");
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsPresent(d);
        String f = buildRule.getProject().getProperty("dest") + "/e/f";
        assertFileIsPresent(f);
        assertThat(buildRule.getFullLog(), not(containsString("Removing orphan file:")));
    }

    @Test
    public void testCopyNoRemoveSelectors() {
        buildRule.executeTarget("copynoremove-selectors");
        String d = buildRule.getProject().getProperty("dest") + "/a/b/c/d";
        assertFileIsPresent(d);
        String f = buildRule.getProject().getProperty("dest") + "/e/f";
        assertFileIsPresent(f);
        assertThat(buildRule.getFullLog(), not(containsString("Removing orphan file:")));
    }

    /**
     * Test for bz-62890 bug fix
     */
    @Test
    public void testCaseSensitivityOfDest() {
        buildRule.executeTarget("casesensitivity-test");
        final String destDir = buildRule.getProject().getProperty("dest") + "/casecheck";
        assertFileIsPresent(destDir + "/a.txt");
        final boolean caseSensitive = FileUtils.isCaseSensitiveFileSystem(
                buildRule.getProject().resolveFile(destDir).toPath())
                .orElse(true); // directory scanner defaults to case sensitive = true
        if (caseSensitive) {
            assertFileIsNotPresent(destDir + "/A.txt");
        } else {
            assertFileIsPresent(destDir + "/A.txt");
        }
        assertFileIsPresent(destDir + "/foo.txt");

        assertFileIsNotPresent(destDir + "/bar.txt");
    }


    public void assertFileIsPresent(String f) {
        assertTrue("Expected file " + f, buildRule.getProject().resolveFile(f).exists());
    }

    public void assertFileIsNotPresent(String f) {
        assertFalse("Didn't expect file " + f, buildRule.getProject().resolveFile(f).exists());
    }
}
