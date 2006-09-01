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

package org.apache.tools.ant.util.facade;

import junit.framework.TestCase;

/**
 * @since Ant 1.5
 */
public class FacadeTaskHelperTest extends TestCase {

    public FacadeTaskHelperTest(String name) {
        super(name);
    }

    public void testPrecedenceRules() {
        FacadeTaskHelper fth = new FacadeTaskHelper("foo");
        assertEquals("foo", fth.getImplementation());

        fth.setMagicValue("bar");
        assertEquals("bar", fth.getImplementation());

        fth = new FacadeTaskHelper("foo", "bar");
        assertEquals("bar", fth.getImplementation());

        fth = new FacadeTaskHelper("foo", null);
        assertEquals("foo", fth.getImplementation());

        fth = new FacadeTaskHelper("foo");
        fth.setMagicValue("bar");
        fth.setImplementation("baz");
        assertEquals("baz", fth.getImplementation());
    }

    public void testHasBeenSet() {
        FacadeTaskHelper fth = new FacadeTaskHelper("foo");
        assertTrue("nothing set", !fth.hasBeenSet());
        fth.setMagicValue(null);
        assertTrue("magic has not been set", !fth.hasBeenSet());
        fth.setMagicValue("foo");
        assertTrue("magic has been set", fth.hasBeenSet());
        fth.setMagicValue(null);
        assertTrue(!fth.hasBeenSet());
        fth.setImplementation("baz");
        assertTrue("set explicitly", fth.hasBeenSet());
    }
}
