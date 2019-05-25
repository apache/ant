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
package org.apache.tools.ant.taskdefs.optional.junit;

import junit.framework.Test;
import junit.framework.TestCase;

/**
 * validates that the suite() method works in classes that don't
 * implement Test.
 */
public class SuiteMethodTest {

    public static Test suite() {
        return new Nested("testMethod");
    }

    public static class Nested extends TestCase {
        public Nested(String name) {
            super(name);
        }

        public void testMethod() {
            assertTrue(true);
        }
    }
}
