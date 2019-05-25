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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class JavadocTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/javadoc/javadoc.xml");
    }

    // PR 38370
    @Test
    public void testDirsetPath() {
        buildRule.executeTarget("dirsetPath");
    }

    // PR 38370
    @Test
    public void testDirsetPathWithoutPackagenames() {
        buildRule.executeTarget("dirsetPathWithoutPackagenames");
    }

    // PR 38370
    @Test
    public void testNestedDirsetPath() {
        buildRule.executeTarget("nestedDirsetPath");
    }

    // PR 38370
    @Test
    public void testFilesetPath() {
        buildRule.executeTarget("filesetPath");
    }

    // PR 38370
    @Test
    public void testNestedFilesetPath() {
        buildRule.executeTarget("nestedFilesetPath");
    }

    // PR 38370
    @Test
    public void testFilelistPath() {
        buildRule.executeTarget("filelistPath");
    }

    // PR 38370
    @Test
    public void testNestedFilelistPath() {
        buildRule.executeTarget("nestedFilelistPath");
    }

    // PR 38370
    @Test
    public void testPathelementPath() {
        buildRule.executeTarget("pathelementPath");
    }

    // PR 38370
    @Test
    public void testPathelementLocationPath() {
        buildRule.executeTarget("pathelementLocationPath");
    }

    // PR 38370
    @Test
    public void testNestedSource() {
        buildRule.executeTarget("nestedSource");
    }

    // PR 38370
    @Test
    public void testNestedFilesetRef() {
        buildRule.executeTarget("nestedFilesetRef");
    }

    // PR 38370
    @Test
    public void testNestedFilesetRefInPath() {
        buildRule.executeTarget("nestedFilesetRefInPath");
    }

    @Test
    public void testNestedFilesetNoPatterns() {
        buildRule.executeTarget("nestedFilesetNoPatterns");
    }

    @Test
    public void testDoublyNestedFileset() {
        buildRule.executeTarget("doublyNestedFileset");
    }

    @Test
    public void testDoublyNestedFilesetNoPatterns() {
        buildRule.executeTarget("doublyNestedFilesetNoPatterns");
    }

    @Test
    public void testNonJavaIncludes() { // #41264
        buildRule.executeTarget("nonJavaIncludes");
    }

}
