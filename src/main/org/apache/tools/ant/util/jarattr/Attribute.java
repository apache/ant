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

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.List;

// CheckStyle:LineLength OFF - Link is too long.
/**
 * {@code attribute_info} data as defined in the "{@code class} File Format"
 * section of the Java Virtual Machine Specification.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html">{@code class File Format</a>
 */
// CheckStyle:LineLength ON
abstract class Attribute {
    /** Index in constant pool of attribute name. */
    private final int attributeNameIndex;

    /** Length of attribute data in bytes. */
    private final int attributeLength;

    /**
     * Initializes a new instance.
     *
     * @param attributeNameIndex index in constant pool of attribute's name
     * @param length size of attribute data in bytes
     */
    Attribute(int attributeNameIndex,
              int length) {
        this.attributeNameIndex = attributeNameIndex;
        this.attributeLength = length;
    }

    /**
     * Returns the index in the constant pool of the Utf8 constant which
     * contains this attribute's name.
     *
     * @return index in constant pool of this attribute's name
     */
    int attributeNameIndex() {
        return attributeNameIndex;
    }

    /**
     * Returns the size of this attribute's data in bytes, not including
     * the name index and attribute length themselves.
     *
     * @return size of attribute's data in bytes
     */
    int attributeLength() {
        return attributeLength;
    }

    /**
     * Writes this attribute's data in class file format.
     * The first two bytes are always the {@link #attributeNameIndex},
     * and the next four bytes are always the {@link #attributeLength}.
     *
     * @param out stream to which attribute should be written
     *
     * @throws IOException if stream cannot be written to
     */
    abstract void writeTo(DataOutputStream out)
    throws IOException;

    /**
     * Reads a concrete {@code Attribute} object from a
     * {@code module-info.class} descriptor.  The stream is presumed to
     * point to the start of an {@code attribute_info} block.
     *
     * @param in module-info stream to read from
     * @param constantPool fully read constant pool of module-info;
     *                     never modified by this method
     *
     * @return new {@code Attribute} instance, never {@code null}
     */
    static Attribute readFrom(DataInputStream in,
                              List<? extends Constant> constantPool)
    throws IOException {
        int nameIndex = in.readUnsignedShort();
        int len = in.readInt();

        Constant c = constantPool.get(nameIndex - 1);
        if (!(c instanceof UTF8Constant)) {
            throw new ClassFormatException(
                "Attribute's attribute_name_index (" + nameIndex + ")"
                + " does not point to a CONSTANT_Utf8 in the constant pool");
        }

        UTF8Constant utf8 = (UTF8Constant) c;
        String name = utf8.value();

        if (name.equals(ModuleAttribute.NAME)) {
            return ModuleAttribute.readFrom(in, nameIndex, len);
        } else if (name.equals(MainClassAttribute.NAME)) {
            int mainClassNameIndex = in.readUnsignedShort();
            return new MainClassAttribute(nameIndex, len, mainClassNameIndex);
        } else {
            return new OtherAttribute(nameIndex, len, readNBytes(in, len));
        }
    }

    // Needed since we may be compiling or running with Java 9 or 10.
    /**
     * Identical to Java 11's {@code InputStream.readNBytes}, which reads
     * the specified number of bytes (or less if EOF is reached).
     *
     * @param in stream from which to read
     * @param count number of bytes to read
     *
     * @return new byte array containing {@code count} bytes (or fewer,
     *         if EOF is encountered before that many bytes can be read)
     */
    static byte[] readNBytes(InputStream in,
                             int count)
        throws IOException {

        byte[] bytes = new byte[count];

        int totalBytesRead = 0;
        while (totalBytesRead < count) {
            int bytesRead =
                in.read(bytes, totalBytesRead, count - totalBytesRead);

            if (bytesRead < 0) {
                return Arrays.copyOf(bytes, totalBytesRead);
            }

            totalBytesRead += bytesRead;
        }

        return bytes;
    }
}
