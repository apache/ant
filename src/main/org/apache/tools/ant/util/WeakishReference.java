/*
 * Copyright  2000-2005 The Apache Software Foundation
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

import org.apache.tools.ant.util.optional.WeakishReference12;

/**
 * This is a weak reference on java1.2 and up, that is all
 * platforms Ant1.6 supports.
 * @since ant1.6
 */
public abstract class WeakishReference  {

    /**
     * create the appropriate type of reference for the java version
     * @param object the object that the reference will refer to.
     * @return reference to the Object.
     */
    public static WeakishReference createReference(Object object) {
            return new WeakishReference12(object);
    }

    /**
     * Returns this reference object's referent.  If this reference object has
     * been cleared, then this method returns <code>null</code>.
     *
     * @return The object to which this reference refers, or
     *         <code>null</code> if this reference object has been cleared.
     */
    public abstract Object get();

    /**
     * A hard reference for Java 1.1.
     * Hopefully nobody is using this.
     */
    public static class HardReference extends WeakishReference {
        private Object object;

        /**
         * constructor.
         * @param object the object that the reference will refer to.
         */
        public HardReference(Object object) {
            this.object = object;
        }

        /**
         * Returns this reference object's referent.
         * @return the object to which this reference refers.
         */
        public Object get() {
            return object;
        }
    }

}
