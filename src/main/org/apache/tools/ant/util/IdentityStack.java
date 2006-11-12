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

import java.util.Stack;

/**
 * Identity Stack.
 * @since Ant 1.7
 */
public class IdentityStack extends Stack {

    /**
     * Get an IdentityStack containing the contents of the specified Stack.
     * @param s the Stack to copy; ignored if null.
     * @return an IdentityStack instance.
     */
    public static IdentityStack getInstance(Stack s) {
        if (s instanceof IdentityStack) {
            return (IdentityStack) s;
        }
        IdentityStack result = new IdentityStack();
        if (s != null) {
            result.addAll(s);
        }
        return result;
    }

    /**
     * Default constructor.
     */
    public IdentityStack() {
    }

    /**
     * Construct a new IdentityStack with the specified Object
     * as the bottom element.
     * @param o the bottom element.
     */
    public IdentityStack(Object o) {
        super();
        push(o);
    }

    /**
     * Override methods that use <code>.equals()</code> comparisons on elements.
     * @param o the Object to search for.
     * @return true if the stack contains the object.
     * @see java.util.Vector#contains(Object)
     */
    public synchronized boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    /**
     * Override methods that use <code>.equals()</code> comparisons on elements.
     * @param o   the Object to search for.
     * @param pos the position from which to search.
     * @return the position of the object, -1 if not found.
     * @see java.util.Vector#indexOf(Object, int)
     */
    public synchronized int indexOf(Object o, int pos) {
        for (int i = pos; i < size(); i++) {
            if (get(i) == o) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Override methods that use <code>.equals()</code> comparisons on elements.
     * @param o   the Object to search for.
     * @param pos the position from which to search (backward).
     * @return the position of the object, -1 if not found.
     * @see java.util.Vector#indexOf(Object, int)
     */
    public synchronized int lastIndexOf(Object o, int pos) {
        for (int i = pos; i >= 0; i--) {
            if (get(i) == o) {
                return i;
            }
        }
        return -1;
    }

}
