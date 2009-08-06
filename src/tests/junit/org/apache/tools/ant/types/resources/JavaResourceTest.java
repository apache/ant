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

import org.apache.tools.ant.BuildFileTest;

public class JavaResourceTest extends BuildFileTest {

    public JavaResourceTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/types/resources/javaresource.xml");
    }

    public void testLoadManifest() {
        executeTarget("loadManifest");
        assertNotNull(getProject().getProperty("manifest"));

        // this actually relies on the first manifest being found on
        // the classpath (probably rt.jar's) being valid
        assertTrue(getProject().getProperty("manifest")
                   .startsWith("Manifest-Version:"));
    }
}
