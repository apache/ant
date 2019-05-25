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


package org.apache.tools.ant;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * class to look at how we expand properties
 */
@RunWith(Parameterized.class)
public class PropertyExpansionTest {

    @Parameterized.Parameters(name = "expand \"{0}\" => \"{1}\"")
    public static Collection<Object[]> targets() {
        return Arrays.asList(new Object [][] {
                /* property expansion */
                {"", ""},
                {"$", "$"},
                {"$$-", "$-"},
                {"$$", "$"},
                {"a${expanded}b", "aEXPANDEDb"},
                {"${expanded}${expanded}", "EXPANDEDEXPANDED"},
                {"$$$", "$$"},
                {"$$$$-", "$$-"},
                {"", ""},
                {"Class$$subclass", "Class$subclass"},
                /* dollar passthrough */
                {"$-", "$-"},
                {"Class$subclass", "Class$subclass"},
                {"$$$-", "$$-"},
                {"$$$$$", "$$$"},
                {"${unassigned.property}", "${unassigned.property}"},
                {"a$b", "a$b"},
                {"$}}", "$}}"}
                /* old things
                {"Class$subclass", "Classsubclass"},
                {"$$$-", "$-"},
                {"a$b", "ab"},
                {"$}}", "}}"},
                */
        });
    }

    @Parameterized.Parameter
    public String input;

    @Parameterized.Parameter(1)
    public String expected;

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    /**
     * we bind to an existing test file because we are too lazy to write our
     * own, and we don't really care what it is
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/core/immutable.xml");
        buildRule.getProject().setProperty("expanded", "EXPANDED");
    }

    /**
     * the test itself
     */
    @Test
    public void test() {
        assertEquals(input, expected, buildRule.getProject().replaceProperties(input));
    }

}
