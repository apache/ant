/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * @author Nico Seessle <nico@seessle.de>
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
        assertEquals("true",project.getProperty("test"));
    }

    // Core class that exists in system classpath is not ignored with ignoresystemclass="false"
    public void test22() {
        executeTarget("test22");
        assertEquals("true",project.getProperty("test"));
    }

    // Core class that exists in system classpath is not ignored with default ignoresystemclasses value
    public void test23() {
        executeTarget("test23");
        assertEquals("true",project.getProperty("test"));
    }

    // Class is found in specified classpath
    public void test24() {
        executeTarget("test24");
        assertEquals("true",project.getProperty("test"));
    }

    // File is not found in specified filepath
    public void testSearchInPathNotThere() {
        executeTarget("searchInPathNotThere");
        assertNull(project.getProperty("test"));
    }

    // File is not found in specified filepath
    public void testSearchInPathIsThere() {
        executeTarget("searchInPathIsThere");
        assertEquals("true",project.getProperty("test"));
    }
}
