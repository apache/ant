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
 * A parameter is composed of a name, type and value.
 *
 */
public final class Parameter {
    private String name = null;
    private String type = null;
    private String value = null;

    /**
     * Set the name attribute.
     *
     * @param name a <code>String</code> value
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Set the type attribute.
     *
     * @param type a <code>String</code> value
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * Set the value attribute.
     *
     * @param value a <code>String</code> value
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * Get the name attribute.
     *
     * @return a <code>String</code> value
     */
    public String getName() {
        return name;
    }

    /**
     * Get the type attribute.
     *
     * @return a <code>String</code> value
     */
    public String getType() {
        return type;
    }

    /**
     * Get the value attribute.
     *
     * @return a <code>String</code> value
     */
    public String getValue() {
        return value;
    }
}
