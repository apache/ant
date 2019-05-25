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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.apache.tools.ant.types.Resource;

/**
 * Resource collection which load underlying resource collection only on demand
 * with support for caching
 */
public class LazyResourceCollectionWrapper extends
        AbstractResourceCollectionWrapper {

    /** List of cached resources */
    private final List<Resource> cachedResources = new ArrayList<>();

    private Iterator<Resource> filteringIterator;

    private final Supplier<Iterator<Resource>> filteringIteratorSupplier =
        () -> new FilteringIterator(getResourceCollection().iterator());

    @Override
    protected Iterator<Resource> createIterator() {
        if (isCache()) {
            if (filteringIterator == null) {
                // no worry of thread safety here, see function's contract
                filteringIterator = filteringIteratorSupplier.get();
            }
            return new CachedIterator(filteringIterator);
        }
        return filteringIteratorSupplier.get();
    }

    @Override
    protected int getSize() {
        // to compute the size, just iterate: the iterator will take care of
        // caching
        final Iterator<Resource> it = createIterator();
        int size = 0;
        while (it.hasNext()) {
            it.next();
            size++;
        }
        return size;
    }

    /**
     * Specify if the resource should be filtered or not. This function should
     * be overridden in order to define the filtering algorithm
     *
     * @param r resource considered for filtration
     * @return whether the resource should be filtered or not
     */
    protected boolean filterResource(final Resource r) {
        return false;
    }

    private class FilteringIterator implements Iterator<Resource> {

        Resource next = null;

        boolean ended = false;

        protected final Iterator<Resource> it;

        FilteringIterator(final Iterator<Resource> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            if (ended) {
                return false;
            }
            while (next == null) {
                if (!it.hasNext()) {
                    ended = true;
                    return false;
                }
                next = it.next();
                if (filterResource(next)) {
                    next = null;
                }
            }
            return true;
        }

        @Override
        public Resource next() {
            if (!hasNext()) {
                throw new UnsupportedOperationException();
            }
            final Resource r = next;
            next = null;
            return r;
        }

    }

    /**
     * Iterator that will put in the shared cache array list the selected
     * resources
     */
    private class CachedIterator implements Iterator<Resource> {

        int cursor = 0;

        private final Iterator<Resource> it;

        /**
         * Default constructor
         *
         * @param it
         *            the iterator which will provide the resources to put in
         *            cache
         */
        public CachedIterator(final Iterator<Resource> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            synchronized (cachedResources) {
                // have we already cached the next entry ?
                if (cachedResources.size() > cursor) {
                    return true;
                }
                // does the wrapped iterator any more resource ?
                if (!it.hasNext()) {
                    return false;
                }
                // put in cache the next resource
                final Resource r = it.next();
                cachedResources.add(r);
            }
            return true;
        }

        @Override
        public Resource next() {
            // first check that we have some to deliver
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            synchronized (cachedResources) {
                // return the cached entry as hasNext should have put one for
                // this iterator
                return cachedResources.get(cursor++);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
