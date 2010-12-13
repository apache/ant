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

package org.apache.tools.ant.taskdefs.optional.junit;

import junit.framework.ComparisonFailure;
import junit.framework.TestCase;

/**
 *
 * @author  Marian Petras
 */
public class BatchTestTest extends TestCase {
    
    public BatchTestTest(String testName) {
        super(testName);
    }


    public void testParseTestMethodNamesList() {
        try {
            JUnitTest.parseTestMethodNamesList(null);
            fail("IllegalArgumentException expected when the param is <null>");
        } catch (IllegalArgumentException ex) {
            //this is an expected exception
        }

        assertEquals(new String[0], JUnitTest.parseTestMethodNamesList(""));
        assertEquals(new String[0], JUnitTest.parseTestMethodNamesList(" "));
        assertEquals(new String[0], JUnitTest.parseTestMethodNamesList("  "));

        checkParseCausesIAE(",");
        checkParseCausesIAE(" ,");
        checkParseCausesIAE(", ");
        checkParseCausesIAE(" , ");
        checkParseCausesIAE(",a");
        checkParseCausesIAE(" ,a");
        checkParseCausesIAE("  ,a");
        checkParseCausesIAE("  , a");
        checkParseCausesIAE("  ,a  ");
        checkParseCausesIAE("  ,a  ,");
        checkParseCausesIAE("ab,,cd");
        checkParseCausesIAE("ab, ,cd");
        checkParseCausesIAE("ab,  ,cd");
        checkParseCausesIAE("ab,  ,cd,");
        checkParseCausesIAE(",ab,  ,cd,");

        assertEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc"));
        assertEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc "));
        assertEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList(" abc"));
        assertEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList(" abc "));
        assertEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc  "));
        assertEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc,"));
        assertEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc, "));
        assertEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc ,"));
        assertEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc , "));
        assertEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList(" abc  ,"));

        /* legal Java identifiers: */
        assertEquals(new String[] {"a"}, JUnitTest.parseTestMethodNamesList("a"));
        assertEquals(new String[] {"a1"}, JUnitTest.parseTestMethodNamesList("a1"));
        assertEquals(new String[] {"a$"}, JUnitTest.parseTestMethodNamesList("a$"));
        assertEquals(new String[] {"a$1"}, JUnitTest.parseTestMethodNamesList("a$1"));
        assertEquals(new String[] {"_bc"}, JUnitTest.parseTestMethodNamesList("_bc"));
        assertEquals(new String[] {"___"}, JUnitTest.parseTestMethodNamesList("___"));

        /* illegal Java identifiers: */
        checkParseCausesIAE("1");
        checkParseCausesIAE("1a");
        checkParseCausesIAE("1ab");
        checkParseCausesIAE("1abc");
        checkParseCausesIAE("1abc d");
        checkParseCausesIAE("1abc de");
        checkParseCausesIAE("1abc def");
        checkParseCausesIAE("1abc def,");
        checkParseCausesIAE(",1abc def");

        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc,def"));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc,def,"));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc,def "));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc, def"));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc, def "));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc ,def"));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc ,def "));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc , def"));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc , def "));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc,def"));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc,def "));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc, def"));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc, def "));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc ,def"));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc ,def "));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc , def"));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc , def "));
        assertEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc , def ,"));
    }

    private static void checkParseCausesIAE(String param) {
        try {
            JUnitTest.parseTestMethodNamesList(param);
            fail("IllegalArgumentException expected when the param is \"" + param + '"');
        } catch (IllegalArgumentException ex) {
            //this is an expected exception
        }
    }

    private static void assertEquals(String[] expected, String[] actual) {
        assertEquals(null, expected, actual);
    }

    private static void assertEquals(String message,
                                     String[] expected,
                                     String[] actual) {
        if ((expected == null) && (actual == null)) {
            return;
        }
        if (expected.length != actual.length) {
            throw new ComparisonFailure(message,
                                        expected.toString(),
                                        actual.toString());
        }
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

}
