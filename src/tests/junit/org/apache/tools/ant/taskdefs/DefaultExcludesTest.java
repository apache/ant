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
import org.apache.tools.ant.DirectoryScanner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class DefaultExcludesTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/defaultexcludes.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    // Output the default excludes
    @Test
    public void test1() {
        String[] expected = {
                          "**/*~",
                          "**/#*#",
                          "**/.#*",
                          "**/%*%",
                          "**/._*",
                          "**/CVS",
                          "**/CVS/**",
                          "**/.cvsignore",
                          "**/SCCS",
                          "**/SCCS/**",
                          "**/vssver.scc",
                          "**/.svn",
                          "**/.svn/**",
                          "**/.git",
                          "**/.git/**",
                          "**/.gitattributes",
                          "**/.gitignore",
                          "**/.gitmodules",
                          "**/.hg",
                          "**/.hg/**",
                          "**/.hgignore",
                          "**/.hgsub",
                          "**/.hgsubstate",
                          "**/.hgtags",
                          "**/.bzr",
                          "**/.bzr/**",
                          "**/.bzrignore",
                          "**/.DS_Store"};
        buildRule.getProject().executeTarget("test1");
        assertArrayContentsEquals("current default excludes", expected, DirectoryScanner.getDefaultExcludes());
    }

    // adding something to the excludes'
    @Test
    public void test2() {
        String[] expected = {
                          "**/*~",
                          "**/#*#",
                          "**/.#*",
                          "**/%*%",
                          "**/._*",
                          "**/CVS",
                          "**/CVS/**",
                          "**/.cvsignore",
                          "**/SCCS",
                          "**/SCCS/**",
                          "**/vssver.scc",
                          "**/.svn",
                          "**/.svn/**",
                          "**/.git",
                          "**/.git/**",
                          "**/.gitattributes",
                          "**/.gitignore",
                          "**/.gitmodules",
                          "**/.hg",
                          "**/.hg/**",
                          "**/.hgignore",
                          "**/.hgsub",
                          "**/.hgsubstate",
                          "**/.hgtags",
                          "**/.bzr",
                          "**/.bzr/**",
                          "**/.bzrignore",
                          "**/.DS_Store",
                          "foo"};
        buildRule.executeTarget("test2");
        assertArrayContentsEquals("current default excludes", expected, DirectoryScanner.getDefaultExcludes());
    }

    // removing something from the defaults
    @Test
    public void test3() {
        String[] expected = {
                          "**/*~",
                          "**/#*#",
                          "**/.#*",
                          "**/%*%",
                          "**/._*",
                          //CVS missing
                          "**/CVS/**",
                          "**/.cvsignore",
                          "**/SCCS",
                          "**/SCCS/**",
                          "**/vssver.scc",
                          "**/.svn",
                          "**/.svn/**",
                          "**/.git",
                          "**/.git/**",
                          "**/.gitattributes",
                          "**/.gitignore",
                          "**/.gitmodules",
                          "**/.hg",
                          "**/.hg/**",
                          "**/.hgignore",
                          "**/.hgsub",
                          "**/.hgsubstate",
                          "**/.hgtags",
                          "**/.bzr",
                          "**/.bzr/**",
                          "**/.bzrignore",
                          "**/.DS_Store"};
        buildRule.executeTarget("test3");
        assertArrayContentsEquals("current default excludes", expected, DirectoryScanner.getDefaultExcludes());
    }

    private void assertArrayContentsEquals(String message, String[] expected, String[] actual) {
        // check that both arrays have the same size
        assertEquals(message + " : string array length match", expected.length, actual.length);
        for (int counter=0; counter < expected.length; counter++) {
            boolean found = false;
            for (int i = 0; !found && i < actual.length; i++) {
                found |= expected[counter].equals(actual[i]);
            }
            assertTrue(message + " : didn't find element "
                    + expected[counter] + " in array match", found);
        }

    }
}
