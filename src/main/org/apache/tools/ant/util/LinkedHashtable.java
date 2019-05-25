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

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Subclass of Hashtable that wraps a LinkedHashMap to provide
 * predictable iteration order.
 *
 * <p>This is not a general purpose class but has been written because
 * the protected members of {@link org.apache.tools.ant.taskdefs.Copy
 * Copy} prohibited later revisions from using a more predictable
 * collection.</p>
 *
 * <p>Methods are synchronized to keep Hashtable's contract.</p>
 *
 * @since Ant 1.8.2
 */
public class LinkedHashtable<K, V> extends Hashtable<K, V> {
    private static final long serialVersionUID = 1L;

    private final LinkedHashMap<K, V> map;

    public LinkedHashtable() {
        map = new LinkedHashMap<>();
    }

    public LinkedHashtable(int initialCapacity) {
        map = new LinkedHashMap<>(initialCapacity);
    }

    public LinkedHashtable(int initialCapacity, float loadFactor) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    public LinkedHashtable(Map<K, V> m) {
        map = new LinkedHashMap<>(m);
    }

    public synchronized void clear() {
        map.clear();
    }

    @Override
    public boolean contains(Object value) {
        return containsKey(value);
    }

    @Override
    public synchronized boolean containsKey(Object value) {
        return map.containsKey(value);
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Enumeration<V> elements() {
        return Collections.enumeration(values());
    }

    @Override
    public synchronized Set<Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public synchronized boolean equals(Object o) {
        return map.equals(o);
    }

    @Override
    public synchronized V get(Object k) {
        return map.get(k);
    }

    @Override
    public synchronized int hashCode() {
        return map.hashCode();
    }

    @Override
    public synchronized boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Enumeration<K> keys() {
        return Collections.enumeration(keySet());
    }

    @Override
    public synchronized Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public synchronized V put(K k, V v) {
        return map.put(k, v);
    }

    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public synchronized V remove(Object k) {
        return map.remove(k);
    }

    @Override
    public synchronized int size() {
        return map.size();
    }

    @Override
    public synchronized String toString() {
        return map.toString();
    }

    @Override
    public synchronized Collection<V> values() {
        return map.values();
    }
}
