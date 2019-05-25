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
package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class CharSetTest {

    @RunWith(Parameterized.class)
    public static class LegalArgumentTest {
        // requires JUnit 4.12
        @Parameterized.Parameters(name = "legal argument: |{0}|")
        public static Collection<String> data() {
            return Arrays.asList("UTF-8", "ISO-8859-1", "037", "us", "IBM500",
                    // some java.io encodings are not provided as aliases in java.nio.charset
                    // so, for backwards compatibility, the case should not matter
                    "ascii", "utf-8", "Cp1252");
        }

        @Parameterized.Parameter
        public String argument;

        @Test
        public void testCorrectNames() {
            CharSet cs = new CharSet(argument);
            assertThat(argument, equalToIgnoringCase(cs.getValue()));
        }
    }

    @RunWith(Parameterized.class)
    public static class IllegalArgumentTest {
        // requires JUnit 4.12
        @Parameterized.Parameters(name = "illegal argument: |{0}|")
        public static Collection<String> data() {
            return Arrays.asList("mojibake", "dummy");
        }

        @Parameterized.Parameter
        public String argument;

        @Test(expected = BuildException.class)
        public void testNonExistentNames() {
            new CharSet(argument);
        }
    }

    @RunWith(Parameterized.class)
    public static class LegalEquivalenceTest {
        // requires JUnit 4.12
        @Parameterized.Parameters(name = "equivalent argument: |{0}|")
        public static Collection<String> data() {
            return Arrays.asList("UTF8", "unicode-1-1-utf-8");
        }

        @Parameterized.Parameter
        public String argument;

        @Test
        public void testCorrectNames() {
            assertTrue(new CharSet(argument).equivalent(CharSet.getUtf8()));
        }
    }

    @RunWith(Parameterized.class)
    public static class IncorrectEquivalenceTest {
        // requires JUnit 4.12
        @Parameterized.Parameters(name = "non-equivalent argument: |{0}|")
        public static Collection<String> data() {
            return Arrays.asList("us", "ISO-8859-1", "default");
        }

        @Parameterized.Parameter
        public String argument;

        @Test
        public void testIncorrectNames() {
            assertFalse(new CharSet(argument).equivalent(CharSet.getUtf8()));
        }
    }

}
