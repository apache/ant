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

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author  Marian Petras
 */
@RunWith(Enclosed.class)
public class BatchTestTest {

    @RunWith(Parameterized.class)
    public static class IllegalArgumentTest {

        // requires JUnit 4.12
        @Parameters(name = "illegal argument: |{0}|")
        public static Collection<String> data() {
            return Arrays.asList(null, ",", " ,", ", ", " , ",
                    ",a", " ,a", "  ,a", "  , a", "  ,a  ", "  ,a  ,",
                    "ab,,cd", "ab, ,cd", "ab,  ,cd", "ab,  ,cd,", ",ab,  ,cd,",
                    /* illegal Java identifiers: */
                    "1", "1a", "1ab", "1abc", "1abc d", "1abc de", "1abc def", "1abc def,",
                    ",1abc def");
        }

        @Parameter
        public String argument;

        /**
         * Expected failure when the parameter is illegal
         */
        @Test(expected = IllegalArgumentException.class)
        public void testParseTestMethodNamesList() {
            JUnitTest.parseTestMethodNamesList(argument);
        }
    }

    @RunWith(Parameterized.class)
    public static class LegalArgumentTest {

        @Parameters(name = "legal argument: |{0}|")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    {"", new String[0]}, {" ", new String[0]}, {"  ", new String[0]},
                    {"abc", new String[]{"abc"}}, {"abc ", new String[]{"abc"}},
                    {" abc", new String[]{"abc"}}, {" abc ", new String[]{"abc"}},
                    {"abc  ", new String[]{"abc"}}, {"abc,", new String[]{"abc"}},
                    {"abc, ", new String[]{"abc"}}, {"abc ,", new String[]{"abc"}},
                    {"abc , ", new String[]{"abc"}}, {" abc  ,", new String[]{"abc"}},
                    /* legal Java identifiers: */
                    {"a", new String[]{"a"}}, {"a1", new String[]{"a1"}},
                    {"a$", new String[]{"a$"}}, {"a$1", new String[]{"a$1"}},
                    {"_bc", new String[]{"_bc"}}, {"___", new String[]{"___"}},
                    {"abc,def", new String[]{"abc", "def"}},
                    {"abc,def,", new String[]{"abc", "def"}},
                    {"abc,def ", new String[]{"abc", "def"}},
                    {"abc, def", new String[]{"abc", "def"}},
                    {"abc, def ", new String[]{"abc", "def"}},
                    {"abc ,def", new String[]{"abc", "def"}},
                    {"abc ,def ", new String[]{"abc", "def"}},
                    {"abc , def", new String[]{"abc", "def"}},
                    {"abc , def ", new String[]{"abc", "def"}},
                    {" abc,def", new String[]{"abc", "def"}},
                    {" abc,def ", new String[]{"abc", "def"}},
                    {" abc, def", new String[]{"abc", "def"}},
                    {" abc, def ", new String[]{"abc", "def"}},
                    {" abc ,def", new String[]{"abc", "def"}},
                    {" abc ,def ", new String[]{"abc", "def"}},
                    {" abc , def", new String[]{"abc", "def"}},
                    {" abc , def ", new String[]{"abc", "def"}},
                    {" abc , def ,", new String[]{"abc", "def"}},
            });
        }

        @Parameter
        public String argument;

        @Parameter(1)
        public String[] result;

        @Test
        public void testParseTestMethodNamesList() {
            assertArrayEquals(result, JUnitTest.parseTestMethodNamesList(argument));
       }
    }

}
