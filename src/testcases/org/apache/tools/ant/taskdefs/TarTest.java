/*
 * Copyright  2000-2002,2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
