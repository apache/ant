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
package org.apache.tools.ant;

import static org.junit.Assert.fail;

import static org.apache.tools.ant.AntAssert.assertContains;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * created 16-Mar-2006 12:25:12
 */

public class ExtendedTaskdefTest {

	@Rule
	public BuildFileRule buildRule = new BuildFileRule();
	
	@Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/core/extended-taskdef.xml");
    }

    @After
    public void tearDown() throws Exception {
        buildRule.executeTarget("teardown");
    }

    @Test
    public void testRun() throws Exception {
    	try {
    		buildRule.executeTarget("testRun");
    		fail("BuildException should have been thrown");
    	} catch(BuildException ex) {
    		assertContains("exception thrown by the subclass", "executing the Foo task", ex.getMessage());
    	}
    }

    @Test
    public void testRun2() throws Exception {
    	try {
    		buildRule.executeTarget("testRun2");
    		fail("BuildException should have been thrown");
    	} catch(BuildException ex) {
    		assertContains("exception thrown by the subclass", "executing the Foo task", ex.getMessage());
    	}
    }

}
