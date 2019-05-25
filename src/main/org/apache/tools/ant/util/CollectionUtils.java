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
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

// CheckStyle:HideUtilityClassConstructorCheck OFF - bc

/**
 * A set of helper methods related to collection manipulation.
 *
 * @since Ant 1.5
 */
@Deprecated
public class CollectionUtils {

    @SuppressWarnings("rawtypes")
    @Deprecated
    public static final List EMPTY_LIST = Collections.EMPTY_LIST; //NOSONAR

    /**
     * Please use Vector.equals() or List.equals().
     * @param v1 the first vector.
     * @param v2 the second vector.
     * @return true if the vectors are equal.
     * @since Ant 1.5
     * @deprecated since 1.6.x.
     */
    @Deprecated
    public static boolean equals(Vector<?> v1, Vector<?> v2) {
        return Objects.equals(v1, v2);
    }

    /**
     * Dictionary does not have an equals.
     * Please use  Map.equals().
     *
     * <p>Follows the equals contract of Java 2's Map.</p>
     * @param d1 the first directory.
     * @param d2 the second directory.
     * @return true if the directories are equal.
     * @since Ant 1.5
     * @deprecated since 1.6.x.
     */
    @Deprecated
    public static boolean equals(Dictionary<?, ?> d1, Dictionary<?, ?> d2) {
        if (d1 == d2) {
            return true;
        }

        if (d1 == null || d2 == null) {
            return false;
        }

        if (d1.size() != d2.size()) {
            return false;
        }

        // don't need the opposite check as the Dictionaries have the
        // same size, so we've also covered all keys of d2 already.
        return StreamUtils.enumerationAsStream(d1.keys())
                .allMatch(key -> d1.get(key).equals(d2.get(key)));
    }

    /**
     * Creates a comma separated list of all values held in the given
     * collection.
     *
     * @param c collection to transform
     * @return string representation of the collection
     * @since Ant 1.8.0
     * @deprecated use stream().collect(Collectors.joining(","))
     */
    @Deprecated
    public static String flattenToString(Collection<?> c) {
        return c.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * Dictionary does not know the putAll method. Please use Map.putAll().
     * @param m1 the to directory.
     * @param m2 the from directory.
     * @param <K> type of the key
     * @param <V> type of the value
     * @since Ant 1.6
     * @deprecated since 1.6.x.
     */
    @Deprecated
    public static <K, V> void putAll(Dictionary<? super K, ? super V> m1,
        Dictionary<? extends K, ? extends V> m2) {
        StreamUtils.enumerationAsStream(m2.keys()).forEach(key -> m1.put(key, m2.get(key)));
    }

    /**
     * An empty enumeration.
     * @since Ant 1.6
     */
    @Deprecated
    public static final class EmptyEnumeration<E> implements Enumeration<E> {

        /**
         * @return false always.
         */
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        /**
         * @return nothing.
         * @throws NoSuchElementException always.
         */
        @Override
        public E nextElement() throws NoSuchElementException {
            throw new NoSuchElementException();
        }
    }

    /**
     * Append one enumeration to another.
     * Elements are evaluated lazily.
     * @param e1 the first enumeration.
     * @param e2 the subsequent enumeration.
     * @param <E> element type
     * @return an enumeration representing e1 followed by e2.
     * @since Ant 1.6.3
     * @deprecated use Stream.concat(Collections.list(e1).stream(), Collections.list(e2).stream())
     *                 .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::enumeration))
     */
    @Deprecated
    public static <E> Enumeration<E> append(Enumeration<E> e1, Enumeration<E> e2) {
        return new CompoundEnumeration<>(e1, e2);
    }

    /**
     * Adapt the specified Iterator to the Enumeration interface.
     * @param iter the Iterator to adapt.
     * @param <E> element type
     * @return an Enumeration.
     * @deprecated use Collections.enumeration()
     */
    @Deprecated
    public static <E> Enumeration<E> asEnumeration(final Iterator<E> iter) {
        return new Enumeration<E>() {
            @Override
            public boolean hasMoreElements() {
                return iter.hasNext();
            }
            @Override
            public E nextElement() {
                return iter.next();
            }
        };
    }

    /**
     * Adapt the specified Enumeration to the Iterator interface.
     * @param e the Enumeration to adapt.
     * @param <E> element type
     * @return an Iterator.
     * @deprecated use Collections.list(e).iterator()
     */
    @Deprecated
    public static <E> Iterator<E> asIterator(final Enumeration<E> e) {
        return new Iterator<E>() {
            @Override
            public boolean hasNext() {
                return e.hasMoreElements();
            }
            @Override
            public E next() {
                return e.nextElement();
            }
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns a collection containing all elements of the iterator.
     *
     * @param iter the Iterator to convert
     * @param <T> element type
     * @return the collection
     * @since Ant 1.8.0
     * @deprecated instantiate a list and use forEachRemaining(list::add)
     */
    @Deprecated
    public static <T> Collection<T> asCollection(final Iterator<? extends T> iter) {
        List<T> l = new ArrayList<>();
        iter.forEachRemaining(l::add);
        return l;
    }

    private static final class CompoundEnumeration<E> implements Enumeration<E> {

        private final Enumeration<E> e1, e2;

        public CompoundEnumeration(Enumeration<E> e1, Enumeration<E> e2) {
            this.e1 = e1;
            this.e2 = e2;
        }

        @Override
        public boolean hasMoreElements() {
            return e1.hasMoreElements() || e2.hasMoreElements();
        }

        @Override
        public E nextElement() throws NoSuchElementException {
            if (e1.hasMoreElements()) {
                return e1.nextElement();
            }
            return e2.nextElement();
        }

    }

    /**
     * Counts how often the given Object occurs in the given
     * collection using equals() for comparison.
     *
     * @param c collection in which to search
     * @param o object to search
     * @return frequency
     * @since Ant 1.8.0
     */
    @Deprecated
    public static int frequency(Collection<?> c, Object o) {
        return c == null ? 0 : Collections.frequency(c, o);
    }

    private CollectionUtils() {
    }
}
