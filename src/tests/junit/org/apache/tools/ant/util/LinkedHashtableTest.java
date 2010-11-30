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

package org.apache.tools.ant.util;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import junit.framework.TestCase;

public class LinkedHashtableTest extends TestCase {

    private static final Object K1 = new Object();
    private static final Object K2 = new Object();
    private static final Object V1 = new Object();
    private static final Object V2 = new Object();
    private Hashtable h = new LinkedHashtable();

    public void testClear() {
        h.put(K1, V1);
        h.clear();
        assertTrue(h.isEmpty());
    }

    public void testClone() {
        h.put(K1, V1);
        Hashtable h2 = (Hashtable) h.clone();
        assertTrue(h2 instanceof LinkedHashtable);
        assertTrue(h2.containsKey(K1));
    }

    public void testContainsAndPut() {
        h.put(K1, V1);
        assertTrue(h.contains(K1));
        assertTrue(h.containsKey(K1));
        assertTrue(h.containsValue(V1));
        assertFalse(h.containsKey(K2));
    }

    public void testGet() {
        assertNull(h.get(K1));
        h.put(K1, V1);
        assertSame(V1, h.get(K1));
    }

    public void testIsEmpty() {
        assertTrue(h.isEmpty());
        h.put(K1, V1);
        assertFalse(h.isEmpty());
    }

    public void testPutReturnValue() {
        assertNull(h.put(K1, V1));
        assertSame(V1, h.put(K1, V2));
    }

    public void testPutAll() {
        LinkedHashtable h2 = new LinkedHashtable();
        h.put(K1, V1);
        h2.putAll(h);
        assertTrue(h2.containsKey(K1));
    }

    public void testRemove() {
        h.put(K1, V1);
        assertSame(V1, h.remove(K1));
        assertTrue(h.isEmpty());
        assertNull(h.remove(K1));
    }

    public void testSize() {
        assertEquals(0, h.size());
        h.put(K1, V1);
        assertEquals(1, h.size());
    }

    public void testKeys() {
        multiSetup();
        assertKeys(CollectionUtils.asIterator(h.keys()));
    }

    public void testKeySet() {
        multiSetup();
        assertKeys(h.keySet().iterator());
    }

    public void testElements() {
        multiSetup();
        assertValues(CollectionUtils.asIterator(h.elements()));
    }

    public void testValues() {
        multiSetup();
        assertValues(h.values().iterator());
    }

    public void testEntrySet() {
        multiSetup();
        Iterator i = h.entrySet().iterator();
        assertTrue(i.hasNext());
        Map.Entry e = (Map.Entry) i.next();
        assertSame(K1, e.getKey());
        assertSame(V1, e.getValue());
        assertTrue(i.hasNext());
        e = (Map.Entry) i.next();
        assertSame(K2, e.getKey());
        assertSame(V2, e.getValue());
        assertFalse(i.hasNext());
    }

    private void multiSetup() {
        h.put(K1, V1);
        h.put(K2, V2);
    }

    private static void assertKeys(Iterator i) {
        assertTrue(i.hasNext());
        assertSame(K1, i.next());
        assertTrue(i.hasNext());
        assertSame(K2, i.next());
        assertFalse(i.hasNext());
    }

    private static void assertValues(Iterator i) {
        assertTrue(i.hasNext());
        assertSame(V1, i.next());
        assertTrue(i.hasNext());
        assertSame(V2, i.next());
        assertFalse(i.hasNext());
    }
}
