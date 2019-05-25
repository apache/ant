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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A couple of methods related to enumerations that might be useful.
 * This class should probably disappear once the required JDK is set to 1.2
 * instead of 1.1.
 *
 */
@Deprecated
public final class Enumerations {

    private Enumerations() {
    }

    /**
     * creates an enumeration from an array of objects.
     * @param <T> object type
     * @param array the array of object to enumerate.
     * @return the enumeration over the array of objects.
     * @deprecated use Collections.enumeration(Arrays.asList(array))
     */
    @Deprecated
    @SafeVarargs
    public static <T> Enumeration<T> fromArray(T... array) {
        return Collections.enumeration(Arrays.asList(array));
    }

    /**
     * creates an enumeration from an array of enumeration. The created enumeration
     * will sequentially enumerate over all elements of each enumeration and skip
     * <code>null</code> enumeration elements in the array.
     * @param <T> object type
     * @param enums the array of enumerations.
     * @return the enumeration over the array of enumerations.
     * @deprecated use Stream.concat(Collections.list(one).stream(), Collections.list(two).stream())
     *                 .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::enumeration))
     */
    @Deprecated
    @SafeVarargs
    public static <T> Enumeration<T> fromCompound(Enumeration<? extends T>... enums) {
        return new CompoundEnumeration<>(enums);
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
@Deprecated
class CompoundEnumeration<T> implements Enumeration<T> {

    /** enumeration array */
    private Enumeration<? extends T>[] enumArray;

    /** index in the enums array */
    private int index = 0;

    @SafeVarargs
    public CompoundEnumeration(Enumeration<? extends T>... enumarray) {
        this.enumArray = enumarray;
    }

    /**
     * Tests if this enumeration contains more elements.
     *
     * @return  <code>true</code> if and only if this enumeration object
     *           contains at least one more element to provide;
     *          <code>false</code> otherwise.
     */
    @Override
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
    @Override
    public T nextElement() throws NoSuchElementException {
        if (hasMoreElements()) {
            return enumArray[index].nextElement();
        }
        throw new NoSuchElementException();
    }
}
