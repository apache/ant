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

/**
 * @author Nico Seessle <nico@seessle.de>
 */
public class TarTest extends BuildFileTest {

    public TarTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/tar.xml");
    }

    public void test1() {
        expectBuildException("test1", "required argument not specified");
    }

    public void test2() {
        expectBuildException("test2", "required argument not specified");
    }

    public void test3() {
        expectBuildException("test3", "required argument not specified");
    }

    public void test4() {
        expectBuildException("test4", "tar cannot include itself");
    }

    public void test5() {
        executeTarget("test5");
        java.io.File f
            = new java.io.File("src/etc/testcases/taskdefs/test5.tar");

        if (!f.exists()) {
            fail("Tarring a directory failed");
        }
    }

    public void test6() {
        expectBuildException("test6", "Invalid value specified for longfile attribute.");
    }

    public void test7() {
        executeTarget("test7");
        java.io.File f1
            = new java.io.File("src/etc/testcases/taskdefs/test7-prefix");

        if (!(f1.exists() && f1.isDirectory())) {
            fail("The prefix attribute is not working properly.");
        }

        java.io.File f2
            = new java.io.File("src/etc/testcases/taskdefs/test7dir");

        if (!(f2.exists() && f2.isDirectory())) {
            fail("The prefix attribute is not working properly.");
        }
    }

    public void test8() {
        executeTarget("test8");
        java.io.File f1
            = new java.io.File("src/etc/testcases/taskdefs/test8.xml");
        if (! f1.exists()) {
            fail("The fullpath attribute or the preserveLeadingSlashes attribute does not work propertly");
        }
    }

    public void test9() {
        expectBuildException("test9", "Invalid value specified for compression attribute.");
    }

    public void test10() {
        executeTarget("test10");
        java.io.File f1
            = new java.io.File("src/etc/testcases/taskdefs/test10.xml");
        if (! f1.exists()) {
            fail("The fullpath attribute or the preserveLeadingSlashes attribute does not work propertly");
        }
    }

    public void test11() {
        executeTarget("test11");
        java.io.File f1
            = new java.io.File("src/etc/testcases/taskdefs/test11.xml");
        if (! f1.exists()) {
            fail("The fullpath attribute or the preserveLeadingSlashes attribute does not work propertly");
        }
    }


    public void tearDown() {
        executeTarget("cleanup");
    }
}
