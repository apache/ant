/*
 * Copyright  2002,2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.junit;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * @version $Revision$
 */
public class JUnitVersionHelperTest extends TestCase {

    public JUnitVersionHelperTest(String name) {
        super(name);
    }

    public void testMyOwnName() {
        assertEquals("testMyOwnName",
                     JUnitVersionHelper.getTestCaseName(this));
    }

    public void testNonTestCaseName() {
        assertEquals("I'm a foo",
                     JUnitVersionHelper.getTestCaseName(new Foo1()));
    }

    public void testNoStringReturn() {
        assertEquals("unknown",
                     JUnitVersionHelper.getTestCaseName(new Foo2()));
    }

    public void testNoGetName() {
        assertEquals("unknown",
                     JUnitVersionHelper.getTestCaseName(new Foo3()));
    }

    public void testNameNotGetName() {
        assertEquals("I'm a foo, too",
                     JUnitVersionHelper.getTestCaseName(new Foo4()));
    }

    public void testNull() {
        assertEquals("unknown", JUnitVersionHelper.getTestCaseName(null));
    }

    public static class Foo implements Test {
        public int countTestCases() {return 0;}
        public void run(TestResult result) {}
    }

    public static class Foo1 extends Foo {
        public String getName() {return "I'm a foo";}
    }

    public static class Foo2 extends Foo {
        public int getName() {return 1;}
    }

    public static class Foo3 extends Foo {
    }

    public static class Foo4 extends Foo {
        public String name() {return "I'm a foo, too";}
    }

}
