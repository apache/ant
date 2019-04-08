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
 * A constant consisting only of an index of another constant in the
 * constant pool.  For example, {@code CONSTANT_Class_info} consists of
 * only a one-byte tag and a two-byte {@code name_index}.
 */
class IndexedConstant
extends Constant {
    /** Index in constant pool of data this constant refers to. */
    private final int index;

    /**
     * Creates a new index based constant.
     *
     * @param tag constant pool tag value
     * @param index index in constant pool of data this constant will refer to
     */
    IndexedConstant(byte tag,
                    int index) {
        super(tag);
        this.index = index;
    }

    /**
     * Returns the index in the constant pool of the data to which
     * this constant refers.
     *
     * @return constant pool index for this constant's data
     */
    int index() {
        return index;
    }

    @Override
    void writeTo(DataOutputStream out)
    throws IOException {
        out.writeByte(tag());
        out.writeShort(index);
    }

    /**
     * Returns a diagnostic string form of this constant instance.
     *
     * @return string form of this object
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[tag=" + tag()
            + ", index=" + index + "]";
    }
}
