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

package org.example.junit;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

public class JUnit4Skippable {

    @Test
    public void passingTest() {
        assertTrue("This test passed", true);
    }

    @Ignore("Please don't ignore me!")
    @Test
    public void explicitIgnoreTest() {
        fail("This test should be skipped");
    }

    @Test
    public void implicitlyIgnoreTest() {
        assumeFalse("This test will be ignored", true);
        fail("I told you, this test should have been ignored!");
    }

    @Test
    @Ignore
    public void explicitlyIgnoreTestNoMessage() {
        fail("This test should be skipped");
    }

    @Test
    public void implicitlyIgnoreTestNoMessage() {
        assumeFalse(true);
        fail("I told you, this test should have been ignored!");
    }

    @Test
    public void failingTest() {
        fail("I told you this test was going to fail");
    }

    @Test
    public void failingTestNoMessage() {
        fail();
    }

    @Test
    public void errorTest() {
        throw new RuntimeException("Whoops, this test went wrong");
    }

}
