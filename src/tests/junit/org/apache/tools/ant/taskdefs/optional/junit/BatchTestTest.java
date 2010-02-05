/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
