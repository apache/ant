/*
 * Copyright  2000-2004 The Apache Software Foundation
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

import java.util.Hashtable;
import java.util.Enumeration;

/** Hashtable implementation that allows delayed construction
 * of expensive objects
 *
 * All operations that need access to the full list of objects
 * will call initAll() first. Get and put are cheap.
 *
 * @since Ant 1.6
 */
public class LazyHashtable extends Hashtable {
    protected boolean initAllDone = false;

    public LazyHashtable() {
        super();
    }

    /** Used to be part of init. It must be done once - but
     * we delay it until we do need _all_ tasks. Otherwise we
     * just get the tasks that we need, and avoid costly init.
     */
    protected void initAll() {
        if (initAllDone) {
            return;
        }
        initAllDone = true;
    }


    public Enumeration elements() {
        initAll();
        return super.elements();
    }

    public boolean isEmpty() {
        initAll();
        return super.isEmpty();
    }

    public int size() {
        initAll();
        return super.size();
    }

    public boolean contains(Object value) {
        initAll();
        return super.contains(value);
    }

    public boolean containsKey(Object value) {
        initAll();
        return super.containsKey(value);
    }

    /**
     * Delegates to {@link #contains contains}.
     */
    public boolean containsValue(Object value) {
        return contains(value);
    }

    public Enumeration keys() {
        initAll();
        return super.keys();
    }

    // XXX Unfortunately JDK1.2 adds entrySet(), keySet(), values() -
    // implementing this requires a small hack, we can add it later.
}
