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

import org.apache.tools.ant.BuildFileTest;

/**
 */
public class LoadPropertiesTest extends BuildFileTest {

    public LoadPropertiesTest(String name) {
        super(name);
    }

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/loadproperties.xml");
    }

    /**
     * The teardown method for JUnit
     */
    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testPrefixedProperties() {
        executeTarget("testPrefixedProperties");
        String url = project.getProperty("server1.http.url");
        assertEquals("http://localhost:80", url);
    }

    public void testPropertiesFromResource() {
        executeTarget("testPropertiesFromResource");
        executeTarget("loadPropertiesCheck");
    }

    public void testPropertiesFromFileSet() {
        executeTarget("testPropertiesFromFileSet");
        executeTarget("loadPropertiesCheck");
    }
}
