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
package org.apache.tools.ant.types.resources;

import java.util.Stack;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.AbstractCollection;
import java.util.NoSuchElementException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.comparators.ResourceComparator;
import org.apache.tools.ant.types.resources.comparators.DelegatedResourceComparator;

/**
 * ResourceCollection that sorts another ResourceCollection.
 * @since Ant 1.7
 */
public class Sort extends BaseResourceCollectionWrapper {

    //sorted bag impl. borrowed from commons-collections TreeBag:
    private static class SortedBag extends AbstractCollection {
        private class MutableInt {
            private int value = 0;
        }
        private class MyIterator implements Iterator {
            private Iterator keyIter = t.keySet().iterator();
            private Object current;
            private int occurrence;
            public synchronized boolean hasNext() {
                return occurrence > 0 || keyIter.hasNext();
            }
            public synchronized Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                if (occurrence == 0) {
                    current = keyIter.next();
                    occurrence = ((MutableInt) t.get(current)).value;
                }
                --occurrence;
                return current;
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
        private TreeMap t;
        private int size;

        SortedBag(Comparator c) {
            t = new TreeMap(c);
        }
        public synchronized Iterator iterator() {
            return new MyIterator();
        }
        public synchronized boolean add(Object o) {
            if (size < Integer.MAX_VALUE) {
                ++size;
            }
            MutableInt m = (MutableInt) (t.get(o));
            if (m == null) {
                m = new MutableInt();
                t.put(o, m);
            }
            m.value++;
            return true;
        }
        public synchronized int size() {
            return size;
        }
    }

    private DelegatedResourceComparator comp = new DelegatedResourceComparator();

    /**
     * Sort the contained elements.
     * @return a Collection of Resources.
     */
    protected synchronized Collection getCollection() {
        ResourceCollection rc = getResourceCollection();
        Iterator iter = rc.iterator();
        if (!(iter.hasNext())) {
            return Collections.EMPTY_SET;
        }
        SortedBag b = new SortedBag(comp);
        while (iter.hasNext()) {
            b.add(iter.next());
        }
        return b;
    }

    /**
     * Add a ResourceComparator to this Sort ResourceCollection.
     * If multiple ResourceComparators are added, they will be processed in LIFO order.
     * @param c the ResourceComparator to add.
     */
    public synchronized void add(ResourceComparator c) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        comp.add(c);
        FailFast.invalidate(this);
    }

    /**
     * Overrides the BaseResourceCollectionContainer version
     * to recurse on nested ResourceComparators.
     * @param stk the stack of data types to use (recursively).
     * @param p   the project to use to dereference the references.
     * @throws BuildException on error.
     */
    protected synchronized void dieOnCircularReference(Stack stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            DataType.invokeCircularReferenceCheck(comp, stk, p);
            setChecked(true);
        }
    }

}
