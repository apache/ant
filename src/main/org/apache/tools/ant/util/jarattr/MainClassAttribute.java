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

package org.apache.tools.ant.util.jarattr;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Class file attribute which describes a module's main class
 * entry point.
 */
class MainClassAttribute
extends Attribute {
    /** Official name of this attribute, as per JVM specification. */
    static final String NAME = "ModuleMainClass";

    /** Bytes in attribute, not counting tag. */
    private static final int SIZE = 2;

    /**
     * Index in constant pool of a {@code CONSTANT_Class} entry
     * representing the main class of the module.
     */
    private int mainClassIndex;

    /**
     * Creates a new attribute instance.
     *
     * @param nameIndex index in constant pool of this attribute's name
     *                  (a {@code CONSTANT_Utf8} which must contain the
     *                  value of {@link #NAME}).
     * @param mainClassNameIndex index in constant pool of the binary name
     *                  of the main class (a {@code CONSTANT_Class} entry)
     */
    MainClassAttribute(int nameIndex,
                       int mainClassNameIndex) {
        super(nameIndex, SIZE);
        this.mainClassIndex = mainClassNameIndex;
    }

    /**
     * Creates a new attribute instance with an explicitly specified length
     * (which should be unnecessary, since the length must always be 2).
     *
     * @param nameIndex index in constant pool of this attribute's name
     *                  (a {@code CONSTANT_Utf8} which must contain the
     *                  value of {@link #NAME}).
     * @param length length of attribute data (should always be 2)
     * @param mainClassNameIndex index in constant pool of the binary name
     *                  of the main class (a {@code CONSTANT_Class} entry)
     */
    MainClassAttribute(int nameIndex,
                       int length,
                       int mainClassNameIndex) {
        super(nameIndex, length);
        this.mainClassIndex = mainClassNameIndex;

        if (length != SIZE) {
            throw new IllegalArgumentException(
                "Main class attribute must have a length 2.");
        }
    }

    /**
     * Returns index in constant pool of a {@code CONSTANT_Class} entry
     * representing the main class of the containing module.
     *
     * @return index of class constant in constant pool, or zero if not set
     */
    int getClassIndex() {
        return mainClassIndex;
    }

    /**
     * Sets index in constant pool of a {@code CONSTANT_Class} entry
     * representing the main class of the containing module.
     *
     * @param index index of class constant in constant pool
     */
    void setClassIndex(int index) {
        this.mainClassIndex = index;
    }

    @Override
    void writeTo(DataOutputStream out)
    throws IOException {
        out.writeShort(attributeNameIndex());
        out.writeInt(attributeLength());
        out.writeShort(mainClassIndex);
    }
}
