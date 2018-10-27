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

import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class ProjectComponentTest {

    @Test
    public void testClone() throws CloneNotSupportedException {
        Project expectedProject = new Project();
        Location expectedLocation = new Location("foo");
        String expectedDescription = "bar";

        // use an anonymous subclass since ProjectComponent is abstract
        ProjectComponent pc = new ProjectComponent() {
            };
        pc.setProject(expectedProject);
        pc.setLocation(expectedLocation);
        pc.setDescription(expectedDescription);

        ProjectComponent cloned = (ProjectComponent) pc.clone();
        assertNotSame(pc, cloned);
        assertSame(cloned.getProject(), expectedProject);
        assertSame(cloned.getLocation(), expectedLocation);
        assertSame(cloned.getDescription(), expectedDescription);
    }
}