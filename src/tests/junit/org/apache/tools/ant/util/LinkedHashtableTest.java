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

package org.apache.tools.ant.util;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LinkedHashtableTest {

    private static final Object K1 = new Object();
    private static final Object K2 = new Object();
    private static final Object V1 = new Object();
    private static final Object V2 = new Object();
    private Hashtable<Object, Object> h = new LinkedHashtable<>();

    @Test
    public void testClear() {
        h.put(K1, V1);
        h.clear();
        assertTrue(h.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testClone() {
        h.put(K1, V1);
        Hashtable<Object, Object> h2 = (Hashtable<Object, Object>) h.clone();
        assertThat(h2, instanceOf(LinkedHashtable.class));
        assertThat(h2, hasKey(K1));
    }

    @Test
    public void testContainsAndPut() {
        h.put(K1, V1);
        assertTrue(h.contains(K1));
        assertThat(h, hasKey(K1));
        assertThat(h, hasValue(V1));
        assertThat(h, not(hasKey(K2)));
    }

    @Test
    public void testGet() {
        assertNull(h.get(K1));
        h.put(K1, V1);
        assertSame(V1, h.get(K1));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(h.isEmpty());
        h.put(K1, V1);
        assertFalse(h.isEmpty());
    }

    @Test
    public void testPutReturnValue() {
        assertNull(h.put(K1, V1));
        assertSame(V1, h.put(K1, V2));
    }

    @Test
    public void testPutAll() {
        LinkedHashtable<Object, Object> h2 = new LinkedHashtable<>();
        h.put(K1, V1);
        h2.putAll(h);
        assertThat(h2, hasKey(K1));
    }

    @Test
    public void testRemove() {
        h.put(K1, V1);
        assertSame(V1, h.remove(K1));
        assertTrue(h.isEmpty());
        assertNull(h.remove(K1));
    }

    @Test
    public void testSize() {
        assertEquals(0, h.size());
        h.put(K1, V1);
        assertEquals(1, h.size());
    }

    @Test
    public void testKeys() {
        multiSetup();
        assertKeys(Collections.list(h.keys()).iterator());
    }

    @Test
    public void testKeySet() {
        multiSetup();
        assertKeys(h.keySet().iterator());
    }

    @Test
    public void testElements() {
        multiSetup();
        assertValues(Collections.list(h.elements()).iterator());
    }

    @Test
    public void testValues() {
        multiSetup();
        assertValues(h.values().iterator());
    }

    @Test
    public void testEntrySet() {
        multiSetup();
        Iterator<Map.Entry<Object, Object>> i = h.entrySet().iterator();
        assertTrue(i.hasNext());
        Map.Entry<Object, Object> e = i.next();
        assertSame(K1, e.getKey());
        assertSame(V1, e.getValue());
        assertTrue(i.hasNext());
        e = i.next();
        assertSame(K2, e.getKey());
        assertSame(V2, e.getValue());
        assertFalse(i.hasNext());
    }

    private void multiSetup() {
        h.put(K1, V1);
        h.put(K2, V2);
    }

    private static void assertKeys(Iterator<Object> i) {
        assertTrue(i.hasNext());
        assertSame(K1, i.next());
        assertTrue(i.hasNext());
        assertSame(K2, i.next());
        assertFalse(i.hasNext());
    }

    private static void assertValues(Iterator<Object> i) {
        assertTrue(i.hasNext());
        assertSame(V1, i.next());
        assertTrue(i.hasNext());
        assertSame(V2, i.next());
        assertFalse(i.hasNext());
    }
}
