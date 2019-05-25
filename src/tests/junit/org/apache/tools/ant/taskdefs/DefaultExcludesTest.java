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
import org.apache.tools.ant.DirectoryScanner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
@RunWith(Parameterized.class)
public class DefaultExcludesTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> targets() {
        return Arrays.asList(new Object[][]{
                /* default */
                {"test1", new String[]{
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
                        "**/.DS_Store"}},
                /* add */
                {"test2", new String[]{
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
                        "foo"}},
                /* subtract */
                {"test3", new String[]{
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
                        "**/.DS_Store"}}
        });
    }

    @Parameterized.Parameter
    public String targetName;

    @Parameterized.Parameter(1)
    public String[] expected;

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

    @Test
    public void test() {
        buildRule.getProject().executeTarget(targetName);
        String[] actual = DirectoryScanner.getDefaultExcludes();
        // check that both arrays have the same size
        assertEquals("current default excludes: string array length match", expected.length, actual.length);
        for (String element : expected) {
            assertTrue("current default excludes: didn't find element " + element + " in array match",
                    Arrays.asList(actual).contains(element));
        }
    }
}
