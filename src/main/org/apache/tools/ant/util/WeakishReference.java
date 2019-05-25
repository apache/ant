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


import java.lang.ref.WeakReference;

/**
 * These classes are part of some code to reduce memory leaks by only
 * retaining weak references to things
 * on Java1.2+, and yet still work (with leaky hard references) on Java1.1.
 * Now that Ant is 1.2+ only,
 * life is simpler and none of the classes are needed any more.
 *
 * They are only retained in case a third-party task uses them
 * @since ant1.6
 * @see org.apache.tools.ant.util.optional.WeakishReference12
 * @deprecated deprecated 1.7; will be removed in Ant1.8
 *             Just use {@link java.lang.ref.WeakReference} directly.
 */
@Deprecated
public class WeakishReference  {


    private WeakReference<Object> weakref;

    /**
     * create a new soft reference, which is bound to a
     * Weak reference inside
     *
     * @param reference ditto
     * @see java.lang.ref.WeakReference
     */
    WeakishReference(Object reference) {
        this.weakref = new WeakReference<>(reference);
    }

    /**
     * Returns this reference object's referent.  If this reference object has
     * been cleared, then this method returns <code>null</code>.
     *
     * @return The object to which this reference refers, or
     *         <code>null</code> if this reference object has been cleared.
     */
    public Object get() {
        return weakref.get();
    }

    /**
     * create the appropriate type of reference for the java version
     * @param object the object that the reference will refer to.
     * @return reference to the Object.
     */
    public static WeakishReference createReference(Object object) {
            return new WeakishReference(object);
    }


    /**
     * This was a hard reference for Java 1.1. Since Ant1.7,
     * @deprecated since 1.7.
     *             Hopefully nobody is using this.
     */
    public static class HardReference extends WeakishReference {

        /**
         * constructor.
         * @param object the object that the reference will refer to.
         */
        public HardReference(Object object) {
            super(object);
        }

    }

}
