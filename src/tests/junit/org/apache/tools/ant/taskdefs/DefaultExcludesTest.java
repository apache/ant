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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.DirectoryScanner;

/**
 */
public class DefaultExcludesTest extends BuildFileTest {

    public DefaultExcludesTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/defaultexcludes.xml");
    }

    public void tearDown() {
        project.executeTarget("cleanup");
    }

    // Output the default excludes
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
                          "**/.DS_Store"};
        project.executeTarget("test1");
        assertEquals("current default excludes", expected, DirectoryScanner.getDefaultExcludes());
    }

    // adding something to the excludes'
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
                          "**/.DS_Store",
                          "foo"};
        project.executeTarget("test2");
        assertEquals("current default excludes", expected, DirectoryScanner.getDefaultExcludes());
    }

    // removing something from the defaults
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
                          "**/.DS_Store"};
        project.executeTarget("test3");
        assertEquals("current default excludes", expected, DirectoryScanner.getDefaultExcludes());
    }
    private void assertEquals(String message, String[] expected, String[] actual) {
        // check that both arrays have the same size
        assertEquals(message + " : string array length match", expected.length, actual.length);
        for (int counter=0; counter <expected.length; counter++) {
            assertEquals(message + " : " + counter + "th element in array match", expected[counter], actual[counter]);
        }

    }
}
