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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Represents a {@code cp_info} entry in a the constant pool of a
 * module-info.class file.
 */
abstract class Constant {
    /** Constant pool tag value for {@code CONSTANT_Utf8}. */
    static final byte UTF8 = 1;
    /** Constant pool tag value for {@code CONSTANT_Integer}. */
    static final byte INTEGER = 3;
    /** Constant pool tag value for {@code CONSTANT_Float}. */
    static final byte FLOAT = 4;
    /** Constant pool tag value for {@code CONSTANT_Long}. */
    static final byte LONG = 5;
    /** Constant pool tag value for {@code CONSTANT_Double}. */
    static final byte DOUBLE = 6;
    /** Constant pool tag value for {@code CONSTANT_Class}. */
    static final byte CLASS = 7;
    /** Constant pool tag value for {@code CONSTANT_String}. */
    static final byte STRING = 8;
    /** Constant pool tag value for {@code CONSTANT_Fieldref}. */
    static final byte FIELDREF = 9;
    /** Constant pool tag value for {@code CONSTANT_Methodref}. */
    static final byte METHODREF = 10;
    /** Constant pool tag value for {@code CONSTANT_InterfaceMethodref}. */
    static final byte INTERFACEMETHODREF = 11;
    /** Constant pool tag value for {@code CONSTANT_NameAndType}. */
    static final byte NAMEANDTYPE = 12;
    /** Constant pool tag value for {@code CONSTANT_MethodHandle}. */
    static final byte METHODHANDLE = 15;
    /** Constant pool tag value for {@code CONSTANT_MethodType}. */
    static final byte METHODTYPE = 16;
    /** Constant pool tag value for {@code CONSTANT_Dynamic}. */
    static final byte DYNAMIC = 17;
    /** Constant pool tag value for {@code CONSTANT_InvokeDynamic}. */
    static final byte INVOKEDYNAMIC = 18;
    /** Constant pool tag value for {@code CONSTANT_Module}. */
    static final byte MODULE = 19;
    /** Constant pool tag value for {@code CONSTANT_Package}. */
    static final byte PACKAGE = 20;

    /** Tag value of this constant pool entry. */
    private final byte tag;

    /**
     * Initializes a new constant instance.
     *
     * @param tag constant pool tag value of new instance
     */
    Constant(byte tag) {
        this.tag = tag;
    }

    /**
     * Returns this constant's JVM-defined tag value.
     *
     * @return JVM tag number of this constant
     *
     * @see #UTF8
     * @see #INTEGER
     * @see #FLOAT
     * @see #LONG
     * @see #DOUBLE
     * @see #CLASS
     * @see #STRING
     * @see #FIELDREF
     * @see #METHODREF
     * @see #INTERFACEMETHODREF
     * @see #NAMEANDTYPE
     * @see #METHODHANDLE
     * @see #METHODTYPE
     * @see #DYNAMIC
     * @see #INVOKEDYNAMIC
     * @see #MODULE
     * @see #PACKAGE
     */
    byte tag() {
        return tag;
    }

    /**
     * Reads a {@code cp_info} block from a {@code module-info.class}
     * stream.  Upon return, the stream is positioned immediately after
     * the {@code cp_info} block.
     *
     * @param in {@code module-info.class} stream, positioned at start
     *           of {@code cp_info} data
     *
     * @return new {@code Constant} instance
     *
     * @throws IOException if stream cannot be read
     */
    static Constant readFrom(DataInputStream in)
    throws IOException {

        byte tag = (byte) in.readUnsignedByte();

        switch (tag) {
            case UTF8:
                return new UTF8Constant(tag, in.readUTF());
            case CLASS:
            case STRING:
            case METHODTYPE:
            case MODULE:
            case PACKAGE:
                return new IndexedConstant(tag, in.readUnsignedShort());
            case LONG:
            case DOUBLE:
                return new OtherConstant(tag, Attribute.readNBytes(in, 8));
            default:
                return new OtherConstant(tag, Attribute.readNBytes(in, 4));
        }
    }

    /**
     * Saves this constant in {@code module-info.class} format.
     *
     * @param out destination to write to
     *
     * @throws IOException if stream cannot be written to
     */
    abstract void writeTo(DataOutputStream out)
    throws IOException;
}
