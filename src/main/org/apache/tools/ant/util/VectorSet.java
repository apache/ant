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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

/**
 * Subclass of Vector that won't store duplicate entries and shows
 * HashSet's constant time performance characteristics for the
 * contains method.
 *
 * <p>This is not a general purpose class but has been written because
 * the protected members of {@link
 * org.apache.tools.ant.DirectoryScanner DirectoryScanner} prohibited
 * later revisions from using a more efficient collection.</p>
 *
 * <p>Methods are synchronized to keep Vector's contract.</p>
 *
 * @since Ant 1.8.0
 */
public final class VectorSet<E> extends Vector<E> {
    private static final long serialVersionUID = 1L;

    private final HashSet<E> set = new HashSet<>();

    public VectorSet() {
        super();
    }

    public VectorSet(int initialCapacity) {
        super(initialCapacity);
    }

    public VectorSet(int initialCapacity, int capacityIncrement) {
        super(initialCapacity, capacityIncrement);
    }

    public VectorSet(Collection<? extends E> c) {
        if (c != null) {
            this.addAll(c);
        }
    }

    @Override
    public synchronized boolean add(E o) {
        if (!set.contains(o)) {
            doAdd(size(), o);
            return true;
        }
        return false;
    }

    /**
     * This implementation may not add the element at the given index
     * if it is already contained in the collection.
     */
    @Override
    public void add(int index, E o) {
        doAdd(index, o);
    }

    private synchronized void doAdd(int index, E o) {
        // Vector.add seems to delegate to insertElementAt, but this
        // is not documented so we may better implement it ourselves
        if (set.add(o)) {
            int count = size();
            ensureCapacity(count + 1);
            if (index != count) {
                System.arraycopy(elementData, index, elementData, index + 1,
                                 count - index);
            }
            elementData[index] = o;
            elementCount++;
        }
    }

    @Override
    public synchronized void addElement(E o) {
        doAdd(size(), o);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> c) {
        boolean changed = false;
        for (E e : c) {
            changed |= add(e);
        }
        return changed;
    }

    /**
     * This implementation may not add all elements at the given index
     * if any of them are already contained in the collection.
     */
    @Override
    public synchronized boolean addAll(int index, Collection<? extends E> c) {
        LinkedList<E> toAdd = new LinkedList<>();
        for (E e : c) {
            if (set.add(e)) {
                toAdd.add(e);
            }
        }
        if (toAdd.isEmpty()) {
            return false;
        }
        int count = size();
        ensureCapacity(count + toAdd.size());
        if (index != count) {
            System.arraycopy(elementData, index, elementData, index + toAdd.size(),
                             count - index);
        }
        for (Object o : toAdd) {
            elementData[index++] = o;
        }
        elementCount += toAdd.size();
        return true;
    }

    @Override
    public synchronized void clear() {
        super.clear();
        set.clear();
    }

    @Override
    public Object clone() {
        @SuppressWarnings("unchecked")
        final VectorSet<E> vs = (VectorSet<E>) super.clone();
        vs.set.addAll(set);
        return vs;
    }

    @Override
    public synchronized boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        return set.containsAll(c);
    }

    @Override
    public void insertElementAt(E o, int index) {
        doAdd(index, o);
    }

    @Override
    public synchronized E remove(int index) {
        E o = get(index);
        remove(o);
        return o;
    }

    @Override
    public boolean remove(Object o) {
        return doRemove(o);
    }

    private synchronized boolean doRemove(Object o) {
        // again, remove seems to delegate to removeElement, but we
        // shouldn't trust it
        if (set.remove(o)) {
            int index = indexOf(o);
            if (index < elementData.length - 1) {
                System.arraycopy(elementData, index + 1, elementData, index,
                                 elementData.length - index - 1);
            }
            elementCount--;
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            changed |= remove(o);
        }
        return changed;
    }

    @Override
    public synchronized void removeAllElements() {
        set.clear();
        super.removeAllElements();
    }

    @Override
    public boolean removeElement(Object o) {
        return doRemove(o);
    }

    @Override
    public synchronized void removeElementAt(int index) {
        remove(get(index));
    }

    @Override
    public synchronized void removeRange(final int fromIndex, int toIndex) {
        while (toIndex > fromIndex) {
            remove(--toIndex);
        }
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        if (!(c instanceof Set)) {
            c = new HashSet<>(c);
        }
        LinkedList<E> l = new LinkedList<>();
        for (E o : this) {
            if (!c.contains(o)) {
                l.addLast(o);
            }
        }
        if (!l.isEmpty()) {
            removeAll(l);
            return true;
        }
        return false;
    }

    @Override
    public synchronized E set(int index, E o) {
        E orig = get(index);
        if (set.add(o)) {
            elementData[index] = o;
            set.remove(orig);
        } else {
            int oldIndexOfO = indexOf(o);
            remove(o);
            remove(orig);
            add(oldIndexOfO > index ? index : index - 1, o);
        }
        return orig;
    }

    @Override
    public void setElementAt(E o, int index) {
        set(index, o);
    }

}
