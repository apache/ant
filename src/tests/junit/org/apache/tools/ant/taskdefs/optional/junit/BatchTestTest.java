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


import static org.junit.Assert.fail;
import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/**
 *
 * @author  Marian Petras
 */
public class BatchTestTest {
    
	@Test
    public void testParseTestMethodNamesList() {
        try {
            JUnitTest.parseTestMethodNamesList(null);
            fail("IllegalArgumentException expected when the param is <null>");
        } catch (IllegalArgumentException ex) {
            //this is an expected exception
        }

        assertArrayEquals(new String[0], JUnitTest.parseTestMethodNamesList(""));
        assertArrayEquals(new String[0], JUnitTest.parseTestMethodNamesList(" "));
        assertArrayEquals(new String[0], JUnitTest.parseTestMethodNamesList("  "));

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

        assertArrayEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc"));
        assertArrayEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc "));
        assertArrayEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList(" abc"));
        assertArrayEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList(" abc "));
        assertArrayEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc  "));
        assertArrayEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc,"));
        assertArrayEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc, "));
        assertArrayEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc ,"));
        assertArrayEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList("abc , "));
        assertArrayEquals(new String[] {"abc"}, JUnitTest.parseTestMethodNamesList(" abc  ,"));

        /* legal Java identifiers: */
        assertArrayEquals(new String[] {"a"}, JUnitTest.parseTestMethodNamesList("a"));
        assertArrayEquals(new String[] {"a1"}, JUnitTest.parseTestMethodNamesList("a1"));
        assertArrayEquals(new String[] {"a$"}, JUnitTest.parseTestMethodNamesList("a$"));
        assertArrayEquals(new String[] {"a$1"}, JUnitTest.parseTestMethodNamesList("a$1"));
        assertArrayEquals(new String[] {"_bc"}, JUnitTest.parseTestMethodNamesList("_bc"));
        assertArrayEquals(new String[] {"___"}, JUnitTest.parseTestMethodNamesList("___"));

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

        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc,def"));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc,def,"));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc,def "));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc, def"));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc, def "));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc ,def"));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc ,def "));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc , def"));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList("abc , def "));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc,def"));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc,def "));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc, def"));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc, def "));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc ,def"));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc ,def "));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc , def"));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc , def "));
        assertArrayEquals(new String[] {"abc", "def"}, JUnitTest.parseTestMethodNamesList(" abc , def ,"));
    }

    private static void checkParseCausesIAE(String param) {
        try {
            JUnitTest.parseTestMethodNamesList(param);
            fail("IllegalArgumentException expected when the param is \"" + param + '"');
        } catch (IllegalArgumentException ex) {
            //this is an expected exception
        }
    }

}
