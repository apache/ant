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

/**
 * JUnit test for the Available task/condition.
 */
public class AvailableTest extends BuildFileTest {

    public AvailableTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/available.xml");
    }

    // Nothing specified -> Fail
    public void test1() {
        expectBuildException("test1", "required argument not specified");
    }

    // Only property specified -> Fail
    public void test2() {
        expectBuildException("test2", "required argument not specified");
    }

    // Only file specified -> Fail
    public void test3() {
        expectBuildException("test3", "required argument not specified");
    }

    // file doesn't exist -> property 'test' == null
    public void test4() {
        executeTarget("test4");
        assertTrue(project.getProperty("test") == null);
    }

    // file does exist -> property 'test' == 'true'
    public void test5() {
        executeTarget("test5");
        assertEquals("true", project.getProperty("test"));
    }

    // resource doesn't exist -> property 'test' == null
    public void test6() {
        executeTarget("test6");
        assertTrue(project.getProperty("test") == null);
    }

    // resource does exist -> property 'test' == 'true'
    public void test7() {
        executeTarget("test7");
        assertEquals("true", project.getProperty("test"));
    }

    // class doesn't exist -> property 'test' == null
    public void test8() {
        executeTarget("test8");
        assertTrue(project.getProperty("test") == null);
    }

    // class does exist -> property 'test' == 'true'
    public void test9() {
        executeTarget("test9");
        assertEquals("true", project.getProperty("test"));
    }

    // All three specified and all three exist -> true
    public void test10() {
        executeTarget("test10");
        assertEquals("true", project.getProperty("test"));
    }

    // All three specified but class missing -> null
    public void test11() {
        executeTarget("test11");
        assertNull(project.getProperty("test"));
    }

    // Specified property-name is "" -> true
    public void test12() {
        executeTarget("test12");
        assertNull(project.getProperty("test"));
        assertEquals("true", project.getProperty(""));
    }

    // Specified file is "" -> invalid files do not exist
    public void test13() {
        executeTarget("test13");
        assertNull(project.getProperty("test"));
    }

    // Specified file is "" actually a directory, so it should pass
    public void test13b() {
        executeTarget("test13b");
        assertEquals("true", project.getProperty("test"));
    }

    // Specified resource is "" -> can such a thing exist?
    /*
     * returns non null IBM JDK 1.3 Linux
     */
//    public void test14() {
//        executeTarget("test14");
//        assertEquals(project.getProperty("test"), null);
//    }

    // Specified class is "" -> can not exist
    public void test15() {
        executeTarget("test15");
        assertNull(project.getProperty("test"));
    }

    // Specified dir is "" -> this is the current directory and should
    // always exist
    public void test16() {
        executeTarget("test16");
        assertEquals("true", project.getProperty("test"));
    }

    // Specified dir is "../taskdefs" -> should exist since it's the
    // location of the buildfile used...
    public void test17() {
        executeTarget("test17");
        assertEquals("true", project.getProperty("test"));
    }

    // Specified dir is "../this_dir_should_never_exist" -> null
    public void test18() {
        executeTarget("test18");
        assertNull(project.getProperty("test"));
    }

    // Invalid type specified
    public void test19() {
        expectBuildException("test19", "Invalid value for type attribute.");
    }

    // Core class that exists in system classpath is ignored
    public void test20() {
        executeTarget("test20");
        assertNull(project.getProperty("test"));
    }

    // Core class that exists in system classpath is ignored, but found in specified classpath
    public void test21() {
        executeTarget("test21");
        assertEquals("true", project.getProperty("test"));
    }

    // Core class that exists in system classpath is not ignored with ignoresystemclass="false"
    public void test22() {
        executeTarget("test22");
        assertEquals("true", project.getProperty("test"));
    }

    // Core class that exists in system classpath is not ignored with default ignoresystemclasses value
    public void test23() {
        executeTarget("test23");
        assertEquals("true", project.getProperty("test"));
    }

    // Class is found in specified classpath
    public void test24() {
        executeTarget("test24");
        assertEquals("true", project.getProperty("test"));
    }

    // File is not found in specified filepath
    public void testSearchInPathNotThere() {
        executeTarget("searchInPathNotThere");
        assertNull(project.getProperty("test"));
    }

    // File is not found in specified filepath
    public void testSearchInPathIsThere() {
        executeTarget("searchInPathIsThere");
        assertEquals("true", project.getProperty("test"));
    }

    // test when file begins with basedir twice
    public void testDoubleBasedir() {
        executeTarget("testDoubleBasedir");
    }

    // test for searching parents
    public void testSearchParents() {
        executeTarget("search-parents");
    }
    // test for not searching parents
    public void testSearchParentsNot() {
        executeTarget("search-parents-not");
    }
}
