/* 
 * Copyright  2001-2004 Apache Software Foundation
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
import org.apache.tools.ant.util.FileUtils;

import java.io.IOException;

/**
 * @author Stefan Bodewig
 * @version $Revision$
 */
public class BUnzip2Test extends BuildFileTest {

    public BUnzip2Test(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/bunzip2.xml");
        executeTarget("prepare");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testRealTest() throws java.io.IOException {
        FileUtils fileUtils = FileUtils.newFileUtils();
        executeTarget("realTest");
        assertTrue("File content mismatch after bunzip2",
            fileUtils.contentEquals(project.resolveFile("expected/asf-logo-huge.tar"),
                                    project.resolveFile("asf-logo-huge.tar")));
    }
}
