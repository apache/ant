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
package org.apache.tools.ant.types.resources;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.tools.ant.types.Resource;

/**
 * Helper class for ResourceCollections to return Iterators
 * that fail on changes to the object.
 * @since Ant 1.7
 */
/*package-private*/ class FailFast implements Iterator<Resource> {
    private static final WeakHashMap<Object, Set<FailFast>> MAP = new WeakHashMap<>();

    /**
     * Invalidate any in-use Iterators from the specified Object.
     * @param o the parent Object.
     */
    static synchronized void invalidate(Object o) {
        Set<FailFast> s = MAP.get(o);
        if (s != null) {
            s.clear();
        }
    }

    private static synchronized void add(FailFast f) {
        MAP.computeIfAbsent(f.parent, k -> new HashSet<>()).add(f);
    }

    private static synchronized void remove(FailFast f) {
        Set<FailFast> s = MAP.get(f.parent);
        if (s != null) {
            s.remove(f);
        }
    }

    private static synchronized void failFast(FailFast f) {
        Set<FailFast> s = MAP.get(f.parent);
        if (!s.contains(f)) {
            throw new ConcurrentModificationException();
        }
    }

    private final Object parent;
    private Iterator<Resource> wrapped;

    /**
     * Construct a new FailFast Iterator wrapping the specified Iterator
     * and dependent upon the specified parent Object.
     * @param o the parent Object.
     * @param i the wrapped Iterator.
     */
    FailFast(Object o, Iterator<Resource> i) {
        if (o == null) {
            throw new IllegalArgumentException("parent object is null");
        }
        if (i == null) {
            throw new IllegalArgumentException("cannot wrap null iterator");
        }
        parent = o;
        if (i.hasNext()) {
            wrapped = i;
            add(this);
        }
    }

    /**
     * Fulfill the Iterator contract.
     * @return true if there are more elements.
     */
    @Override
    public boolean hasNext() {
        if (wrapped == null) {
            return false;
        }
        failFast(this);
        return wrapped.hasNext();
    }

    /**
     * Fulfill the Iterator contract.
     * @return the next element.
     * @throws NoSuchElementException if no more elements.
     */
    @Override
    public Resource next() {
        if (wrapped == null || !wrapped.hasNext()) {
            throw new NoSuchElementException();
        }
        failFast(this);
        try {
            return wrapped.next();
        } finally {
            if (!wrapped.hasNext()) {
                wrapped = null;
                remove(this);
            }
        }
    }

    /**
     * Fulfill the Iterator contract.
     * @throws UnsupportedOperationException always.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}

