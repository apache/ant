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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 */
public class JupiterSampleTest {

    private static final String message = "The quick brown fox jumps over the lazy dog";

    @BeforeAll
    static void beforeAll() {
    }

    @BeforeEach
    void beforeEach() {
    }

    @Test
    void testSucceeds() {
        System.out.println(message);
        System.out.print("<some-other-message>Hello world! <!-- some comment --></some-other-message>");
    }

    @Test
    void testFails() {
        fail("intentionally failing");
    }

    @Test
    @Disabled("intentionally skipped")
    void testSkipped() {
    }

    @Test
    @Tag("fast")
    void testMethodIncludeTagisExecuted() {
    }

    @Test
    @Tag("fast")
    void testMethodIncludeTagisExecuted2() {
    }

    @Test
    @Tag("slow")
    void testMethodIncludeTagisNotExecuted() {
    }

    @AfterEach
    void afterEach() {
    }

    @AfterAll
    static void afterAll() {
    }
}
