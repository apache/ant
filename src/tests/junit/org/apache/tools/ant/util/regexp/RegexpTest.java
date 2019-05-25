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

package org.apache.tools.ant.util.regexp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for all implementations of the Regexp interface.
 *
 */
public abstract class RegexpTest extends RegexpMatcherTest {

    private static final String test = "abcdefg-abcdefg";
    private static final String pattern = "ab([^d]*)d([^f]*)f";

    public final RegexpMatcher getImplementation() {
        return getRegexpImplementation();
    }

    public abstract Regexp getRegexpImplementation();

    @Test
    public void testSubstitution() {
        Regexp reg = (Regexp) getReg();
        reg.setPattern(pattern);
        assertTrue(reg.matches(test));
        assertEquals("abedcfg-abcdefg", reg.substitute(test, "ab\\2d\\1f",
                                                       Regexp.MATCH_DEFAULT));
    }

    @Test
    public void testReplaceFirstSubstitution() {
        Regexp reg = (Regexp) getReg();
        reg.setPattern(pattern);
        assertTrue(reg.matches(test));
        assertEquals("abedcfg-abcdefg", reg.substitute(test, "ab\\2d\\1f",
                                                       Regexp.REPLACE_FIRST));
    }

    @Test
    public void testReplaceAllSubstitution() {
        Regexp reg = (Regexp) getReg();
        reg.setPattern(pattern);
        assertTrue(reg.matches(test));
        assertEquals("abedcfg-abedcfg", reg.substitute(test, "ab\\2d\\1f",
                                                       Regexp.REPLACE_ALL));
    }
}
