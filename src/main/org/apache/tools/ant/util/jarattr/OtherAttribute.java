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

import java.util.Objects;

/**
 * A class file attribute whose specific data is unimportant to this
 * package, because it does not relate to module information.  It will be
 * preserved as is, and written back byte-for-byte when updating
 * {@code module-info.class}.
 */
class OtherAttribute
extends Attribute {
    /** Attribute data. */
    private final byte[] info;

    /**
     * Creates a new attribute instance.
     *
     * @param nameIndex index in constant pool of {@code Constant_Utf8}
     *                  containing attribute's name
     * @param length size in bytes of attribute data
     * @param info raw attribute data, must have size equal to {@code length}
     *
     * @throws IllegalArgumentException if {@code info} has a length different
     *                                  from {@code length}
     */
    OtherAttribute(int nameIndex,
                   int length,
                   byte[] info) {
        super(nameIndex, length);

        this.info = Objects.requireNonNull(info, "info cannot be null");

        if (info.length != length) {
            throw new IllegalArgumentException(
                "length (" + length + ") must be the same"
                + " as byte array's length (" + info.length + ")");
        }
    }

    @Override
    void writeTo(DataOutputStream out)
    throws IOException {
        out.writeShort(attributeNameIndex());
        out.writeInt(attributeLength());
        out.write(info);
    }
}
