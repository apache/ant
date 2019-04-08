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
 * A {@code CONSTANT_Utf8} constant pool entry containing text.
 */
class UTF8Constant
extends Constant {
    /** {@code cp_info} tag value for all constants of this type. */
    static final byte TAG = 1;

    /** This constant's text value. */
    private final String value;

    /**
     * Creates a new constant pool entry containing the specified text.
     *
     * @param value new constant's text value
     */
    UTF8Constant(String value) {
        this(TAG, value);
    }

    /**
     * Creates a new constant pool entry with an explicitly specified tag
     * (though the tag should always be the value in {@link #TAG}).
     *
     * @param tag new constant's tag (should always be {@link #TAG})
     * @param value new constant's text value
     */
    UTF8Constant(byte tag,
                 String value) {
        super(tag);
        this.value = value;
    }

    /**
     * Returns the text of this constant.
     *
     * @return non-{@code null} string value of constant
     */
    String value() {
        return value;
    }

    @Override
    void writeTo(DataOutputStream out)
    throws IOException {
        out.writeByte(tag());
        out.writeUTF(value);
    }

    /**
     * Returns a diagnostic string containing this constant entry's
     * tag and text data.
     *
     * @return string form of this object, suitable for debugging
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[tag=" + tag()
            + ", value=\"" + value + "\"]";
    }
}
