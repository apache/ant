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
package org.apache.tools.ant.taskdefs.optional.jlink;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads just enough of a class file to determine the class' full name.
 *
 * <p>Extremely minimal constant pool implementation, mainly to support extracting
 * strings from a class file.
 */
class ConstantPool {
    // CheckStyle:VisibilityModifier OFF - bc
    static final
        byte UTF8 = 1, UNUSED = 2, INTEGER = 3, FLOAT = 4, LONG = 5, DOUBLE = 6,
        CLASS = 7, STRING = 8, FIELDREF = 9, METHODREF = 10,
        INTERFACEMETHODREF = 11, NAMEANDTYPE = 12;

    byte[] types;

    Object[] values;
    // CheckStyle:VisibilityModifier ON

    /**
     * Create a constant pool.
     * @param data the data input containing the class.
     * @throws IOException if there is an error.
     */
    ConstantPool(DataInput data) throws IOException {
        super();

        int count = data.readUnsignedShort();
        types = new byte[count];
        values = new Object[count];
        // read in all constant pool entries.
        for (int i = 1; i < count; i++) {
            byte type = data.readByte();
            types[i] = type;
            switch (type) {
            case UTF8 :
                values[i] = data.readUTF();
                break;

            case UNUSED :
                break;

            case INTEGER :
            case FIELDREF :
            case METHODREF :
            case INTERFACEMETHODREF :
            case NAMEANDTYPE :
                values[i] = data.readInt();
                break;

            case FLOAT :
                values[i] = data.readFloat();
                break;

            case LONG :
                values[i] = data.readLong();
                ++i;
                break;

            case DOUBLE :
                values[i] = data.readDouble();
                ++i;
                break;

            case CLASS :
            case STRING :
                values[i] = data.readUnsignedShort();
                break;

            default:
                // Do nothing
            }
        }
    }
}

/**
 * Provides a quick and dirty way to determine the true name of a class
 * given just an InputStream. Reads in just enough to perform this
 * minimal task only.
 */
public class ClassNameReader {
    private static final int CLASS_MAGIC_NUMBER =  0xCAFEBABE;

    /**
     * Get the class name of a class in an input stream.
     *
     * @param input an <code>InputStream</code> value
     * @return the name of the class
     * @exception IOException if an error occurs
     */
    public static String getClassName(InputStream input) throws IOException {
        DataInputStream data = new DataInputStream(input);
        // verify this is a valid class file.
        int cookie = data.readInt();
        if (cookie != CLASS_MAGIC_NUMBER) {
            return null;
        }
        /* int version = */ data.readInt();
        // read the constant pool.
        ConstantPool constants = new ConstantPool(data);
        Object[] values = constants.values;
        // read access flags and class index.
        /* int accessFlags = */ data.readUnsignedShort();
        int classIndex = data.readUnsignedShort();
        Integer stringIndex = (Integer) values[classIndex];
        return (String) values[stringIndex];
    }

}


