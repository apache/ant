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

import java.util.Collection;
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
public class LinkedHashtable extends Hashtable {
    private final LinkedHashMap map;

    public LinkedHashtable() {
        map = new LinkedHashMap();
    }

    public LinkedHashtable(int initialCapacity) {
        map = new LinkedHashMap(initialCapacity);
    }

    public LinkedHashtable(int initialCapacity, float loadFactor) {
        map = new LinkedHashMap(initialCapacity, loadFactor);
    }

    public LinkedHashtable(Map m) {
        map = new LinkedHashMap(m);
    }

    public synchronized void clear() {
        map.clear();
    }

    public boolean contains(Object value) {
        return containsKey(value);
    }

    public synchronized boolean containsKey(Object value) {
        return map.containsKey(value);
    }

    public synchronized boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public Enumeration elements() {
        return CollectionUtils.asEnumeration(values().iterator());
    }

    public synchronized Set entrySet() {
        return map.entrySet();
    }

    public synchronized boolean equals(Object o) {
        return map.equals(o);
    }

    public synchronized Object get(Object k) {
        return map.get(k);
    }

    public synchronized int hashCode() {
        return map.hashCode();
    }

    public synchronized boolean isEmpty() {
        return map.isEmpty();
    }

    public Enumeration keys() {
        return CollectionUtils.asEnumeration(keySet().iterator());
    }

    public synchronized Set keySet() {
        return map.keySet();
    }

    public synchronized Object put(Object k, Object v) {
        return map.put(k, v);
    }

    public synchronized void putAll(Map m) {
        map.putAll(m);
    }

    public synchronized Object remove(Object k) {
        return map.remove(k);
    }

    public synchronized int size() {
        return map.size();
    }

    public synchronized String toString() {
        return map.toString();
    }

    public synchronized Collection values() {
        return map.values();
    }
}
