/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import org.apache.tools.ant.BuildFileTest;

/**
 * Test the build file inclusion using XML entities.
 *
 * @author Conor MacNeill
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

}
