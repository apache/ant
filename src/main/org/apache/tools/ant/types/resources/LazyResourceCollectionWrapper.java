package org.apache.tools.ant.types.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.tools.ant.types.Resource;

/**
 * Resource collection which load underlying resource collection only on demand
 * with support for caching
 */
public class LazyResourceCollectionWrapper extends
        AbstractResourceCollectionWrapper {

    /** List of cached resources */
    private List cachedResources = new ArrayList();

    private FilteringIterator filteringIterator;

    protected Iterator createIterator() {
        Iterator iterator;
        if (isCache()) {
            if (filteringIterator == null) {
                // no worry of thread safety here, see function's contract
                filteringIterator = new FilteringIterator(
                        getResourceCollection().iterator());
            }
            iterator = new CachedIterator(filteringIterator);
        } else {
            iterator = new FilteringIterator(getResourceCollection().iterator());
        }
        return iterator;
    }

    protected int getSize() {
        // to compute the size, just iterate: the iterator will take care of
        // caching
        Iterator it = createIterator();
        int size = 0;
        while (it.hasNext()) {
            it.next();
            size++;
        }
        return size;
    }

    /**
     * Specify if the resource should be filtered or not. This function should
     * be overrided in order to define the filtering algorithm
     * 
     * @param r resource considered for filtration
     * @return whether the resource should be filtered or not
     */
    protected boolean filterResource(Resource r) {
        return false;
    }

    private class FilteringIterator implements Iterator {

        Resource next = null;

        boolean ended = false;

        protected final Iterator it;

        public FilteringIterator(Iterator it) {
            this.it = it;
        }

        public boolean hasNext() {
            if (ended) {
                return false;
            }
            while (next == null) {
                if (!it.hasNext()) {
                    ended = true;
                    return false;
                }
                next = (Resource) it.next();
                if (filterResource(next)) {
                    next = null;
                }
            }
            return true;
        }

        public Object next() {
            if (!hasNext()) {
                throw new UnsupportedOperationException();
            }
            Resource r = next;
            next = null;
            return r;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Iterator that will put in the shared cache array list the selected
     * resources
     */
    private class CachedIterator implements Iterator {

        int cusrsor = 0;

        private final Iterator it;

        /**
         * Default constructor
         * 
         * @param it
         *            the iterator which will provide the resources to put in
         *            cache
         */
        public CachedIterator(Iterator it) {
            this.it = it;
        }

        public boolean hasNext() {
            synchronized (cachedResources) {
                // have we already cached the next entry ?
                if (cachedResources.size() > cusrsor) {
                    return true;
                }
                // does the wrapped iterator any more resource ?
                if (!it.hasNext()) {
                    return false;
                }
                // put in cache the next resource
                Resource r = (Resource) it.next();
                cachedResources.add(r);
            }
            return true;
        }

        public Object next() {
            // first check that we have some to deliver
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            synchronized (cachedResources) {
                // return the cached entry as hasNext should have put one for
                // this iterator
                return cachedResources.get(cusrsor++);
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
