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

package org.apache.tools.ant.taskdefs.optional.depend;

import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.FileUtilities;
import org.apache.tools.ant.taskdefs.condition.JavaVersion;
import org.apache.tools.ant.types.FileSet;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Testcase for the Depend optional task.
 *
 */
public class DependTest {
    public static final String RESULT_FILESET = "result";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/depend/depend.xml");
    }

    /**
     * Test direct dependency removal
     */
    @Test
    public void testDirect() {
        buildRule.executeTarget("src1setup");
        buildRule.executeTarget("compile");

        FileUtilities.rollbackTimestamps(new File(buildRule.getProject().getProperty("tempsrc.dir")), 5);
        FileUtilities.rollbackTimestamps(new File(buildRule.getProject().getProperty("classes.dir")), 5);

        buildRule.executeTarget("testdirect");
        Hashtable<String, String> files = getResultFiles();
        assertEquals("Depend did not leave correct number of files", 3, files.size());
        assertThat("Result did not contain A.class", files, hasKey("A.class"));
        assertThat("Result did not contain D.class", files, hasKey("D.class"));
    }

    /**
     * Test dependency traversal (closure)
     */
    @Test
    public void testClosure() {
        buildRule.executeTarget("src1setup");
        buildRule.executeTarget("compile");

        FileUtilities.rollbackTimestamps(new File(buildRule.getProject().getProperty("tempsrc.dir")), 5);
        FileUtilities.rollbackTimestamps(new File(buildRule.getProject().getProperty("classes.dir")), 5);

        buildRule.executeTarget("testclosure");
        Hashtable<String, String> files = getResultFiles();
        assertTrue("Depend did not leave correct number of files", files.size() <= 2);
        assertThat("Result did not contain D.class", files, hasKey("D.class"));
    }

    /**
     * Test that inner class dependencies trigger deletion of the outer class
     */
    @Test
    public void testInner() {
        buildRule.executeTarget("src2setup");
        buildRule.executeTarget("compile");

        FileUtilities.rollbackTimestamps(new File(buildRule.getProject().getProperty("tempsrc.dir")), 5);
        FileUtilities.rollbackTimestamps(new File(buildRule.getProject().getProperty("classes.dir")), 5);

        buildRule.executeTarget("testinner");
        assertEquals("Depend did not leave correct number of files", 0, getResultFiles().size());
    }

    /**
     * Test that multi-level inner class dependencies trigger deletion of
     * the outer class
     */
    @Test
    public void testInnerInner() {
        buildRule.executeTarget("src3setup");
        buildRule.executeTarget("compile");

        FileUtilities.rollbackTimestamps(new File(buildRule.getProject().getProperty("tempsrc.dir")), 5);
        FileUtilities.rollbackTimestamps(new File(buildRule.getProject().getProperty("classes.dir")), 5);

        buildRule.executeTarget("testinnerinner");
        assertEquals("Depend did not leave correct number of files", 0, getResultFiles().size());
    }

    /**
     * Test that an exception is thrown when there is no source
     */
    @Test
    public void testNoSource() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("srcdir attribute must be set");
        buildRule.executeTarget("testnosource");
    }

    /**
     * Test that an exception is thrown when the source attribute is empty
     */
    @Test
    public void testEmptySource() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("srcdir attribute must be non-empty");
        buildRule.executeTarget("testemptysource");
    }

    /**
     * Read the result fileset into a Hashtable
     *
     * @return a Hashtable containing the names of the files in the result
     * fileset
     */
    private Hashtable<String, String> getResultFiles() {
        FileSet resultFileSet = buildRule.getProject().getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(buildRule.getProject());
        return Arrays.stream(scanner.getIncludedFiles())
                .collect(Collectors.toMap(file -> file, file -> file, (a, b) -> b, Hashtable::new));
    }


    /**
     * Test mutual dependency between inner and outer do not cause both to be
     * deleted
     */
    @Test
    public void testInnerClosure() {
        buildRule.executeTarget("testinnerclosure");
        assertEquals("Depend did not leave correct number of files", 4,
            getResultFiles().size());
    }

    /**
     * Test the operation of the cache
     */
    @Test
    public void testCache() {
        buildRule.executeTarget("testcache");
    }

    /**
     * Test the detection and warning of non public classes
     */
    @Test
    public void testNonPublic() {
        buildRule.executeTarget("src5setup");
        buildRule.executeTarget("compile");

        FileUtilities.rollbackTimestamps(new File(buildRule.getProject().getProperty("tempsrc.dir")), 5);
        FileUtilities.rollbackTimestamps(new File(buildRule.getProject().getProperty("classes.dir")), 5);

        buildRule.executeTarget("testnonpublic");
        assertThat("Expected warning about APrivate",
                buildRule.getLog(), both(containsString("The class APrivate in file"))
                        .and(containsString("but has not been deleted because its source file could not be determined")));
    }

    /**
     * Tests that the depend task when run against a path containing a module-info.class (Java 9+ construct)
     * doesn't run into error
     */
    @Test
    public void testModuleInfo() {
        final JavaVersion atLeastJava9 = new JavaVersion();
        atLeastJava9.setAtLeast("9");
        Assume.assumeTrue("Skipping test execution since Java version is lesser than 9", atLeastJava9.eval());
        buildRule.executeTarget("testmoduleinfo");
    }

}
