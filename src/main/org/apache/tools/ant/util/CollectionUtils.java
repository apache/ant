/*
 * Copyright  2002-2004 The Apache Software Foundation
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
package org.apache.tools.ant.util;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * A set of helper methods related to collection manipulation.
 *
 * @since Ant 1.5
 */
public class CollectionUtils {

    /**
     * Please use Vector.equals() or List.equals()
     *
     * @since Ant 1.5
     * @deprecated
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
     * Please use  Map.equals()
     *
     * <p>Follows the equals contract of Java 2's Map.</p>
     *
     * @since Ant 1.5
     * @deprecated
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
     *
     * @since Ant 1.6
     * @deprecated
     */
    public static void putAll(Dictionary m1, Dictionary m2) {
        for (Enumeration it = m2.keys(); it.hasMoreElements();) {
            Object key = it.nextElement();
            m1.put(key, m2.get(key));
        }
    }

    /**
     * @since Ant 1.6
     */
    public static final class EmptyEnumeration implements Enumeration {
        public EmptyEnumeration() {
        }

        public boolean hasMoreElements() {
            return false;
        }

        public Object nextElement() throws NoSuchElementException {
            throw new NoSuchElementException();
        }
    }

}
