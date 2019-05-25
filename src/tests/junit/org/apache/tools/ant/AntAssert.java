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
package org.apache.tools.ant;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Provides common assert functions for use across multiple tests, similar to the <code>Assert</code>s
 * within JUnit.
 *
 * @deprecated use assertThat() in JUnit 4.4+ in combination with containsString() matcher;
 * for exception messages, use ExpectedException rule.
 */
@Deprecated
public class AntAssert {

    /**
     * Assert that a string contains the given substring.
     * @param message the message to fail with if the substring is not present in the target string.
     * @param needle the string to search for.
     * @param haystack the string to search in.
     */
    public static void assertContains(String message, String needle, String haystack) {
        String formattedMessage = (message == null ? "" : message + " ");
        assertTrue(formattedMessage + String.format("expected message containing: <%s> but got: <%s>",
                needle, haystack), haystack.contains(needle));
    }

    /**
     * Assert that a string contains the given substring. A default failure message will be used if the target string
     * is not found.
     * @param needle the target string to search for.
     * @param haystack the string to search in.
     */
    public static void assertContains(String needle, String haystack) {
        assertContains("", needle, haystack);
    }

    /**
     * Assert that a string does not contain the given substring.
     * @param message the message to fail with if the substring is present in the target string.
     * @param needle the string to search for.
     * @param haystack the string to search in.
     */
    public static void assertNotContains(String message, String needle, String haystack) {
        String formattedMessage = (message == null ? "" : message + " ");
        assertFalse(formattedMessage + String.format("expected message not to contain: <%s> but got: <%s>",
                needle, haystack), haystack.contains(needle));
    }

    /**
     * Assert that a string does not contain the given substring. A default failure message will be used if the target
     * string is found.
     * @param needle the target string to search for.
     * @param haystack the string to search in.
     */
    public static void assertNotContains(String needle, String haystack) {
        assertNotContains("", needle, haystack);
    }


}
