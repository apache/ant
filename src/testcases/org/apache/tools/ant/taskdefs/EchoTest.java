/*
 * Copyright  2000,2004 Apache Software Foundation
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
public class EchoTest extends BuildFileTest {

    public EchoTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/echo.xml");
    }

    // Output an empty String
    public void test1() {
        expectLog("test1", "");
    }

    // Output 'OUTPUT OF ECHO'
    public void test2() {
        expectLog("test2", "OUTPUT OF ECHO");
    }

    public void test3() {
        expectLog("test3", "\n"+
                              "    This \n"+
                              "    is\n"+
                              "    a \n"+
                              "    multiline\n"+
                              "    message\n"+
                              "    ");
    }
}
