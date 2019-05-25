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

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

public class JavaResourceTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/resources/javaresource.xml");
    }

    @Test
    public void testLoadManifest() {
        buildRule.executeTarget("loadManifest");
        assertNotNull(buildRule.getProject().getProperty("manifest"));

        // this actually relies on the first manifest being found on
        // the classpath (probably rt.jar's) being valid
        assertThat(buildRule.getProject().getProperty("manifest"),
                   startsWith("Manifest-Version:"));
    }

    @Test
    public void testIsURLProvider() {
        JavaResource r = new JavaResource();
        assertSame(r, r.as(URLProvider.class));
    }

    @Test
    public void testGetURLOfManifest() {
        JavaResource r = new JavaResource();
        r.setName("META-INF/MANIFEST.MF");
        assertNotNull(r.getURL());
    }
}
