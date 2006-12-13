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

import org.apache.tools.ant.BuildFileTest;

/**
 * @created 13 January 2002
 */
public class ConditionTest extends BuildFileTest {

    /**
     * Constructor for the ConditionTest object
     *
     * @param name we dont know
     */
    public ConditionTest(String name) {
        super(name);
    }


    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/condition.xml");
    }


    /**
     * The teardown method for JUnit
     */
    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testBasic() {
       expectPropertySet("basic","basic");
    }

    public void testConditionIncomplete() {
        expectSpecificBuildException("condition-incomplete",
                                     "property attribute has been omitted",
                                     "The property attribute is required.");
    }

    public void testConditionEmpty() {
        expectSpecificBuildException("condition-empty",
                                     "no conditions",
                                     "You must nest a condition into <condition>");
    }

    public void testShortcut() {
        expectPropertySet("shortcut","shortcut","set");
    }

    public void testUnset() {
        expectPropertyUnset("dontset","dontset");
    }

    public void testSetValue() {
        expectPropertySet("setvalue","setvalue","woowoo");
    }

    public void testNegation() {
        expectPropertySet("negation","negation");
    }

    public void testNegationFalse() {
        expectPropertyUnset("negationfalse","negationfalse");
    }

    public void testNegationIncomplete() {
        expectSpecificBuildException("negationincomplete",
                                     "no conditions in <not>",
                                     "You must nest a condition into <not>");
    }

    public void testAnd() {
        expectPropertySet("and","and");
    }

    public void testAndFails() {
        expectPropertyUnset("andfails","andfails");
    }

    public void testAndIncomplete() {
        expectPropertyUnset("andincomplete","andincomplete");
    }

    public void testAndempty() {
        expectPropertySet("andempty","andempty");
    }

    public void testOr() {
        expectPropertySet("or","or");
    }

    public void testOrincomplete() {
        expectPropertySet("or","or");
    }

    public void testOrFails() {
        expectPropertyUnset("orfails","orfails");
    }

    public void testOrboth() {
        expectPropertySet("orboth","orboth");
    }

    public void testFilesmatchIdentical() {
        expectPropertySet("filesmatch-identical","filesmatch-identical");
    }


    public void testFilesmatchIncomplete() {
        expectSpecificBuildException("filesmatch-incomplete",
                                     "Missing file2 attribute",
                                     "both file1 and file2 are required in filesmatch");
    }

    public void testFilesmatchOddsizes() {
        expectPropertyUnset("filesmatch-oddsizes","filesmatch-oddsizes");
    }

    public void testFilesmatchExistence() {
        expectPropertyUnset("filesmatch-existence", "filesmatch-existence");
    }

    public void testFilesmatchDifferent() {
        expectPropertyUnset("filesmatch-different","filesmatch-different");
    }

    public void testFilesmatchMatch() {
        expectPropertySet("filesmatch-match","filesmatch-match");
    }

    public void testFilesmatchDifferentSizes() {
        expectPropertyUnset("filesmatch-different-sizes",
            "filesmatch-different-sizes");
    }

    public void testFilesmatchDifferentOnemissing() {
        expectPropertyUnset("filesmatch-different-onemissing",
            "filesmatch-different-onemissing");
    }

    public void testFilesmatchDifferentEol() {
        executeTarget("filesmatch-different-eol");
    }

    public void testFilesmatchSameEol() {
        executeTarget("filesmatch-same-eol");
    }

    public void testFilesmatchNeitherExist() {
        executeTarget("filesmatch-neitherexist");
    }

    public void testContains() {
        expectPropertySet("contains","contains");
    }


    public void testContainsDoesnt() {
        expectPropertyUnset("contains-doesnt","contains-doesnt");
    }

    public void testContainsAnycase() {
        expectPropertySet("contains-anycase","contains-anycase");
    }


    public void testContainsIncomplete1() {
        expectSpecificBuildException("contains-incomplete1",
                    "Missing contains attribute",
                    "both string and substring are required in contains");
    }

    public void testContainsIncomplete2() {
        expectSpecificBuildException("contains-incomplete2",
                    "Missing contains attribute",
                    "both string and substring are required in contains");
    }

    public void testIstrue() {
        expectPropertySet("istrue","istrue");
    }

    public void testIstrueNot() {
        expectPropertyUnset("istrue-not","istrue-not");
    }

    public void testIstrueFalse() {
        expectPropertyUnset("istrue-false","istrue-false");
    }


    public void testIstrueIncomplete1() {
        expectSpecificBuildException("istrue-incomplete",
                    "Missing attribute",
                    "Nothing to test for truth");
    }

    public void testIsfalseTrue() {
        expectPropertyUnset("isfalse-true","isfalse-true");
    }

    public void testIsfalseNot() {
        expectPropertySet("isfalse-not","isfalse-not");
    }

    public void testIsfalseFalse() {
        expectPropertySet("isfalse-false","isfalse-false");
    }


    public void testIsfalseIncomplete1() {
        expectSpecificBuildException("isfalse-incomplete",
                    "Missing attribute",
                    "Nothing to test for falsehood");
    }

    public void testElse() {
        executeTarget("testElse");
    }

    public void testResourcesmatchError() {
        expectBuildException("resourcesmatch-error",
            "should fail because no resources specified");
    }

    public void testResourcesmatchEmpty() {
        executeTarget("resourcesmatch-match-empty");
    }

    public void testResourcesmatchOne() {
        executeTarget("resourcesmatch-match-one");
    }

    public void testResourcesmatchBinary() {
        executeTarget("resourcesmatch-match-binary");
    }

    public void testResourcesmatchMultipleBinary() {
        executeTarget("resourcesmatch-match-multiple-binary");
    }

    public void testResourcesmatchDiffer() {
        executeTarget("resourcesmatch-differ");
    }

    public void testResourcesmatchText() {
        executeTarget("resourcesmatch-match-text");
    }

    public void testResourcesmatchNoneExist() {
        executeTarget("resourcesmatch-noneexist");
    }
}
