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

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;

public class VectorSetTest {

    private static final Object O = new Object();
    private VectorSet<Object> v = new VectorSet<>();

    @Test
    public void testAdd() {
        assertTrue(v.add(O));
        assertFalse(v.add(O));
        assertEquals(1, v.size());
    }

    @Test
    public void testAdd2() {
        v.add(0, O);
        v.add(1, O);
        assertEquals(1, v.size());
    }

    @Test
    public void testAddElement() {
        v.addElement(O);
        v.addElement(O);
        assertEquals(1, v.size());
    }

    @Test
    public void testAddAll() {
        assertTrue(v.addAll(Arrays.asList(O, O)));
        assertEquals(1, v.size());
    }

    @Test
    public void testAddAll2() {
        assertTrue(v.addAll(0, Arrays.asList(O, O)));
        assertEquals(1, v.size());
    }

    @Test
    public void testClear() {
        v.add(O);
        v.clear();
        assertEquals(0, v.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testClone() {
        v.add(O);
        Object o = v.clone();
        assertThat(o, instanceOf(VectorSet.class));
        VectorSet<Object> vs = (VectorSet<Object>) o;
        assertEquals(1, vs.size());
        assertTrue(vs.contains(O));
    }

    @Test
    public void testContains() {
        assertFalse(v.contains(O));
        v.add(O);
        assertTrue(v.contains(O));
        assertFalse(v.contains(null));
    }

    @Test
    public void testContainsAll() {
        assertFalse(v.containsAll(Arrays.asList(O, O)));
        v.add(O);
        assertTrue(v.containsAll(Arrays.asList(O, O)));
        assertFalse(v.containsAll(Arrays.asList(O, null)));
    }

    @Test
    public void testInsertElementAt() {
        v.insertElementAt(O, 0);
        v.insertElementAt(O, 1);
        assertEquals(1, v.size());
    }

    @Test
    public void testRemoveIndex() {
        v.add(O);
        assertSame(O, v.remove(0));
        assertEquals(0, v.size());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testCantRemoveByIndexFromEmptySet() {
        v.remove(0);
    }

    @Test
    public void testRemoveObject() {
        v.add(O);
        assertTrue(v.remove(O));
        assertEquals(0, v.size());
        assertFalse(v.remove(O));
    }

    @Test
    public void testRemoveAtEndWhenSizeEqualsCapacity() {
        v = new VectorSet<>(3, 1);
        Object a = new Object();
        v.add(a);
        Object b = new Object();
        v.add(b);
        v.add(O);
        assertEquals(3, v.size());
        assertEquals(3, v.capacity());
        assertTrue(v.remove(O));
        assertEquals(2, v.size());
        assertFalse(v.remove(O));
        assertSame(a, v.elementAt(0));
        assertSame(b, v.elementAt(1));
    }

    @Test
    public void testRemoveAtFrontWhenSizeEqualsCapacity() {
        v = new VectorSet<>(3, 1);
        v.add(O);
        Object a = new Object();
        v.add(a);
        Object b = new Object();
        v.add(b);
        assertEquals(3, v.size());
        assertEquals(3, v.capacity());
        assertTrue(v.remove(O));
        assertEquals(2, v.size());
        assertFalse(v.remove(O));
        assertSame(a, v.elementAt(0));
        assertSame(b, v.elementAt(1));
    }

    @Test
    public void testRemoveInMiddleWhenSizeEqualsCapacity() {
        v = new VectorSet<>(3, 1);
        Object a = new Object();
        v.add(a);
        v.add(O);
        Object b = new Object();
        v.add(b);
        assertEquals(3, v.size());
        assertEquals(3, v.capacity());
        assertTrue(v.remove(O));
        assertEquals(2, v.size());
        assertFalse(v.remove(O));
        assertSame(a, v.elementAt(0));
        assertSame(b, v.elementAt(1));
    }

    @Test
    public void testRemoveAll() {
        v.add(O);
        assertTrue(v.removeAll(Arrays.asList(O, O)));
        assertEquals(0, v.size());
        assertFalse(v.removeAll(Arrays.asList(O, O)));
    }

    @Test
    public void testRemoveAllElements() {
        v.add(O);
        v.removeAllElements();
        assertEquals(0, v.size());
    }

    @Test
    public void testRemoveElement() {
        v.add(O);
        assertTrue(v.removeElement(O));
        assertEquals(0, v.size());
        assertFalse(v.removeElement(O));
    }

    @Test
    public void testRemoveElementAt() {
        v.add(O);
        v.removeElementAt(0);
        assertEquals(0, v.size());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testCantRemoveAtFromEmptySet() {
        v.removeElementAt(0);
    }

    @Test
    public void testRemoveRange() {
        Object a = new Object();
        Object b = new Object();
        Object c = new Object();
        v.addAll(Arrays.asList(O, a, b, c));
        v.removeRange(1, 3);
        assertEquals(2, v.size());
        assertTrue(v.contains(O));
        assertTrue(v.contains(c));
    }

    @Test
    public void testRetainAll() {
        Object a = new Object();
        Object b = new Object();
        Object c = new Object();
        v.addAll(Arrays.asList(O, a, b, c));
        assertEquals(0, v.indexOf(O));
        assertTrue(v.retainAll(Arrays.asList(c, O)));
        assertEquals(2, v.size());
        assertTrue(v.contains(O));
        assertTrue(v.contains(c));
        assertEquals(0, v.indexOf(O));
    }

    @Test
    public void testRetainAllReturnValueAndEmptiness() {
        v.add(1);
        v.add(2);
        v.add(3);
        assertTrue(v.retainAll(Arrays.asList(1, 2)));
        assertEquals(2, v.size());
        assertFalse(v.retainAll(Arrays.asList(1, 2)));
        assertEquals(2, v.size());
        assertTrue(v.retainAll(Arrays.asList(4, 5)));
        assertEquals(0, v.size());
        assertFalse(v.retainAll(Arrays.asList(4, 5)));
    }

    @Test
    public void testSet() {
        v.add(O);
        Object a = new Object();
        assertSame(O, v.set(0, a));
        assertSame(a, v.get(0));
        assertEquals(1, v.size());
    }

    @Test
    public void testSetElementAt() {
        v.add(O);
        Object a = new Object();
        v.setElementAt(a, 0);
        assertSame(a, v.get(0));
        assertEquals(1, v.size());
    }

    @Test
    public void testRetainAllSpeed() {
        int size = 50000;
        for (int i = 0; i < size; i++) {
            v.add(i);
            v.add(i);
        }
        assertEquals(size, v.size());
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = size - 4; i < 2 * size; i++) {
            list.add(i);
            v.add(i);
        }
        assertTrue(v.retainAll(list));
        assertEquals(v.toString(), size + 4, v.size());
    }

}
