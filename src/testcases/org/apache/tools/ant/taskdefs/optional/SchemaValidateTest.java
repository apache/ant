/*
 * Copyright  2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildFileTest;

/**
 * Test schema validation
 */

public class SchemaValidateTest extends BuildFileTest {

    /**
     * where tasks run
     */
    private final static String TASKDEFS_DIR =
            "src/etc/testcases/taskdefs/optional/";

    /**
     * Constructor
     *
     * @param name testname
     */
    public SchemaValidateTest(String name) {
        super(name);
    }

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject(TASKDEFS_DIR + "schemavalidate.xml");
    }

    /**
     * test with no namespace
     */
    public void testNoNamespace() throws Exception {
        executeTarget("testNoNamespace");
    }

    /**
     * add namespace awareness.
     */
    public void testNSMapping() throws Exception {
        executeTarget("testNSMapping");
    }

}
