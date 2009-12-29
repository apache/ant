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
package org.apache.tools.ant.types.resources;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Reference;

public class ResourceListTest extends BuildFileTest {

    protected void setUp() throws Exception {
        configureProject("src/etc/testcases/types/resources/resourcelist.xml");
    }

    protected void tearDown() throws Exception {
        executeTarget("tearDown");
    }

    public void testEmptyElementWithReference() {
        ResourceList rl = new ResourceList();
        rl.setEncoding("foo");
        try {
            rl.setRefid(new Reference(getProject(), "dummyref"));
            fail("Can add reference to ResourceList with encoding attribute set.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        rl = new ResourceList();
        rl.setRefid(new Reference(getProject(), "dummyref"));
        try {
            rl.setEncoding("foo");
            fail("Can set encoding in ResourceList that is a reference");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        rl = new ResourceList();
        rl.add(new FileResource(getProject(), "."));
        try {
            rl.setRefid(new Reference(getProject(), "dummyref"));
            fail("Can add reference to ResourceList with nested resource collection.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }

        rl = new ResourceList();
        rl.setRefid(new Reference(getProject(), "dummyref"));
        try {
            rl.add(new FileResource(getProject(), "."));
            fail("Can add reference to ResourceList with nested resource collection.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }

        rl = new ResourceList();
        rl.addFilterChain(new FilterChain());
        try {
            rl.setRefid(new Reference(getProject(), "dummyref"));
            fail("Can add reference to ResourceList with nested filter chain.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }

        rl = new ResourceList();
        rl.setRefid(new Reference(getProject(), "dummyref"));
        try {
            rl.addFilterChain(new FilterChain());
            fail("Can add reference to ResourceList with nested filter chain.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }
    }

    public void testCircularReference() throws Exception {
        ResourceList rl1 = new ResourceList();
        rl1.setProject(getProject());
        rl1.setRefid(new Reference(getProject(), "foo"));

        ResourceList rl2 = new ResourceList();
        rl2.setProject(getProject());
        getProject().addReference("foo", rl2);

        Union u = new Union();
        u.add(rl1);
        u.setProject(getProject());

        rl2.add(u);

        try {
            rl2.size();
            fail("Can make ResourceList a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }
    }
}
