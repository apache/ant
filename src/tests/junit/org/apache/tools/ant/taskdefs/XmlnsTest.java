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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Task;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class XmlnsTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/xmlns.xml");
    }

    @Test
    public void testXmlns() {
        buildRule.executeTarget("xmlns");
		assertEquals("MyTask called", buildRule.getLog());
    }

    @Test
    public void testXmlnsFile() {
        buildRule.executeTarget("xmlns.file");
		assertEquals("MyTask called", buildRule.getLog());
    }

    @Test
    public void testCore() {
        buildRule.executeTarget("core");
		assertEquals("MyTask called", buildRule.getLog());
    }

    @Test
    public void testExcluded() {
        try {
            buildRule.executeTarget("excluded");
            fail("BuildException expected: excluded uri");
        } catch (BuildException ex) {
            assertEquals("Attempt to use a reserved URI ant:notallowed", ex.getMessage());
        }
    }

    @Test
    public void testOther() {
        buildRule.executeTarget("other");
		assertEquals("a message", buildRule.getLog());
    }

    @Test
    public void testNsAttributes() {
        buildRule.executeTarget("ns.attributes");
		assertEquals("hello world", buildRule.getLog());
    }

    public static class MyTask extends Task {
        public void execute() {
            log("MyTask called");
        }
    }

}

