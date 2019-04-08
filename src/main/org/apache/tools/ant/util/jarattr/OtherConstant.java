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
 * A constant pool entry of a type not relevant to the
 * {@code module-info.class} manipulation done by this package.
 * Constant data is stored as a raw byte array.
 */
class OtherConstant
extends Constant {
    /** Constant's raw data. */
    private final byte[] info;

    /**
     * Creates a new instance.
     *
     * @param tag {@code cp_info} tag value
     * @param info raw data of constant
     */
    OtherConstant(byte tag,
                  byte[] info) {
        super(tag);
        this.info = info;
    }

    @Override
    void writeTo(DataOutputStream out)
    throws IOException {
        out.writeByte(tag());
        out.write(info);
    }

    /**
     * Returns a diagnostic string containing this constant entry's
     * tag and data size.
     *
     * @return string form of this object, suitable for debugging
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[tag=" + tag()
            + ", " + info.length + " bytes]";
    }
}
