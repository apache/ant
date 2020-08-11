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
package org.apache.tools.ant.property;

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;

import java.util.Set;

/**
 * Thread local class containing local properties.
 * @since Ant 1.8.0
 */
public class LocalProperties
    extends InheritableThreadLocal<LocalPropertyStack>
    implements PropertyHelper.PropertyEvaluator,
        PropertyHelper.PropertySetter, PropertyHelper.PropertyEnumerator {

    /**
     * Get a localproperties for the given project.
     * @param project the project to retrieve the localproperties for.
     * @return the localproperties.
     */
    public static synchronized LocalProperties get(Project project) {
        LocalProperties l =
            project.getReference(MagicNames.REFID_LOCAL_PROPERTIES);
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
    @Override
    protected synchronized LocalPropertyStack initialValue() {
        return new LocalPropertyStack();
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
        get().addLocal(property);
    }

    /** enter the scope */
    public void enterScope() {
        get().enterScope();
    }

    /** exit the scope */
    public void exitScope() {
        get().exitScope();
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
        set(get().copy());
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
    @Override
    public Object evaluate(String property, PropertyHelper helper) {
        return get().evaluate(property, helper);
    }

    /**
     * Set a *new" property.
     * @param property the property's String "identifier".
     * @param value    the value to set.
     * @param propertyHelper the invoking PropertyHelper.
     * @return true if this entity 'owns' the property.
     */
    @Override
    public boolean setNew(
        String property, Object value, PropertyHelper propertyHelper) {
        return get().setNew(property, value, propertyHelper);
    }

    /**
     * Set a property.
     * @param property the property's String "identifier".
     * @param value    the value to set.
     * @param propertyHelper the invoking PropertyHelper.
     * @return true if this entity 'owns' the property.
     */
    @Override
    public boolean set(
        String property, Object value, PropertyHelper propertyHelper) {
        return get().set(property, value, propertyHelper);
    }

    @Override
    public Set<String> getPropertyNames() {
        return get().getPropertyNames();
    }
}
