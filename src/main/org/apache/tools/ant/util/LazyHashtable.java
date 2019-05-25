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

import java.util.Enumeration;
import java.util.Hashtable;

/** Hashtable implementation that allows delayed construction
 * of expensive objects
 *
 * All operations that need access to the full list of objects
 * will call initAll() first. Get and put are cheap.
 *
 * @since Ant 1.6
 */
@Deprecated
public class LazyHashtable<K, V> extends Hashtable<K, V> {
    // CheckStyle:VisibilityModifier OFF - bc
    protected boolean initAllDone = false;
    // CheckStyle:VisibilityModifier OFF - bc

    /** No arg constructor. */
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


    /**
     * Get a enumeration over the elements.
     * @return an enumeration.
     */
    public Enumeration<V> elements() {
        initAll();
        return super.elements();
    }

    /**
     * Check if the table is empty.
     * @return true if it is.
     */
    public boolean isEmpty() {
        initAll();
        return super.isEmpty();
    }

    /**
     * Get the size of the table.
     * @return the size.
     */
    public int size() {
        initAll();
        return super.size();
    }

    /**
     * Check if the table contains a particular value.
     * @param value the value to look for.
     * @return true if the table contains the value.
     */
    public boolean contains(Object value) {
        initAll();
        return super.contains(value);
    }

    /**
     * Check if the table contains a particular key.
     * @param value the key to look for.
     * @return true if the table contains key.
     */
    public boolean containsKey(Object value) {
        initAll();
        return super.containsKey(value);
    }

    /**
     * Delegates to {@link #contains contains}.
     * @param value the value to look for.
     * @return true if the table contains the value.
     */
    public boolean containsValue(Object value) {
        return contains(value);
    }

    /**
     * Get an enumeration over the keys.
     * @return an enumeration.
     */
    public Enumeration<K> keys() {
        initAll();
        return super.keys();
    }

    // TODO Unfortunately JDK1.2 adds entrySet(), keySet(), values() -
    // implementing this requires a small hack, we can add it later.
}
