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
package org.apache.tools.ant.types.resources;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Reference;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ResourceListTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    private ResourceList rl;

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/resources/resourcelist.xml");
        rl = new ResourceList();
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("tearDown");
    }

    @Test
    public void testEmptyElementSetEncodingThenRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        rl.setEncoding("foo");
        rl.setRefid(new Reference(buildRule.getProject(), "dummyref"));
    }

    @Test
    public void testEmptyElementSetRefidThenEncoding() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        rl.setRefid(new Reference(buildRule.getProject(), "dummyref"));
        rl.setEncoding("foo");
    }

    @Test
    public void testEmptyElementAddFileResourceThenRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        rl.add(new FileResource(buildRule.getProject(), "."));
        rl.setRefid(new Reference(buildRule.getProject(), "dummyref"));
    }

    @Test
    public void testEmptyElementAddRefidThenFileResource() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        rl.setRefid(new Reference(buildRule.getProject(), "dummyref"));
        rl.add(new FileResource(buildRule.getProject(), "."));
    }

    @Test
    public void testEmptyElementAddFilterChainThenRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        rl.addFilterChain(new FilterChain());
        rl.setRefid(new Reference(buildRule.getProject(), "dummyref"));
    }

    @Test
    public void testEmptyElementAddRefidThenFilterChain() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        rl.setRefid(new Reference(buildRule.getProject(), "dummyref"));
        rl.addFilterChain(new FilterChain());
    }

    @Test
    public void testCircularReference() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        rl.setProject(buildRule.getProject());
        rl.setRefid(new Reference(buildRule.getProject(), "foo"));

        ResourceList resourceList = new ResourceList();
        resourceList.setProject(buildRule.getProject());
        buildRule.getProject().addReference("foo", resourceList);

        Union u = new Union();
        u.add(rl);
        u.setProject(buildRule.getProject());

        resourceList.add(u);
        resourceList.size();
    }
}
