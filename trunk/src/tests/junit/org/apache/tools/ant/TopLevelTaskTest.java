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

// This test will fail with embed, or if top-level is moved out of
// dependency - as 'echo' happens as part of configureProject stage.

/**
 * Tests for builds with tasks at the top level
 *
 * @since Ant 1.6
 */
public class TopLevelTaskTest extends BuildFileTest {

    public TopLevelTaskTest(String name) {
        super(name);
    }

    public void testNoTarget() {
        configureProject("src/etc/testcases/core/topleveltasks/notarget.xml");
        expectLog("", "Called");
    }

    public void testCalledFromTopLevelAnt() {
        configureProject("src/etc/testcases/core/topleveltasks/toplevelant.xml");
        expectLog("", "Called");
    }

    public void testCalledFromTargetLevelAnt() {
        configureProject("src/etc/testcases/core/topleveltasks/targetlevelant.xml");
        expectLog("foo", "Called");
    }
}
