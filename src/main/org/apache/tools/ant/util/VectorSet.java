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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
public class VectorSet extends Vector {
    private final HashSet set = new HashSet();

    public synchronized boolean add(Object o) {
        if (set.add(o)) {
            return super.add(o);
        }
        return false;
    }

    /**
     * This implementation may not add the element at the given index
     * if it is already contained in the collection.
     */
    public void add(int index, Object o) {
        doAdd(index, o);
    }

    private synchronized void doAdd(int index, Object o) {
        // Vector.add seems to delegate to insertElementAt, but this
        // is not documented so we may better implement it ourselves
        if (set.add(o)) {
            ensureCapacity(size() + 1);
            Object[] elems = new Object[elementData.length];
            System.arraycopy(elementData, 0, elems, 0, index);
            elems[index] = o;
            System.arraycopy(elementData, index, elems, index + 1,
                             size() - index);
            elementData = elems;
            elementCount++;
        }
    }

    public void addElement(Object o) {
        add(o);
    }

    public synchronized boolean addAll(Collection c) {
        boolean changed = false;
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            changed |= add(i.next());
        }
        return changed;
    }

    /**
     * This implementation may not add all elements at the given index
     * if any of them are already contained in the collection.
     */
    public synchronized boolean addAll(int index, Collection c) {
        boolean changed = false;
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            Object o = i.next();
            boolean added = set.add(o);
            if (added) {
                super.add(index++, o);
            }
            changed |= added;
        }
        return changed;
    }

    public synchronized void clear() {
        super.clear();
        set.clear();
    }

    public Object clone() {
        VectorSet vs = (VectorSet) super.clone();
        vs.set.addAll(set);
        return vs;
    }

    public synchronized boolean contains(Object o) {
        return set.contains(o);
    }

    public synchronized boolean containsAll(Collection c) {
        return set.containsAll(c);
    }

    public void insertElementAt(Object o, int index) {
        doAdd(index, o);
    }

    public synchronized Object remove(int index) {
        Object o = get(index);
        remove(o);
        return o;
    }

    public boolean remove(Object o) {
        return doRemove(o);
    }

    private synchronized boolean doRemove(Object o) {
        // again, remove seems to delegate to removeElement, but we
        // shouldn't trust it
        if (set.remove(o)) {
            int index = indexOf(o);
            System.arraycopy(elementData, index + 1, elementData, index,
                             size() - index);
            elementCount--;
            return true;
        }
        return false;
    }

    public synchronized boolean removeAll(Collection c) {
        boolean changed = false;
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            changed |= remove(i.next());
        }
        return changed;
    }

    public synchronized void removeAllElements() {
        set.clear();
        super.removeAllElements();
    }

    public boolean removeElement(Object o) {
        return doRemove(o);
    }

    public synchronized void removeElementAt(int index) {
        remove(get(index));
    }

    public synchronized void removeRange(final int fromIndex, int toIndex) {
        while (toIndex > fromIndex) {
            remove(--toIndex);
        }
    }

    public synchronized boolean retainAll(Collection c) {
        LinkedList l = new LinkedList();
        for (Iterator i = iterator(); i.hasNext(); ) {
            Object o = i.next();
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

    public synchronized Object set(int index, Object o) {
        Object orig = get(index);
        if (set.add(o)) {
            super.set(index, o);
            set.remove(orig);
        } else {
            int oldIndexOfO = indexOf(o);
            remove(o);
            remove(orig);
            add(oldIndexOfO > index ? index : index - 1, o);
        }
        return orig;
    }

    public void setElementAt(Object o, int index) {
        set(index, o);
    }

}