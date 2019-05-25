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

import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class to contain version numbers in "Dewey Decimal"
 * syntax.  Numbers in the "Dewey Decimal" syntax consist of positive
 * decimal integers separated by periods ".".  For example, "2.0" or
 * "1.2.3.4.5.6.7".  This allows an extensible number to be used to
 * represent major, minor, micro, etc versions.  The version number
 * must begin with a number.
 *
 */
public class DeweyDecimal implements Comparable<DeweyDecimal> {

    /** Array of components that make up DeweyDecimal */
    private final int[] components;

    /**
     * Construct a DeweyDecimal from an array of integer components.
     *
     * @param components an array of integer components.
     */
    public DeweyDecimal(final int[] components) {
        this.components = new int[components.length];
        System.arraycopy(components, 0, this.components, 0, components.length);
    }

    /**
     * Construct a DeweyDecimal from string in DeweyDecimal format.
     *
     * @param string the string in dewey decimal format
     * @exception NumberFormatException if string is malformed
     */
    public DeweyDecimal(final String string)
        throws NumberFormatException {
        final StringTokenizer tokenizer = new StringTokenizer(string, ".", true);
        final int size = tokenizer.countTokens();

        components = new int[(size + 1) / 2];

        for (int i = 0; i < components.length; i++) {
            final String component = tokenizer.nextToken();
            if (component.isEmpty()) {
                throw new NumberFormatException("Empty component in string");
            }

            components[i] = Integer.parseInt(component);

            //Strip '.' token
            if (tokenizer.hasMoreTokens()) {
                tokenizer.nextToken();

                //If it ended in a dot, throw an exception
                if (!tokenizer.hasMoreTokens()) {
                    throw new NumberFormatException("DeweyDecimal ended in a '.'");
                }
            }
        }
    }

    /**
     * Return number of components in <code>DeweyDecimal</code>.
     *
     * @return the number of components in dewey decimal
     */
    public int getSize() {
        return components.length;
    }

    /**
     * Return the component at specified index.
     *
     * @param index the index of components
     * @return the value of component at index
     */
    public int get(final int index) {
        return components[index];
    }

    /**
     * Return <code>true</code> if this <code>DeweyDecimal</code> is
     * equal to the other <code>DeweyDecimal</code>.
     *
     * @param other the other DeweyDecimal
     * @return true if equal to other DeweyDecimal, false otherwise
     */
    public boolean isEqual(final DeweyDecimal other) {
        final int max = Math.max(other.components.length, components.length);

        for (int i = 0; i < max; i++) {
            final int component1 = (i < components.length) ? components[i] : 0;
            final int component2 = (i < other.components.length) ? other.components[i] : 0;

            if (component2 != component1) {
                return false;
            }
        }

        return true; // Exact match
    }

    /**
     * Return <code>true</code> if this <code>DeweyDecimal</code> is
     * less than the other <code>DeweyDecimal</code>.
     *
     * @param other the other DeweyDecimal
     * @return true if less than other DeweyDecimal, false otherwise
     */
    public boolean isLessThan(final DeweyDecimal other) {
        return !isGreaterThanOrEqual(other);
    }

    /**
     * Return <code>true</code> if this <code>DeweyDecimal</code> is
     * less than or equal to the other <code>DeweyDecimal</code>.
     *
     * @param other the other DeweyDecimal
     * @return true if less than or equal to other DeweyDecimal, false otherwise
     */
    public boolean isLessThanOrEqual(final DeweyDecimal other) {
        return !isGreaterThan(other);
    }

    /**
     * Return <code>true</code> if this <code>DeweyDecimal</code> is
     * greater than the other <code>DeweyDecimal</code>.
     *
     * @param other the other DeweyDecimal
     * @return true if greater than other DeweyDecimal, false otherwise
     */
    public boolean isGreaterThan(final DeweyDecimal other) {
        final int max = Math.max(other.components.length, components.length);

        for (int i = 0; i < max; i++) {
            final int component1 = (i < components.length) ? components[i] : 0;
            final int component2 = (i < other.components.length) ? other.components[i] : 0;

            if (component2 > component1) {
                return false;
            }
            if (component2 < component1) {
                return true;
            }
        }

        return false; // Exact match
    }

    /**
     * Return <code>true</code> if this <code>DeweyDecimal</code> is
     * greater than or equal to the other <code>DeweyDecimal</code>.
     *
     * @param other the other DeweyDecimal
     * @return true if greater than or equal to other DeweyDecimal, false otherwise
     */
    public boolean isGreaterThanOrEqual(final DeweyDecimal other) {
        final int max = Math.max(other.components.length, components.length);

        for (int i = 0; i < max; i++) {
            final int component1 = (i < components.length) ? components[i] : 0;
            final int component2 = (i < other.components.length) ? other.components[i] : 0;

            if (component2 > component1) {
                return false;
            }
            if (component2 < component1) {
                return true;
            }
        }

        return true; // Exact match
    }

    /**
     * Return string representation of <code>DeweyDecimal</code>.
     *
     * @return the string representation of DeweyDecimal.
     */
    @Override public String toString() {
        return IntStream.of(components).mapToObj(Integer::toString)
            .collect(Collectors.joining("."));
    }

    /**
     * Compares this DeweyDecimal with another one.
     *
     * @param other another DeweyDecimal to compare with
     * @return result
     * @see java.lang.Comparable#compareTo(Object)
     */
    @Override
    public int compareTo(DeweyDecimal other) {
        final int max = Math.max(other.components.length, components.length);
        for (int i = 0; i < max; i++) {
            final int component1 = (i < components.length) ? components[i] : 0;
            final int component2 = (i < other.components.length) ? other.components[i] : 0;
            if (component1 != component2) {
                return component1 - component2;
            }
        }
        return 0;
    }

    @Override public int hashCode() {
        return toString().hashCode();
    }

    @Override public boolean equals(Object o) {
        return o instanceof DeweyDecimal && isEqual((DeweyDecimal) o);
    }

}
