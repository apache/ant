/*
 * Copyright  2003-2004 Apache Software Foundation
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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * @author Peter Reilly
 */
public class AntlibTest extends BuildFileTest {
    public AntlibTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/antlib.xml");
    }

    public void testAntlibFile() {
        expectLog("antlib.file", "MyTask called");
    }

    /**
     * Confirms that all matching resources will be used, so that you
     * can collect several antlibs in one Definer call.
     * @see "http://issues.apache.org/bugzilla/show_bug.cgi?id=24024"
     */
    public void testAntlibResource() {
        expectLog("antlib.resource", "MyTask called-and-then-MyTask2 called");
    }

    public void testNsCurrent() {
        expectLog("ns.current", "Echo2 inside a macroHello from x:p");
    }

    public static class MyTask extends Task {
        public void execute() {
            log("MyTask called");
        }
    }

    public static class MyTask2 extends Task {
        public void execute() {
            log("MyTask2 called");
        }
    }

}

