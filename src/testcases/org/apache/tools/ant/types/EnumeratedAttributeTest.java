/*
 * Copyright  2000-2001,2004 Apache Software Foundation
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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

/**
 * JUnit 3 testcases for org.apache.tools.ant.EnumeratedAttribute.
 *
 * @author Stefan Bodewig
 */

public class EnumeratedAttributeTest extends TestCase {

    private static String[] expected = {"a", "b", "c"};

    public EnumeratedAttributeTest(String name) {
        super(name);
    }

    public void testContains() {
        EnumeratedAttribute t1 = new TestNormal();
        for (int i=0; i<expected.length; i++) {
            assertTrue(expected[i]+" is in TestNormal",
                   t1.containsValue(expected[i]));
            assertTrue(expected[i].toUpperCase()+" is in TestNormal",
                   !t1.containsValue(expected[i].toUpperCase()));
        }
        assertTrue("TestNormal doesn\'t have \"d\" attribute",
               !t1.containsValue("d"));
        assertTrue("TestNull doesn\'t have \"d\" attribute and doesn\'t die",
               !(new TestNull()).containsValue("d"));
    }

    public void testExceptions() {
        EnumeratedAttribute t1 = new TestNormal();
        for (int i=0; i<expected.length; i++) {
            try {
                t1.setValue(expected[i]);
            } catch (BuildException be) {
                fail("unexpected exception for value "+expected[i]);
            }
        }
        try {
            t1.setValue("d");
            fail("expected exception for value \"d\"");
        } catch (BuildException be) {
        }
        try {
            (new TestNull()).setValue("d");
            fail("expected exception for value \"d\" in TestNull");
        } catch (BuildException be) {
        } catch (Throwable other) {
            fail("unexpected death of TestNull: "+other.getMessage());
        }
    }

    public static class TestNormal extends EnumeratedAttribute {
        public String[] getValues() {
            return expected;
        }
    }

    public static class TestNull extends EnumeratedAttribute {
        public String[] getValues() {
            return null;
        }
    }
}
