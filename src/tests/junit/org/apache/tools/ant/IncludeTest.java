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

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test the build file inclusion using XML entities.
 *
 */
public class IncludeTest {
	
	@Rule
	public BuildFileRule buildRule = new BuildFileRule();

    @Test
    public void test1() {
        buildRule.configureProject("src/etc/testcases/core/include/basic/include.xml");
        buildRule.executeTarget("test1");
        assertEquals("from included entity", buildRule.getLog());
    }

    @Test
    public void test2() {
        buildRule.configureProject("src/etc/testcases/core/include/frag#ment/include.xml");
        buildRule.executeTarget("test1");
        assertEquals("from included entity", buildRule.getLog());
    }

    @Test
    public void test3() {
        buildRule.configureProject("src/etc/testcases/core/include/frag#ment/simple.xml");
        buildRule.executeTarget("test1");
        assertEquals("from simple buildfile", buildRule.getLog());
    }

    @Test
    public void test4() {
        buildRule.configureProject("src/etc/testcases/core/include/basic/relative.xml");
        buildRule.executeTarget("test1");
        assertEquals("from included entity", buildRule.getLog());
    }

    @Test
    public void test5() {
        buildRule.configureProject("src/etc/testcases/core/include/frag#ment/relative.xml");
        buildRule.executeTarget("test1");
        assertEquals("from included entity", buildRule.getLog());
    }

    @Test
    public void testParseErrorInIncluding() {
        try {
            buildRule.configureProject("src/etc/testcases/core/include/including_file_parse_error/build.xml");
            fail("should have caused a parser exception");
        } catch (BuildException e) {
            assertContains(e.getLocation().toString()
                       + " should refer to build.xml",
                       "build.xml:", e.getLocation().toString());
        }
    }

    @Test
    public void testTaskErrorInIncluding() {
        buildRule.configureProject("src/etc/testcases/core/include/including_file_task_error/build.xml");
        try {
            buildRule.executeTarget("test");
            fail("should have cause a build failure");
        } catch (BuildException e) {
            assertTrue(e.getMessage()
                       + " should start with \'Warning: Could not find",
                         e.getMessage().startsWith("Warning: Could not find file "));
            assertTrue(e.getLocation().toString()
                       + " should end with build.xml:14: ",
                       e.getLocation().toString().endsWith("build.xml:14: "));
        }
    }

    @Test
    public void testParseErrorInIncluded() {
        try {
            buildRule.configureProject("src/etc/testcases/core/include/included_file_parse_error/build.xml");
            fail("should have caused a parser exception");
        } catch (BuildException e) {
            assertContains(e.getLocation().toString()
                       + " should refer to included_file.xml",
                       "included_file.xml:",
                       e.getLocation().toString());
        }
    }

    @Test
    public void testTaskErrorInIncluded() {
        buildRule.configureProject("src/etc/testcases/core/include/included_file_task_error/build.xml");
        try {
            buildRule.executeTarget("test");
            fail("should have cause a build failure");
        } catch (BuildException e) {
            assertTrue(e.getMessage()
                       + " should start with \'Warning: Could not find",
                         e.getMessage().startsWith("Warning: Could not find file "));
            assertTrue(e.getLocation().toString()
                       + " should end with included_file.xml:2: ",
                       e.getLocation().toString().endsWith("included_file.xml:2: "));
        }
    }

    @Test
    public void testWithSpaceInclude() {
        buildRule.configureProject("src/etc/testcases/core/include/with space/include.xml");
        buildRule.executeTarget("test1");
        assertEquals("from included entity in 'with space'", buildRule.getLog());
    }

    @Test
    public void testWithSpaceSimple() {
        buildRule.configureProject("src/etc/testcases/core/include/with space/simple.xml");
        buildRule.executeTarget("test1");
        assertEquals("from simple buildfile in 'with space'", buildRule.getLog());
    }

    @Test
    public void testWithSpaceRelative() {
        buildRule.configureProject("src/etc/testcases/core/include/with space/relative.xml");
        buildRule.executeTarget("test1");
        assertEquals("from included entity in 'with space'", buildRule.getLog());
    }

}
