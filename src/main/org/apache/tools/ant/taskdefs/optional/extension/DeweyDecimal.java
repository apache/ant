/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.extension;

import java.util.StringTokenizer;

/**
 * Utility class to contain version numbers in "Dewey Decimal"
 * syntax.  Numbers in the "Dewey Decimal" syntax consist of positive
 * decimal integers separated by periods ".".  For example, "2.0" or
 * "1.2.3.4.5.6.7".  This allows an extensible number to be used to
 * represent major, minor, micro, etc versions.  The version number
 * must begin with a number.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public final class DeweyDecimal {
    /** Array of components that make up DeweyDecimal */
    private int[] components;

    /**
     * Construct a DeweyDecimal from an array of integer components.
     *
     * @param components an array of integer components.
     */
    public DeweyDecimal(final int[] components) {
        this.components = new int[components.length];

        for (int i = 0; i < components.length; i++) {
            this.components[i] = components[i];
        }
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

        components = new int[ (size + 1) / 2 ];

        for (int i = 0; i < components.length; i++) {
            final String component = tokenizer.nextToken();
            if (component.equals("")) {
                throw new NumberFormatException("Empty component in string");
            }

            components[ i ] = Integer.parseInt(component);

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
        return components[ index ];
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
            final int component1 = (i < components.length) ? components[ i ] : 0;
            final int component2 = (i < other.components.length) ? other.components[ i ] : 0;

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
            final int component1 = (i < components.length) ? components[ i ] : 0;
            final int component2 = (i < other.components.length) ? other.components[ i ] : 0;

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
            final int component1 = (i < components.length) ? components[ i ] : 0;
            final int component2 = (i < other.components.length) ? other.components[ i ] : 0;

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
    public String toString() {
        final StringBuffer sb = new StringBuffer();

        for (int i = 0; i < components.length; i++) {
            if (i != 0) {
                sb.append('.');
            }
            sb.append(components[ i ]);
        }

        return sb.toString();
    }
}
