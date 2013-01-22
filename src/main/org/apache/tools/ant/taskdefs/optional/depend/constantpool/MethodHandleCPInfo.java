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
package org.apache.tools.ant.taskdefs.optional.depend.constantpool;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * A MethodHandle CP Info
 *
 */
public class MethodHandleCPInfo extends ConstantPoolEntry {
    private ConstantPoolEntry reference;

    /** reference kind **/
    private ReferenceKind referenceKind;
    /** Must be a valid index into the constant pool tabel. */
    private int referenceIndex;
    /**
     * the index into the constant pool which defined the name and type
     * signature of the method
     */
    private int nameAndTypeIndex;
    public enum ReferenceKind {
        REF_getField(1),
        REF_getStatic(2),
        REF_putField(3),
        REF_putStatic(4),
        REF_invokeVirtual(5),
        REF_invokeStatic(6),
        REF_invokeSpecial(7),
        REF_newInvokeSpecial(8),
        REF_invokeInterface(9);
        private final int referenceKind;
        ReferenceKind(int referenceKind) {
            this.referenceKind = referenceKind;
        }

    }
    /** Constructor. */
    public MethodHandleCPInfo() {
        super(CONSTANT_METHODHANDLE, 1);
    }

    /**
     * read a constant pool entry from a class stream.
     *
     * @param cpStream the DataInputStream which contains the constant pool
     *      entry to be read.
     * @exception java.io.IOException if there is a problem reading the entry from
     *      the stream.
     */
    public void read(DataInputStream cpStream) throws IOException {
        referenceKind = ReferenceKind.values()[cpStream.readUnsignedByte() - 1];

        referenceIndex = cpStream.readUnsignedShort();
    }

    /**
     * Print a readable version of the constant pool entry.
     *
     * @return the string representation of this constant pool entry.
     */
    public String toString() {
        String value;

        if (isResolved()) {
            value = "MethodHandle : " + reference.toString();
        } else {
            value = "MethodHandle : Reference kind = " + referenceKind
                 +  "Reference index = " + referenceIndex;
        }

        return value;
    }

    /**
     * Resolve this constant pool entry with respect to its dependents in
     * the constant pool.
     *
     * @param constantPool the constant pool of which this entry is a member
     *      and against which this entry is to be resolved.
     */
    public void resolve(ConstantPool constantPool) {
        reference = constantPool.getEntry(referenceIndex);
        reference.resolve(constantPool);
        super.resolve(constantPool);
    }


}

