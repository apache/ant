/*
 * Copyright  2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.repository;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * List with an enablement iterator.
 */
public class EnabledLibraryElementList extends LinkedList {

    /**
     * Constructs an empty list.
     */
    public EnabledLibraryElementList() {
    }

    /**
     * return an iterator that only iterates over enabled stuff
     * @return
     */
    public Iterator enabledIterator() {
        return new EnabledIterator(this);
    }

    /**
     * iterator through a list that skips everything that is not enabled
     */
    private static class EnabledIterator implements Iterator {
        private Iterator _underlyingIterator;
        private EnabledLibraryElement _next;


        /**
         * constructor
         *
         * @param collection
         */
        EnabledIterator(Collection collection) {
            _underlyingIterator = collection.iterator();
        }


        /**
         * test for having another enabled component
         *
         * @return
         */
        public boolean hasNext() {
            while (_next == null && _underlyingIterator.hasNext()) {
                EnabledLibraryElement candidate = (EnabledLibraryElement) _underlyingIterator.next();
                if (candidate.getEnabled()) {
                    _next = candidate;
                }
            }
            return (_next != null);
        }

        /**
         * get the next element
         *
         * @return
         */
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            EnabledLibraryElement result = _next;
            _next = null;
            return result;
        }

        /**
         * removal is not supported
         *
         * @throws UnsupportedOperationException always
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
