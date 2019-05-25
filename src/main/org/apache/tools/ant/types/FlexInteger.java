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

package org.apache.tools.ant.types;

/**
 * Helper class which can be used for Ant task attribute setter methods to allow
 * the build file to specify an integer in either decimal, octal, or hexadecimal
 * format.
 *
 * @see java.lang.Integer#decode(String)
 */
public class FlexInteger {
    private Integer value;

    /**
     * Constructor used by Ant's introspection mechanism for attribute population
     * @param value the value to decode
     */
    public FlexInteger(String value) {
        this.value = Integer.decode(value);
    }

    /**
     * Returns the decimal integer value
     * @return the integer value
     */
    public int intValue() {
        return value;
    }

    /**
     * Overridden method to return the decimal value for display
     * @return a string version of the integer
     */
    @Override
    public String toString() {
        return value.toString();
    }
}
