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
package org.apache.tools.ant.taskdefs.optional.depend.constantpool;

/**
 * A Constant Pool entry which represents a constant value.
 *
 */
public abstract class ConstantCPInfo extends ConstantPoolEntry {

    /**
     * The entry's untyped value. Each subclass interprets the constant
     * value based on the subclass's type. The value here must be
     * compatible.
     */
    private Object value;

    /**
     * Initialise the constant entry.
     *
     * @param tagValue the constant pool entry type to be used.
     * @param entries the number of constant pool entry slots occupied by
     *      this entry.
     */
    protected ConstantCPInfo(int tagValue, int entries) {
        super(tagValue, entries);
    }

    /**
     * Get the value of the constant.
     *
     * @return the value of the constant (untyped).
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set the constant value.
     *
     * @param newValue the new untyped value of this constant.
     */
    public void setValue(Object newValue) {
        value = newValue;
    }

}
