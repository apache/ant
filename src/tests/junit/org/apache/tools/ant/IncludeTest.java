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

import junit.framework.AssertionFailedError;

import org.apache.tools.ant.BuildFileTest;

/**
 * Test the build file inclusion using XML entities.
 *
 */
public class IncludeTest extends BuildFileTest {

    public IncludeTest(String name) {
        super(name);
    }

    public void test1() {
        configureProject("src/etc/testcases/core/include/basic/include.xml");
        expectLog("test1", "from included entity");
    }

    public void test2() {
        configureProject("src/etc/testcases/core/include/frag#ment/include.xml");
        expectLog("test1", "from included entity");
    }

    public void test3() {
        configureProject("src/etc/testcases/core/include/frag#ment/simple.xml");
        expectLog("test1", "from simple buildfile");
    }

    public void test4() {
        configureProject("src/etc/testcases/core/include/basic/relative.xml");
        expectLog("test1", "from included entity");
    }

    public void test5() {
        configureProject("src/etc/testcases/core/include/frag#ment/relative.xml");
        expectLog("test1", "from included entity");
    }

    public void testParseErrorInIncluding() {
        try {
            configureProject("src/etc/testcases/core/include/including_file_parse_error/build.xml");
            fail("should have caused a parser exception");
        } catch (BuildException e) {
            assertTrue(e.getLocation().toString()
                       + " should refer to build.xml",
                       e.getLocation().toString().indexOf("build.xml:") > -1);
        }
    }

    public void testTaskErrorInIncluding() {
        configureProject("src/etc/testcases/core/include/including_file_task_error/build.xml");
        try {
            executeTarget("test");
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

    public void testParseErrorInIncluded() {
        try {
            configureProject("src/etc/testcases/core/include/included_file_parse_error/build.xml");
            fail("should have caused a parser exception");
        } catch (BuildException e) {
            assertTrue(e.getLocation().toString()
                       + " should refer to included_file.xml",
                       e.getLocation().toString()
                       .indexOf("included_file.xml:") > -1);
        }
    }

    public void testTaskErrorInIncluded() {
        configureProject("src/etc/testcases/core/include/included_file_task_error/build.xml");
        try {
            executeTarget("test");
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

    public void testWithSpaceInclude() {
        configureProject("src/etc/testcases/core/include/with space/include.xml");
        try {
            expectLog("test1", "from included entity in 'with space'");
        } catch (Throwable t) {
            throw new AssertionFailedError(
                t.toString() + "; log=\n" + getFullLog());
        }
    }

    public void testWithSpaceSimple() {
        configureProject("src/etc/testcases/core/include/with space/simple.xml");
        expectLog("test1", "from simple buildfile in 'with space'");
    }

    public void testWithSpaceRelative() {
        configureProject("src/etc/testcases/core/include/with space/relative.xml");
        expectLog("test1", "from included entity in 'with space'");
    }

}
