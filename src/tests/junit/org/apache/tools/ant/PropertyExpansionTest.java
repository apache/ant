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


package org.apache.tools.ant;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * class to look at how we expand properties
 */
public class PropertyExpansionTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();
    /**
     * we bind to an existing test file because we are too lazy to write our
     * own, and we don't really care what it is
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/core/immutable.xml");
    }

    /**
     * run through the test cases of expansion
     */
    @Test
    public void testPropertyExpansion() {
        assertExpandsTo("","");
        assertExpandsTo("$","$");
        assertExpandsTo("$$-","$-");
        assertExpandsTo("$$","$");
        buildRule.getProject().setProperty("expanded","EXPANDED");
        assertExpandsTo("a${expanded}b","aEXPANDEDb");
        assertExpandsTo("${expanded}${expanded}","EXPANDEDEXPANDED");
        assertExpandsTo("$$$","$$");
        assertExpandsTo("$$$$-","$$-");
        assertExpandsTo("","");
        assertExpandsTo("Class$$subclass","Class$subclass");
    }

    /**
     * new things we want
     */
    @Test
    public void testDollarPassthru() {
        assertExpandsTo("$-","$-");
        assertExpandsTo("Class$subclass","Class$subclass");
        assertExpandsTo("$$$-","$$-");
        assertExpandsTo("$$$$$","$$$");
        assertExpandsTo("${unassigned.property}","${unassigned.property}");
        assertExpandsTo("a$b","a$b");
        assertExpandsTo("$}}","$}}");
    }


    /**
     * old things we dont want; not a test no more
     */
    @Test
    @Ignore("Previously disabled through naming convention")
    public void oldtestQuirkyLegacyBehavior() {
        assertExpandsTo("Class$subclass","Classsubclass");
        assertExpandsTo("$$$-","$-");
        assertExpandsTo("a$b","ab");
        assertExpandsTo("$}}","}}");
    }

    /**
     * little helper method to validate stuff
     */
    private void assertExpandsTo(String source,String expected) {
        String actual = buildRule.getProject().replaceProperties(source);
        assertEquals(source,expected,actual);
    }

//end class
}
