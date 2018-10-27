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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

public class RenameTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/rename.xml");
    }

    @Test
    public void test1() {
        try {
            buildRule.executeTarget("test1");
            fail("BuildException should have been thrown:  required argument missing");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }
    @Test
    public void test2() {
        try {
            buildRule.executeTarget("test2");
            fail("BuildException should have been thrown: required argument missing");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }
    @Test
    public void test3() {
        try {
            buildRule.executeTarget("test3");
            fail("BuildException should have been thrown: required argument missing");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    @Ignore("Previously commented out")
    public void test4() {
        try {
            buildRule.executeTarget("test4");
            fail("BuildException should have been thrown: source and destination the same");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    @Ignore("Previously commented out")
    public void test5() {
        buildRule.executeTarget("test5");
    }
    @Test
    public void test6() {
        buildRule.executeTarget("test6");
    }

}
