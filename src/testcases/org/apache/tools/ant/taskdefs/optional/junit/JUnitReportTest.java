/* 
 * Copyright  2002,2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import org.apache.tools.ant.BuildFileTest;

/**
 * Small testcase for the junitreporttask. 
 * First test added to reproduce an fault, still a lot to improve
 *
 * @author <a href="mailto:martijn@kruithof.xs4all.nl">Martijn Kruithof</a>
 */
public class JUnitReportTest extends BuildFileTest {

    public JUnitReportTest(String name){
        super(name);
    }

    protected void setUp() {
        configureProject("src/etc/testcases/taskdefs/optional/junitreport.xml");
    }

    protected void tearDown() {
        executeTarget("clean");
    }

    /**
     * Verifies that no empty junit-noframes.html is generated when frames
     * output is selected via the default. 
     * Needs reports1 task from junitreport.xml.
     */
    public void testNoFileJUnitNoFrames() {
        executeTarget("reports1");
        if (new File("src/etc/testcases/taskdefs/optional/junitreport/test/html/junit-noframes.html").exists()) 
        {
            fail("No file junit-noframes.html expected");
        }
    }

}

