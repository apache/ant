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
package org.example.junitlauncher.jupiter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JupiterDynamicTests {

    @TestFactory
    Collection<DynamicContainer> generateTests() {
        System.out.println("@TestFactory called on " + this);
        final DynamicTest test1 = DynamicTest.dynamicTest(
                "Dynamic test1",
                () -> {
                    System.out.println("Dynamic test1 being executed on " + this);
                    assertEquals("foo", "foo");
                });
        final DynamicTest test2 = DynamicTest.dynamicTest(
                "Dynamic test2",
                () -> {
                    System.out.println("Dynamic test2 being executed on " + this);
                    assertEquals("bar", "bar");
                });
        final List<DynamicTest> tests = new ArrayList<>();
        tests.add(test1);
        tests.add(test2);
        return Collections.singleton(DynamicContainer.dynamicContainer(
                "Dynamic test container", tests));
    }

    @BeforeEach
    void beforeEach() {
        System.out.println("@BeforeEach called on " + this);
        final boolean shouldFail = Boolean.getBoolean("junitlauncher.test.failBeforeEach");
        if (shouldFail) {
            throw new RuntimeException("Intentionally failing in @BeforeEach of " + this);
        }
    }
}
