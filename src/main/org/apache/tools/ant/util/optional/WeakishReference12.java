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

package org.apache.tools.ant.util.optional;

import org.apache.tools.ant.util.WeakishReference;

/**
 * This is a reference that really is is Weak, as it uses the
 * appropriate java.lang.ref class.
 * @deprecated since 1.7.
 *             Just use {@link java.lang.ref.WeakReference} directly.
 * Note that in ant1.7 is parent was changed to extend HardReference.
 * This is because the latter has access to the (package scoped)
 * WeakishReference(Object) constructor, and both that and this are thin
 * facades on the underlying no-longer-abstract base class.
 */
@Deprecated
public class WeakishReference12 extends WeakishReference.HardReference  {


    /**
     * create a new soft reference, which is bound to a
     * Weak reference inside
     * @param reference the object to reference.
     * @see java.lang.ref.WeakReference
     */
    public WeakishReference12(Object reference) {
        super(reference);
    }
}
