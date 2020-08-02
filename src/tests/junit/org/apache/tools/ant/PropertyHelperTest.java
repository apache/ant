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

package org.apache.tools.ant;

import org.apache.tools.ant.property.LocalProperties;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertyHelperTest {

    @Test
    public void findsPropertyNamesSetDirectly() {
        Project p = new Project();
        p.setNewProperty("foo", "bar");
        assertTrue(p.getPropertyNames().contains("foo"));
    }

    @Test
    public void findsPropertyNamesSetForLocalProperties() {
        Project p = new Project();
        p.setNewProperty("foo", "bar");

        LocalProperties localProperties = LocalProperties.get(p);
        localProperties.enterScope();
        localProperties.addLocal("baz");
        p.setNewProperty("baz", "xyzzy");

        assertTrue(p.getPropertyNames().contains("foo"));
        assertTrue(p.getPropertyNames().contains("baz"));
        assertTrue(p.getProperties().keySet().contains("foo"));
        assertFalse(p.getProperties().keySet().contains("baz"));
        localProperties.exitScope();

        assertTrue(p.getPropertyNames().contains("foo"));
        assertFalse(p.getPropertyNames().contains("baz"));
    }
}
