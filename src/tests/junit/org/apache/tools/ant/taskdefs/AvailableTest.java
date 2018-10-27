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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * JUnit test for the Available task/condition.
 */
public class AvailableTest {

    
    @Rule
    public BuildFileRule buildRule = new BuildFileRule();
    
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/available.xml");
        buildRule.executeTarget("setUp");
    }

    // Nothing specified -> Fail
    @Test
    public void test1() {
        try {
            buildRule.executeTarget("test1");
            fail("Required argument not specified");
        } catch (BuildException ex) {
            //TODO assert exception message
        }
    }

    // Only property specified -> Fail
    @Test
    public void test2() {
        try {
            buildRule.executeTarget("test2");
            fail("Required argument not specified");
        } catch (BuildException ex) {
            //TODO assert exception message
        }
    }

    // Only file specified -> Fail
    @Test
    public void test3() {
        try {
            buildRule.executeTarget("test3");
            fail("Required argument not specified");
        } catch (BuildException ex) {
            //TODO assert exception message
        }
    }

    // file doesn't exist -> property 'test' == null
    @Test
    public void test4() {
        buildRule.executeTarget("test4");
        assertTrue(buildRule.getProject().getProperty("test") == null);
    }

    // file does exist -> property 'test' == 'true'
    public void test5() {
        buildRule.executeTarget("test5");
        assertEquals("true", buildRule.getProject().getProperty("test"));
    }

    // resource doesn't exist -> property 'test' == null
    @Test
    public void test6() {
        buildRule.executeTarget("test6");
        assertTrue(buildRule.getProject().getProperty("test") == null);
    }

    // resource does exist -> property 'test' == 'true'
    @Test
    public void test7() {
        buildRule.executeTarget("test7");
        assertEquals("true", buildRule.getProject().getProperty("test"));
    }

    // class doesn't exist -> property 'test' == null
    @Test
    public void test8() {
        buildRule.executeTarget("test8");
        assertTrue(buildRule.getProject().getProperty("test") == null);
    }

    // class does exist -> property 'test' == 'true'
    @Test
    public void test9() {
        buildRule.executeTarget("test9");
        assertEquals("true", buildRule.getProject().getProperty("test"));
    }

    // All three specified and all three exist -> true
    @Test
    public void test10() {
        buildRule.executeTarget("test10");
        assertEquals("true", buildRule.getProject().getProperty("test"));
    }

    // All three specified but class missing -> null
    @Test
    public void test11() {
        buildRule.executeTarget("test11");
        assertNull(buildRule.getProject().getProperty("test"));
    }

    // Specified property-name is "" -> true
    @Test
    public void test12() {
        buildRule.executeTarget("test12");
        assertNull(buildRule.getProject().getProperty("test"));
        assertEquals("true", buildRule.getProject().getProperty(""));
    }

    // Specified file is "" -> invalid files do not exist
    @Test
    public void test13() {
        buildRule.executeTarget("test13");
        assertNull(buildRule.getProject().getProperty("test"));
    }

    // Specified file is "" actually a directory, so it should pass
    @Test
    public void test13b() {
        buildRule.executeTarget("test13b");
        assertEquals("true", buildRule.getProject().getProperty("test"));
    }

    // Specified resource is "" -> can such a thing exist?
    /*
     * returns non null IBM JDK 1.3 Linux
     */
//    public void test14() {
//        buildRule.executeTarget("test14");
//        assertEquals(buildRule.getProject().getProperty("test"), null);
//    }

    // Specified class is "" -> can not exist
    @Test
    public void test15() {
        buildRule.executeTarget("test15");
        assertNull(buildRule.getProject().getProperty("test"));
    }

    // Specified dir is "" -> this is the current directory and should
    // always exist
    @Test
    public void test16() {
        buildRule.executeTarget("test16");
        assertEquals("true", buildRule.getProject().getProperty("test"));
    }

    // Specified dir is "../taskdefs" -> should exist since it's the
    // location of the buildfile used...
    @Test
    public void test17() {
        buildRule.executeTarget("test17");
        assertEquals("true", buildRule.getProject().getProperty("test"));
    }

    // Specified dir is "../this_dir_should_never_exist" -> null
    @Test
    public void test18() {
        buildRule.executeTarget("test18");
        assertNull(buildRule.getProject().getProperty("test"));
    }

    // Invalid type specified
    @Test
    public void test19() {
        try {
            buildRule.executeTarget("test19");
            fail("Invalid value for type attribute");
        } catch (BuildException ex) {
            //TODO assert exception message
        }
    }

    // Core class that exists in system classpath is ignored
    @Test
    public void test20() {
        buildRule.executeTarget("test20");
        assertNull(buildRule.getProject().getProperty("test"));
    }

    // Core class that exists in system classpath is ignored, but found in specified classpath
    @Test
    public void test21() {
        buildRule.executeTarget("test21");
        assertEquals("true", buildRule.getProject().getProperty("test"));
    }

    // Core class that exists in system classpath is not ignored with ignoresystemclass="false"
    @Test
    public void test22() {
        buildRule.executeTarget("test22");
        assertEquals("true", buildRule.getProject().getProperty("test"));
    }

    // Core class that exists in system classpath is not ignored with default ignoresystemclasses value
    @Test
    public void test23() {
        buildRule.executeTarget("test23");
        assertEquals("true", buildRule.getProject().getProperty("test"));
    }

    // Class is found in specified classpath
    @Test
    public void test24() {
        buildRule.executeTarget("test24");
        assertEquals("true", buildRule.getProject().getProperty("test"));
    }

    // File is not found in specified filepath
    @Test
    public void testSearchInPathNotThere() {
        buildRule.executeTarget("searchInPathNotThere");
        assertNull(buildRule.getProject().getProperty("test"));
    }

    // File is not found in specified filepath
    @Test
    public void testSearchInPathIsThere() {
        buildRule.executeTarget("searchInPathIsThere");
        assertEquals("true", buildRule.getProject().getProperty("test"));
    }

    // test when file begins with basedir twice
    @Test
    public void testDoubleBasedir() {
        buildRule.executeTarget("testDoubleBasedir");
    }

    // test for searching parents
    @Test
    public void testSearchParents() {
        buildRule.executeTarget("search-parents");
    }
    // test for not searching parents
    @Test
    public void testSearchParentsNot() {
        buildRule.executeTarget("search-parents-not");
    }
}
