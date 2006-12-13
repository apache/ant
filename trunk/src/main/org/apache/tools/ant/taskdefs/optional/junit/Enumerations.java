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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A couple of methods related to enumerations that might be useful.
 * This class should probably disappear once the required JDK is set to 1.2
 * instead of 1.1.
 *
 */
public final class Enumerations {

        private Enumerations() {
        }

        /**
         * creates an enumeration from an array of objects.
         * @param       array   the array of object to enumerate.
         * @return the enumeration over the array of objects.
         */
        public static Enumeration fromArray(Object[] array) {
                return new ArrayEnumeration(array);
        }

        /**
        * creates an enumeration from an array of enumeration. The created enumeration
        * will sequentially enumerate over all elements of each enumeration and skip
        * <tt>null</tt> enumeration elements in the array.
        * @param        enums   the array of enumerations.
        * @return the enumeration over the array of enumerations.
         */
        public static Enumeration fromCompound(Enumeration[] enums) {
                return new CompoundEnumeration(enums);
        }

}


/**
 * Convenient enumeration over an array of objects.
 */
class ArrayEnumeration implements Enumeration {

        /** object array */
        private Object[] array;

        /** current index */
        private int pos;

        /**
         * Initialize a new enumeration that wraps an array.
         * @param       array   the array of object to enumerate.
         */
        public ArrayEnumeration(Object[] array) {
                this.array = array;
                this.pos = 0;
        }
        /**
         * Tests if this enumeration contains more elements.
         *
         * @return  <code>true</code> if and only if this enumeration object
         *           contains at least one more element to provide;
         *          <code>false</code> otherwise.
         */
        public boolean hasMoreElements() {
                return (pos < array.length);
        }

        /**
         * Returns the next element of this enumeration if this enumeration
         * object has at least one more element to provide.
         *
         * @return     the next element of this enumeration.
         * @throws  NoSuchElementException  if no more elements exist.
         */
        public Object nextElement() throws NoSuchElementException {
                if (hasMoreElements()) {
                        Object o = array[pos];
                        pos++;
                        return o;
                }
                throw new NoSuchElementException();
        }
}
/**
 * Convenient enumeration over an array of enumeration. For example:
 * <pre>
 * Enumeration e1 = v1.elements();
 * while (e1.hasMoreElements()) {
 *    // do something
 * }
 * Enumeration e2 = v2.elements();
 * while (e2.hasMoreElements()) {
 *    // do the same thing
 * }
 * </pre>
 * can be written as:
 * <pre>
 * Enumeration[] enums = { v1.elements(), v2.elements() };
 * Enumeration e = Enumerations.fromCompound(enums);
 * while (e.hasMoreElements()) {
 *    // do something
 * }
 * </pre>
 * Note that the enumeration will skip null elements in the array. The following is
 * thus possible:
 * <pre>
 * Enumeration[] enums = { v1.elements(), null, v2.elements() }; // a null enumeration in the array
 * Enumeration e = Enumerations.fromCompound(enums);
 * while (e.hasMoreElements()) {
 *    // do something
 * }
 * </pre>
 */
 class CompoundEnumeration implements Enumeration {

        /** enumeration array */
        private Enumeration[] enumArray;

        /** index in the enums array */
        private int index = 0;

    public CompoundEnumeration(Enumeration[] enumarray) {
                this.enumArray = enumarray;
    }

        /**
         * Tests if this enumeration contains more elements.
         *
         * @return  <code>true</code> if and only if this enumeration object
         *           contains at least one more element to provide;
         *          <code>false</code> otherwise.
         */
    public boolean hasMoreElements() {
                while (index < enumArray.length) {
                        if (enumArray[index] != null && enumArray[index].hasMoreElements()) {
                                return true;
                        }
                        index++;
                }
                return false;
    }

        /**
         * Returns the next element of this enumeration if this enumeration
         * object has at least one more element to provide.
         *
         * @return     the next element of this enumeration.
         * @throws  NoSuchElementException  if no more elements exist.
         */
    public Object nextElement() throws NoSuchElementException {
                if (hasMoreElements()) {
                        return enumArray[index].nextElement();
                }
                throw new NoSuchElementException();
    }
}


