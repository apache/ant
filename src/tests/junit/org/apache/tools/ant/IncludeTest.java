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

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test the build file inclusion using XML entities.
 *
 */
public class IncludeTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("location", hasProperty("fileName",
                containsString("build.xml"))));
        buildRule.configureProject("src/etc/testcases/core/include/including_file_parse_error/build.xml");
    }

    @Test
    public void testTaskErrorInIncluding() {
        // TODO the test breaks in IDE
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("location",
                both(hasProperty("fileName", containsString("build.xml")))
                        .and(hasProperty("lineNumber", equalTo(14)))));
        thrown.expectMessage(startsWith("Warning: Could not find file "));
        buildRule.configureProject("src/etc/testcases/core/include/including_file_task_error/build.xml");
    }

    @Test
    public void testParseErrorInIncluded() {
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("location",
                hasProperty("fileName", containsString("included_file.xml"))));
        buildRule.configureProject("src/etc/testcases/core/include/included_file_parse_error/build.xml");
    }

    @Test
    public void testTaskErrorInIncluded() {
        // TODO the test breaks in IDE
        thrown.expect(BuildException.class);
        thrown.expect(hasProperty("location",
                both(hasProperty("fileName", containsString("included_file.xml")))
                        .and(hasProperty("lineNumber", equalTo(2)))));
        thrown.expectMessage(startsWith("Warning: Could not find file "));
        buildRule.configureProject("src/etc/testcases/core/include/included_file_task_error/build.xml");
        buildRule.executeTarget("test");
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
