/*
 * Copyright  2000-2002,2004 The Apache Software Foundation
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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;

/**
 * Helper class for attributes that can only take one of a fixed list
 * of values.
 *
 * <p>See {@link org.apache.tools.ant.taskdefs.FixCRLF FixCRLF} for an
 * example.
 *
 * @author Stefan Bodewig
 */
public abstract class EnumeratedAttribute {

    /**
     * The selected value in this enumeration.
     */
    protected String value;

    /**
     * the index of the selected value in the array.
     */
    private int index = -1;

    /**
     * This is the only method a subclass needs to implement.
     *
     * @return an array holding all possible values of the enumeration.
     * The order of elements must be fixed so that <tt>indexOfValue(String)</tt>
     * always return the same index for the same value.
     */
    public abstract String[] getValues();

    /** bean constructor */
    protected EnumeratedAttribute() {
    }

    /**
     * Invoked by {@link org.apache.tools.ant.IntrospectionHelper IntrospectionHelper}.
     */
    public final void setValue(String value) throws BuildException {
        int index = indexOfValue(value);
        if (index == -1) {
            throw new BuildException(value + " is not a legal value for this attribute");
        }
        this.index = index;
        this.value = value;
    }

    /**
     * Is this value included in the enumeration?
     */
    public final boolean containsValue(String value) {
        return (indexOfValue(value) != -1);
    }

    /**
     * get the index of a value in this enumeration.
     * @param value the string value to look for.
     * @return the index of the value in the array of strings
     * or -1 if it cannot be found.
     * @see #getValues()
     */
    public final int indexOfValue(String value) {
        String[] values = getValues();
        if (values == null || value == null) {
            return -1;
        }
        for (int i = 0; i < values.length; i++) {
            if (value.equals(values[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return the selected value.
     */
    public final String getValue() {
        return value;
    }

    /**
     * @return the index of the selected value in the array.
     * @see #getValues()
     */
    public final int getIndex() {
        return index;
    }


    /**
     * Convert the value to its string form.
     *
     * @return the string form of the value.
     */
    public String toString() {
        return getValue();
    }

}
