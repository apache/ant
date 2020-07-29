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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * JUnit testcases for org.apache.tools.ant.EnumeratedAttribute.
 */
public class EnumeratedAttributeTest {

    private static String[] expected = {"a", "b", "c"};

    @Test
    public void testContains() {
        EnumeratedAttribute t1 = new TestNormal();
        for (String value : expected) {
            assertTrue(value + " is in TestNormal", t1.containsValue(value));
            assertFalse(value.toUpperCase() + " is in TestNormal",
                    t1.containsValue(value.toUpperCase()));
        }
        assertFalse("TestNormal doesn't have \"d\" attribute", t1.containsValue("d"));
        assertFalse("TestNull doesn't have \"d\" attribute and doesn't die",
                (new TestNull()).containsValue("d"));
    }

    /**
     * Expected failure due to attempt to set an illegal value
     */
    @Test(expected = BuildException.class)
    public void testFactory() {
        Factory ea = (Factory) EnumeratedAttribute.getInstance(Factory.class, "one");
        assertEquals("Factory did not set the right value.", ea.getValue(), "one");
        EnumeratedAttribute.getInstance(Factory.class, "illegal");
    }

    @Test
    public void testExceptionsNormal() {
        Arrays.stream(expected).forEach(new TestNormal()::setValue);
    }

    /**
     * Expected exception for value "d" in TestNormal
     */
    @Test(expected = BuildException.class)
    public void testExceptionNormal() {
        new TestNormal().setValue("d");
    }

    /**
     * Expected exception for value "d" in TestNull
     */
    @Test(expected = BuildException.class)
    public void testExceptionNull() {
        new TestNull().setValue("d");
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

    public static class Factory extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"one", "two", "three"};
        }
    }

}
