/*
 * Copyright  2002,2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs.condition;

import junit.framework.TestCase;

/**
 * Testcase for the &lt;equals&gt; condition.
 *
 * @author Stefan Bodewig
 * @version $Revision$
 */
public class EqualsTest extends TestCase {

    public EqualsTest(String name) {
        super(name);
    }

    public void testTrim() {
        Equals eq = new Equals();
        eq.setArg1("a");
        eq.setArg2(" a");
        assertTrue(!eq.eval());

        eq.setTrim(true);
        assertTrue(eq.eval());

        eq.setArg2("a\t");
        assertTrue(eq.eval());
    }

    public void testCaseSensitive() {
        Equals eq = new Equals();
        eq.setArg1("a");
        eq.setArg2("A");
        assertTrue(!eq.eval());

        eq.setCasesensitive(false);
        assertTrue(eq.eval());
    }

}
