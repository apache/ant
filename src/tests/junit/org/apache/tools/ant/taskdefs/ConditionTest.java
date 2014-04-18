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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ConditionTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();


    /**
     * The JUnit setup method
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/condition.xml");
    }


    /**
     * The teardown method for JUnit
     */
    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    @Test
    public void testBasic() {
       buildRule.executeTarget("basic");
       assertEquals("true", buildRule.getProject().getProperty("basic"));
    }

    @Test
    public void testConditionIncomplete() {
        try {
            buildRule.executeTarget("condition-incomplete");
            fail("BuildException should have been thrown - property attribute has been omitted");
        } catch (BuildException ex) {
            assertEquals("The property attribute is required.", ex.getMessage());
        }
    }

    @Test
    public void testConditionEmpty() {
        try {
            buildRule.executeTarget("condition-empty");
            fail("BuildException should have been thrown - no conditions");
        }  catch(BuildException ex) {
            assertEquals("You must nest a condition into <condition>", ex.getMessage());
        }
    }

    @Test
    public void testShortcut() {
        buildRule.executeTarget("shortcut");
        assertEquals("set", buildRule.getProject().getProperty("shortcut"));
    }

    @Test
    public void testUnset() {
        buildRule.executeTarget("dontset");
        assertNull(buildRule.getProject().getProperty("dontset"));
    }

    @Test
    public void testSetValue() {
        buildRule.executeTarget("setvalue");
        assertEquals("woowoo", buildRule.getProject().getProperty("setvalue"));
    }

    @Test
    public void testNegation() {
        buildRule.executeTarget("negation");
        assertEquals("true", buildRule.getProject().getProperty("negation"));
    }

    @Test
    public void testNegationFalse() {
        buildRule.executeTarget("negationfalse");
        assertNull(buildRule.getProject().getProperty("negationfalse"));
    }

    @Test
    public void testNegationIncomplete() {
        try {
            buildRule.executeTarget("negationincomplete");
            fail("BuildException should have been thrown - no conditions in <not>");
        } catch (BuildException ex) {
            assertEquals("You must nest a condition into <not>", ex.getMessage());
        }
    }

    @Test
    public void testAnd() {
        buildRule.executeTarget("and");
        assertEquals("true", buildRule.getProject().getProperty("and"));
    }

    @Test
    public void testAndFails() {
        buildRule.executeTarget("andfails");
        assertNull(buildRule.getProject().getProperty("andfails"));
    }

    @Test
    public void testAndIncomplete() {
        buildRule.executeTarget("andincomplete");
        assertNull(buildRule.getProject().getProperty("andincomplete"));
    }

    @Test
    public void testAndempty() {
        buildRule.executeTarget("andempty");
        assertEquals("true", buildRule.getProject().getProperty("andempty"));
    }

    @Test
    public void testOr() {
        buildRule.executeTarget("or");
        assertEquals("true", buildRule.getProject().getProperty("or"));
    }

    @Test
    public void testOrincomplete() {
        buildRule.executeTarget("or");
        assertEquals("true", buildRule.getProject().getProperty("or"));
    }

    @Test
    public void testOrFails() {
        buildRule.executeTarget("orfails");
        assertNull(buildRule.getProject().getProperty("orfails"));
    }

    @Test
    public void testOrboth() {
        buildRule.executeTarget("orboth");
        assertEquals("true", buildRule.getProject().getProperty("orboth"));
    }

    @Test
    public void testFilesmatchIdentical() {
        buildRule.executeTarget("filesmatch-identical");
        assertEquals("true", buildRule.getProject().getProperty("filesmatch-identical"));
    }

    @Test
    public void testFilesmatchIncomplete() {
        try {
            buildRule.executeTarget("filesmatch-incomplete");
            fail("Build exception should have been thrown - Missing file2 attirbute");
        } catch (BuildException ex) {
            assertEquals("both file1 and file2 are required in filesmatch", ex.getMessage());
        }
    }

    @Test
    public void testFilesmatchOddsizes() {
        buildRule.executeTarget("filesmatch-oddsizes");
        assertNull(buildRule.getProject().getProperty("filesmatch-oddsizes"));
    }

    @Test
    public void testFilesmatchExistence() {
        buildRule.executeTarget("filesmatch-existence");
        assertNull(buildRule.getProject().getProperty("filesmatch-existence"));
    }

    @Test
    public void testFilesmatchDifferent() {
        buildRule.executeTarget("filesmatch-different");
        assertNull(buildRule.getProject().getProperty("filesmatch-different"));
    }

    @Test
    public void testFilesmatchMatch() {
        buildRule.executeTarget("filesmatch-match");
        assertEquals("true", buildRule.getProject().getProperty("filesmatch-match"));
    }

    @Test
    public void testFilesmatchDifferentSizes() {
        buildRule.executeTarget("filesmatch-different-sizes");
        assertNull(buildRule.getProject().getProperty("filesmatch-different-sizes"));
    }

    @Test
    public void testFilesmatchDifferentOnemissing() {
        buildRule.executeTarget("filesmatch-different-onemissing");
        assertNull(buildRule.getProject().getProperty("filesmatch-different-onemissing"));
    }

    @Test
    public void testFilesmatchDifferentEol() {
        buildRule.executeTarget("filesmatch-different-eol");
    }

    @Test
    public void testFilesmatchSameEol() {
        buildRule.executeTarget("filesmatch-same-eol");
    }

    @Test
    public void testFilesmatchNeitherExist() {
        buildRule.executeTarget("filesmatch-neitherexist");
    }

    @Test
    public void testContains() {
        buildRule.executeTarget("contains");
        assertEquals("true", buildRule.getProject().getProperty("contains"));
    }

    @Test
    public void testContainsDoesnt() {
        buildRule.executeTarget("contains-doesnt");
        assertNull(buildRule.getProject().getProperty("contains-doesnt"));
    }

    @Test
    public void testContainsAnycase() {
        buildRule.executeTarget("contains-anycase");
        assertEquals("true", buildRule.getProject().getProperty("contains-anycase"));
    }

    @Test
    public void testContainsIncomplete1() {
        try {
            buildRule.executeTarget("contains-incomplete1");
            fail("BuildException should have been thrown - Missing contains attribute");
        }  catch(BuildException ex) {
            assertEquals("both string and substring are required in contains", ex.getMessage());
        }
    }

    @Test
    public void testContainsIncomplete2() {
        try {
            buildRule.executeTarget("contains-incomplete2");
            fail("BuildException should have been thrown - Missing contains attribute");
        }  catch(BuildException ex) {
            assertEquals("both string and substring are required in contains", ex.getMessage());
        }
    }

    @Test
    public void testIstrue() {
        buildRule.executeTarget("istrue");
        assertEquals("true", buildRule.getProject().getProperty("istrue"));
    }

    @Test
    public void testIstrueNot() {
        buildRule.executeTarget("istrue-not");
        assertNull(buildRule.getProject().getProperty("istrue-not"));
    }

    @Test
    public void testIstrueFalse() {
        buildRule.executeTarget("istrue-false");
        assertNull(buildRule.getProject().getProperty("istrue-false"));
    }

    @Test
    public void testIstrueIncomplete1() {
        try {
            buildRule.executeTarget("istrue-incomplete");
            fail("BuildException should have been thrown - Missing attribute");
        }  catch(BuildException ex) {
            assertEquals("Nothing to test for truth", ex.getMessage());
        }
    }

    @Test
    public void testIsfalseTrue() {
        buildRule.executeTarget("isfalse-true");
        assertNull(buildRule.getProject().getProperty("isfalse-true"));
    }

    @Test
    public void testIsfalseNot() {
        buildRule.executeTarget("isfalse-not");
        assertEquals("true", buildRule.getProject().getProperty("isfalse-not"));
    }

    @Test
    public void testIsfalseFalse() {

        buildRule.executeTarget("isfalse-false");
        assertEquals("true", buildRule.getProject().getProperty("isfalse-false"));
    }

    @Test
    public void testIsfalseIncomplete1() {
        try {
            buildRule.executeTarget("isfalse-incomplete");
            fail("BuildException should have been thrown - Missing attribute");
        }  catch(BuildException ex) {
            assertEquals("Nothing to test for falsehood", ex.getMessage());
        }
    }

    @Test
    public void testElse() {
        buildRule.executeTarget("testElse");
    }

    @Test
    public void testResourcesmatchError() {
        try {
            buildRule.executeTarget("resourcematch-error");
            fail("BuildException should have been thrown - no resources specified");
        } catch (BuildException ex) {
            //TODO assert value
        }
    }

    @Test
    public void testResourcesmatchEmpty() {
        buildRule.executeTarget("resourcesmatch-match-empty");
    }

    @Test
    public void testResourcesmatchOne() {
        buildRule.executeTarget("resourcesmatch-match-one");
    }

    @Test
    public void testResourcesmatchBinary() {
        buildRule.executeTarget("resourcesmatch-match-binary");
    }

    @Test
    public void testResourcesmatchMultipleBinary() {
        buildRule.executeTarget("resourcesmatch-match-multiple-binary");
    }

    @Test
    public void testResourcesmatchDiffer() {
        buildRule.executeTarget("resourcesmatch-differ");
    }

    @Test
    public void testResourcesmatchText() {
        buildRule.executeTarget("resourcesmatch-match-text");
    }

    @Test
    public void testResourcesmatchNoneExist() {
        buildRule.executeTarget("resourcesmatch-noneexist");
    }
}
