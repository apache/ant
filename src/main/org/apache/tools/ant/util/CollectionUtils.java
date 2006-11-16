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

import java.util.Vector;
import java.util.Iterator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.NoSuchElementException;

// CheckStyle:HideUtilityClassConstructorCheck OFF - bc

/**
 * A set of helper methods related to collection manipulation.
 *
 * @since Ant 1.5
 */
public class CollectionUtils {

    /**
     * Please use Vector.equals() or List.equals().
     * @param v1 the first vector.
     * @param v2 the second vector.
     * @return true if the vectors are equal.
     * @since Ant 1.5
     * @deprecated since 1.6.x.
     */
    public static boolean equals(Vector v1, Vector v2) {
        if (v1 == v2) {
            return true;
        }

        if (v1 == null || v2 == null) {
            return false;
        }

        return v1.equals(v2);
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
    public static boolean equals(Dictionary d1, Dictionary d2) {
        if (d1 == d2) {
            return true;
        }

        if (d1 == null || d2 == null) {
            return false;
        }

        if (d1.size() != d2.size()) {
            return false;
        }

        Enumeration e1 = d1.keys();
        while (e1.hasMoreElements()) {
            Object key = e1.nextElement();
            Object value1 = d1.get(key);
            Object value2 = d2.get(key);
            if (value2 == null || !value1.equals(value2)) {
                return false;
            }
        }

        // don't need the opposite check as the Dictionaries have the
        // same size, so we've also covered all keys of d2 already.

        return true;
    }

    /**
     * Dictionary does not know the putAll method. Please use Map.putAll().
     * @param m1 the to directory.
     * @param m2 the from directory.
     * @since Ant 1.6
     * @deprecated since 1.6.x.
     */
    public static void putAll(Dictionary m1, Dictionary m2) {
        for (Enumeration it = m2.keys(); it.hasMoreElements();) {
            Object key = it.nextElement();
            m1.put(key, m2.get(key));
        }
    }

    /**
     * An empty enumeration.
     * @since Ant 1.6
     */
    public static final class EmptyEnumeration implements Enumeration {
        /** Constructor for the EmptyEnumeration */
        public EmptyEnumeration() {
        }

        /**
         * @return false always.
         */
        public boolean hasMoreElements() {
            return false;
        }

        /**
         * @return nothing.
         * @throws NoSuchElementException always.
         */
        public Object nextElement() throws NoSuchElementException {
            throw new NoSuchElementException();
        }
    }

    /**
     * Append one enumeration to another.
     * Elements are evaluated lazily.
     * @param e1 the first enumeration.
     * @param e2 the subsequent enumeration.
     * @return an enumeration representing e1 followed by e2.
     * @since Ant 1.6.3
     */
    public static Enumeration append(Enumeration e1, Enumeration e2) {
        return new CompoundEnumeration(e1, e2);
    }

    /**
     * Adapt the specified Iterator to the Enumeration interface.
     * @param iter the Iterator to adapt.
     * @return an Enumeration.
     */
    public static Enumeration asEnumeration(final Iterator iter) {
        return new Enumeration() {
            public boolean hasMoreElements() {
                return iter.hasNext();
            }
            public Object nextElement() {
                return iter.next();
            }
        };
    }

    /**
     * Adapt the specified Enumeration to the Iterator interface.
     * @param e the Enumeration to adapt.
     * @return an Iterator.
     */
    public static Iterator asIterator(final Enumeration e) {
        return new Iterator() {
            public boolean hasNext() {
                return e.hasMoreElements();
            }
            public Object next() {
                return e.nextElement();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static final class CompoundEnumeration implements Enumeration {

        private final Enumeration e1, e2;

        public CompoundEnumeration(Enumeration e1, Enumeration e2) {
            this.e1 = e1;
            this.e2 = e2;
        }

        public boolean hasMoreElements() {
            return e1.hasMoreElements() || e2.hasMoreElements();
        }

        public Object nextElement() throws NoSuchElementException {
            if (e1.hasMoreElements()) {
                return e1.nextElement();
            } else {
                return e2.nextElement();
            }
        }

    }

}
