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

import java.net.URL;
import java.io.File;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

/**
 * @author Conor MacNeill
 */
public class PropertyTest extends BuildFileTest {

    public PropertyTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/property.xml");
    }

    public void test1() {
        // should get no output at all
        expectOutputAndError("test1", "", "");
    }

    public void test2() {
        expectLog("test2", "testprop1=aa, testprop3=xxyy, testprop4=aazz");
    }

    public void test3() {
        try {
            executeTarget("test3");
        }
        catch (BuildException e) {
            assertEquals("Circular definition not detected - ", true,
                     e.getMessage().indexOf("was circularly defined") != -1);
            return;
        }
        fail("Did not throw exception on circular exception");
    }

    public void test4() {
        expectLog("test4", "http.url is http://localhost:999");
    }

    public void test5() {
        String baseDir = getProject().getProperty("basedir");
        try {
            String uri = FileUtils.newFileUtils().toURI(
                baseDir + "/property3.properties");
            getProject().setNewProperty(
                "test5.url", uri);
        } catch (Exception ex) {
            throw new BuildException(ex);
        }
        expectLog("test5", "http.url is http://localhost:999");
    }

    public void testPrefixSuccess() {
        executeTarget("prefix.success");
        assertEquals("80", project.getProperty("server1.http.port"));
    }

    public void testPrefixFailure() {
       try {
            executeTarget("prefix.fail");
        }
        catch (BuildException e) {
            assertEquals("Prefix allowed on non-resource/file load - ", true,
                     e.getMessage().indexOf("Prefix is only valid") != -1);
            return;
        }
        fail("Did not throw exception on invalid use of prefix");
    }

    public void testCircularReference() {
        try {
            executeTarget("testCircularReference");
        } catch (BuildException e) {
            assertEquals("Circular definition not detected - ", true,
                         e.getMessage().indexOf("was circularly defined") 
                         != -1);
            return;
        }
        fail("Did not throw exception on circular exception");
    }

    public void testThisIsNotACircularReference() {
        expectLog("thisIsNotACircularReference", "b is A/A/A");
    }

}
