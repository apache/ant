/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 */
public class PropertyTest {
	
	@Rule
	public BuildFileRule buildRule = new BuildFileRule();

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/property.xml");
        buildRule.executeTarget("setUp");
    }

    @Test
    public void test1() {
        // should get no output at all
    	buildRule.executeTarget("test1");
    	assertEquals("System output should have been empty", "", buildRule.getOutput());
    	assertEquals("System error should have been empty", "", buildRule.getError());
    }

    @Test
    public void test2() {
    	buildRule.executeTarget("test2");
        assertContains("testprop1=aa, testprop3=xxyy, testprop4=aazz", buildRule.getLog());
    }

    @Test
    public void test3() {
        try {
            buildRule.executeTarget("test3");
            fail("Did not throw exception on circular exception");
        }
        catch (BuildException e) {
            assertTrue("Circular definition not detected - ",
                     e.getMessage().indexOf("was circularly defined") != -1);
        }
        
    }

    @Test
    public void test4() {
    	buildRule.executeTarget("test4");
    	assertContains("http.url is http://localhost:999", buildRule.getLog());
    }

    @Test
    public void test5() {
        String baseDir = buildRule.getProject().getProperty("basedir");
    	String uri = FILE_UTILS.toURI(baseDir + "/property3.properties");
        buildRule.getProject().setNewProperty("test5.url", uri);
        
        buildRule.executeTarget("test5");
        assertContains("http.url is http://localhost:999", buildRule.getLog());
    }

    @Test
    public void testPrefixSuccess() {
        buildRule.executeTarget("prefix.success");
        assertEquals("80", buildRule.getProject().getProperty("server1.http.port"));
    }

    @Test
    public void testPrefixFailure() {
       try {
            buildRule.executeTarget("prefix.fail");
            fail("Did not throw exception on invalid use of prefix");
        }
        catch (BuildException e) {
            assertContains("Prefix allowed on non-resource/file load - ", 
                     "Prefix is only valid", e.getMessage());
        }
    }

    @Test
    public void testCircularReference() {
        try {
            buildRule.executeTarget("testCircularReference");
            fail("Did not throw exception on circular exception");
        } catch (BuildException e) {
            assertContains("Circular definition not detected - ",
                         "was circularly defined", e.getMessage());
        }
    }

    @Test
    public void testThisIsNotACircularReference() {
    	buildRule.executeTarget("thisIsNotACircularReference");
        assertContains("b is A/A/A", buildRule.getLog());
    }
    
    @Test
    public void testXmlProperty() {
        try {
            Class.forName("java.lang.Iterable");
        } catch (ClassNotFoundException e) {
        	Assume.assumeNoException("XML Loading only on Java 5+", e);
        }
        buildRule.executeTarget("testXmlProperty");
        assertEquals("ONE", buildRule.getProject().getProperty("xml.one"));
        assertEquals("TWO", buildRule.getProject().getProperty("xml.two"));
        
    }

}
