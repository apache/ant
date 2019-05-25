/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Testcases for the &lt;isreference&gt; condition.
 *
 */
public class IsReferenceTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/conditions/isreference.xml");
    }

    @Test
    public void testBasic() {
        buildRule.executeTarget("basic");
        assertEquals("true", buildRule.getProject().getProperty("global-path"));
        assertEquals("true", buildRule.getProject().getProperty("target-path"));
        assertNull(buildRule.getProject().getProperty("undefined"));
    }

    /**
     * Expected failure due to omitted refid attribute
     */
    @Test
    public void testNotEnoughArgs() {
        thrown.expect(BuildException .class) ;
            thrown.expectMessage("No reference specified for isreference condition");
        buildRule.executeTarget("isreference-incomplete");
    }

    @Test
    public void testType() {
        buildRule.executeTarget("type");
        assertEquals("true", buildRule.getProject().getProperty("global-path"));
        assertNull(buildRule.getProject().getProperty("global-path-as-fileset"));
        assertNull(buildRule.getProject().getProperty("global-path-as-foo"));
        assertEquals("true", buildRule.getProject().getProperty("global-echo"));
    }

}
