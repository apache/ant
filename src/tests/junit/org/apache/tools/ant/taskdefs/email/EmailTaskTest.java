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

package org.apache.tools.ant.taskdefs.email;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * TODO : develop these testcases - the email task needs to have attributes allowing
 * to simulate sending mail and to catch the output in text files or streams
 */
public class EmailTaskTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/email/mail.xml");
    }

    @Test
    public void test1() {
        try {
            buildRule.executeTarget("test1");
            fail("Build exception expected: SMTP auth only possibly with MIME mail");
        } catch(BuildException ex) {
            //TODO assert exception message
        }
    }

    @Test
    public void test2() {
        try {
            buildRule.executeTarget("test2");
            fail("Build exception expected: SSL only possibly with MIME mail");
        } catch(BuildException ex) {
            //TODO assert exception message
        }
    }

}
