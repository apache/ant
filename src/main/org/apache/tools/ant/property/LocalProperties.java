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

import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.MagicNames;

/**
 * Thread local class containing local properties.
 * @since Ant 1.8.0
 */
public class LocalProperties
    extends InheritableThreadLocal
    implements PropertyHelper.PropertyEvaluator,
    PropertyHelper.PropertySetter {

    /**
     * Get a localproperties for the given project.
     * @param project the project to retieve the localproperties for.
     * @return the localproperties.
     */
    public static synchronized LocalProperties get(Project project) {
        LocalProperties l = (LocalProperties) project.getReference(
            MagicNames.REFID_LOCAL_PROPERTIES);
        if (l == null) {
            l = new LocalProperties();
            project.addReference(MagicNames.REFID_LOCAL_PROPERTIES, l);
            PropertyHelper.getPropertyHelper(project).add(l);
        }
        return l;
    }

    // --------------------------------------------------
    //
    //  Thread stuff
    //
    // --------------------------------------------------

    /**
     * Construct a new LocalProperties object.
     */
    private LocalProperties() {
    }

    /**
     * Get the initial value.
     * @return a new localproperties stack.
     */
    protected synchronized Object initialValue() {
        return new LocalPropertyStack();
    }

    private LocalPropertyStack current() {
        return (LocalPropertyStack) get();
    }

    // --------------------------------------------------
    //
    //  Local property adding and scoping
    //
    // --------------------------------------------------

    /**
     * Add a local property to the current scope.
     * @param property the property name to add.
     */
    public void addLocal(String property) {
        current().addLocal(property);
    }

    /** enter the scope */
    public void enterScope() {
        current().enterScope();
    }

    /** exit the scope */
    public void exitScope() {
        current().exitScope();
    }

    // --------------------------------------------------
    //
    //  Copy - used in parallel to make a new stack
    //
    // --------------------------------------------------

    /**
     * Copy the stack for a parallel thread.
     * To be called from the parallel thread itself.
     */
    public void copy() {
        set(current().copy());
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
        return current().evaluate(property, helper);
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
        return current().setNew(property, value, propertyHelper);
    }

    /**
     * Set a property.
     * @param property the property's String "identifier".
     * @param value    the value to set.
     * @param propertyHelper the invoking PropertyHelper.
     * @return true if this entity 'owns' the property.
     */
    public boolean set(
        String property, Object value, PropertyHelper propertyHelper) {
        return current().set(property, value, propertyHelper);
    }
}


