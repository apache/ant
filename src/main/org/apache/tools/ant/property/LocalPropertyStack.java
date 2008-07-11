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
package org.apache.tools.ant.property;


import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.tools.ant.PropertyHelper;

/**
 * A stack of local property maps.
 * There is a map for each scope (target, sequential, macro).
 * @since Ant 1.8.0
 */
public class LocalPropertyStack {
    private LinkedList stack = new LinkedList();

    // --------------------------------------------------
    //
    //  Local property adding and scoping
    //
    // --------------------------------------------------

    /**
     * Add a local property.
     * @param property the name of the local proeprty.
     */
    public void addLocal(String property) {
        if (!stack.isEmpty()) {
            ((Map) stack.getFirst()).put(property, NullReturn.NULL);
        }
    }

    /**
     * Enter the local scope.
     */
    public void enterScope() {
        stack.addFirst(new HashMap());
    }

    /**
     * Exit the local scope.
     */
    public void exitScope() {
        ((HashMap) stack.removeFirst()).clear();
    }

    // --------------------------------------------------
    //
    //  Copy - used in parallel to make a new stack
    //
    // --------------------------------------------------

    /**
     * Copy the stack for a parallel thread.
     * @return a copy.
     */
    public LocalPropertyStack copy() {
        LocalPropertyStack ret = new LocalPropertyStack();
        ret.stack.addAll(stack);
        return ret;
    }

    // --------------------------------------------------
    //
    //  PropertyHelper delegate methods
    //
    // --------------------------------------------------

    /**
     * Evaluate a property.
     * @param property the property's String "identifier".
     * @param helper the invoking PropertyHelper.
     * @return Object value.
     */
    public Object evaluate(String property, PropertyHelper helper) {
        for (Iterator i = stack.iterator(); i.hasNext();) {
            Map map = (Map) i.next();
            Object ret = map.get(property);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    /**
     * Set a *new" property.
     * @param property the property's String "identifier".
     * @param value    the value to set.
     * @param propertyHelper the invoking PropertyHelper.
     * @return true if this entity 'owns' the property.
     */
    public boolean setNew(
        String property, Object value, PropertyHelper propertyHelper) {
        Map map = getMapForProperty(property);
        if (map == null) {
            return false;
        }
        Object currValue = map.get(property);
        if (currValue == NullReturn.NULL) {
            map.put(property, value);
        }
        return true;
    }

    /**
     * Set a property.
     * @param property the property's String "identifier".
     * @param value    the value to set.
     * @param propertyHelper the invoking PropertyHelper.
     * @return true if this entity 'owns' the property.
     */
    public boolean set(String property, Object value, PropertyHelper propertyHelper) {
        Map map = getMapForProperty(property);
        if (map == null) {
            return false;
        }
        map.put(property, value);
        return true;
    }

    private Map getMapForProperty(String property) {
        for (Iterator i = stack.iterator(); i.hasNext();) {
            Map map = (Map) i.next();
            if (map.get(property) != null) {
                return map;
            }
        }
        return null;
    }
}

