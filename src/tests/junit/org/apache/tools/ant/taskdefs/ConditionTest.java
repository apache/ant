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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConditionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
        thrown.expect(BuildException.class);
        thrown.expectMessage("The property attribute is required.");
        buildRule.executeTarget("condition-incomplete");
    }

    @Test
    public void testConditionEmpty() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must nest a condition into <condition>");
        buildRule.executeTarget("condition-empty");
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
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must nest a condition into <not>");
        buildRule.executeTarget("negationincomplete");
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
        thrown.expect(BuildException.class);
        thrown.expectMessage("both file1 and file2 are required in filesmatch");
        buildRule.executeTarget("filesmatch-incomplete");
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
        thrown.expect(BuildException.class);
        thrown.expectMessage("both string and substring are required in contains");
        buildRule.executeTarget("contains-incomplete1");
    }

    @Test
    public void testContainsIncomplete2() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("both string and substring are required in contains");
        buildRule.executeTarget("contains-incomplete2");
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
        thrown.expect(BuildException.class);
        thrown.expectMessage("Nothing to test for truth");
        buildRule.executeTarget("istrue-incomplete");
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
        thrown.expect(BuildException.class);
        thrown.expectMessage("Nothing to test for falsehood");
        buildRule.executeTarget("isfalse-incomplete");
    }

    @Test
    public void testElse() {
        buildRule.executeTarget("testElse");
    }

    @Test(expected = BuildException.class)
    public void testResourcesmatchError() {
        buildRule.executeTarget("resourcematch-error");
        // TODO assert value
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
