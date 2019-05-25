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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HelloWorldTest {

    @Rule
    public BuildFileRule rule = new BuildFileRule();

    @Rule
    public ExpectedException tried = ExpectedException.none();

    @Before
    public void setUp() {
        // initialize Ant
        rule.configureProject("build.xml");
    }

    @Test
    public void testWithout() {
        rule.executeTarget("use.without");
        assertEquals("Message was logged but should not.", "", rule.getLog());
    }

    @Test
    public void testMessage() {
        // execute target 'use.nestedText' and expect a message
        // 'attribute-text' in the log
        rule.executeTarget("use.message");
        assertEquals(rule.getLog(), "attribute-text");
    }

    @Test
    public void testFail() {
        tried.expect(BuildException.class);
        tried.expectMessage("Fail requested.");
        // execute target 'use.fail' and expect a BuildException
        // with text 'Fail requested.'
        rule.executeTarget("use.fail");
    }

    @Test
    public void testNestedText() {
        rule.executeTarget("use.nestedText");
        assertEquals("nested-text", rule.getLog());
    }

    @Test
    public void testNestedElement() {
        rule.executeTarget("use.nestedElement");
        assertTrue(rule.getLog().contains("Nested Element 1"));
        assertTrue(rule.getLog().contains("Nested Element 2"));
    }
}
