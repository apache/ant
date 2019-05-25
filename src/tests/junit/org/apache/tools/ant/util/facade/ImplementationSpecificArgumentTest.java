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

package org.apache.tools.ant.util.facade;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @since Ant 1.5
 */
public class ImplementationSpecificArgumentTest {

    @Test
    public void testDependsOnImplementation() {
        ImplementationSpecificArgument ia =
            new ImplementationSpecificArgument();
        ia.setLine("A B");
        String[] parts = ia.getParts();
        assertNotNull(parts);
        assertEquals(2, parts.length);
        assertEquals("A", parts[0]);
        assertEquals("B", parts[1]);

        parts = ia.getParts(null);
        assertNotNull(parts);
        assertEquals(2, parts.length);
        assertEquals("A", parts[0]);
        assertEquals("B", parts[1]);

        ia.setImplementation("foo");
        parts = ia.getParts(null);
        assertNotNull(parts);
        assertEquals(0, parts.length);

        parts = ia.getParts("foo");
        assertNotNull(parts);
        assertEquals(2, parts.length);
        assertEquals("A", parts[0]);
        assertEquals("B", parts[1]);
    }
}
