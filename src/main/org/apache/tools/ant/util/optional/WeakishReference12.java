/*
 * Copyright  2000-2004 Apache Software Foundation
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

package org.apache.tools.ant.util.optional;

import org.apache.tools.ant.util.WeakishReference;

import java.lang.ref.WeakReference;

/**
 * This is a reference that really is is Weak, as it uses the
 * appropriate java.lang.ref class.
 *
 * @author Not Specified.
 */
public class WeakishReference12 extends WeakishReference  {

    private WeakReference weakref;

    /**
     * create a new soft reference, which is bound to a
     * Weak reference inside
     * @param reference
     * @see java.lang.ref.WeakReference
     */
    public WeakishReference12(Object reference) {
        this.weakref = new WeakReference(reference);
    }

    /**
     * Returns this reference object's referent.
     *
     * @return referent.
     */
    public Object get() {
        return weakref.get();
    }

}
