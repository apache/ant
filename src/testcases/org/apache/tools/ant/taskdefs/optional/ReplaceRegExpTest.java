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
package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * JUnit Testcase for the optional replaceregexp task.
 *
 * @author Stefan Bodewig 
 * @version $Revision$
 */
public class ReplaceRegExpTest extends BuildFileTest {
    private static final String PROJECT_PATH = "src/etc/testcases/taskdefs/optional";
    public ReplaceRegExpTest(String name) {
        super(name);
    }
    
    public void setUp() { 
        configureProject(PROJECT_PATH + "/replaceregexp.xml");
    }
    
    public void tearDown() { 
        executeTarget("cleanup");
    }
    
    public void testReplace() throws IOException {
        Properties original = new Properties();
        FileInputStream propsFile = null;
        try {
            propsFile = new FileInputStream(PROJECT_PATH + "/replaceregexp.properties");
            original.load(propsFile);
        } finally {
            if (propsFile != null) {
                propsFile.close();
                propsFile = null;
            }
        }

        assertEquals("Def", original.get("OldAbc"));

        executeTarget("testReplace");

        Properties after = new Properties();
        try {
            propsFile = new FileInputStream(PROJECT_PATH + "/test.properties");
            after.load(propsFile);
        } finally {
            if (propsFile != null) {
                propsFile.close();
                propsFile = null;
            }
        }

        assertNull(after.get("OldAbc"));
        assertEquals("AbcDef", after.get("NewProp"));
    }
    // inspired by bug 22541
    public void testDirectoryDateDoesNotChange() {
        executeTarget("touchDirectory");
        File myFile = new File(PROJECT_PATH + "/" + getProject().getProperty("tmpregexp"));
        long timeStampBefore = myFile.lastModified();
        executeTarget("testDirectoryDateDoesNotChange");
        long timeStampAfter = myFile.lastModified();
        assertEquals("directory date should not change",
            timeStampBefore, timeStampAfter);
    }
    public void testDontAddNewline1() throws IOException {
        executeTarget("testDontAddNewline1");
        assertTrue("Files match",
                   FileUtils.newFileUtils()
                   .contentEquals(new File(PROJECT_PATH + "/test.properties"),
                                  new File(PROJECT_PATH + "/replaceregexp2.result.properties")));
    }

    public void testDontAddNewline2() throws IOException {
        executeTarget("testDontAddNewline2");
        assertTrue("Files match",
                   FileUtils.newFileUtils()
                   .contentEquals(new File(PROJECT_PATH + "/test.properties"),
                                  new File(PROJECT_PATH + "/replaceregexp2.result.properties")));
    }

}// ReplaceRegExpTest
