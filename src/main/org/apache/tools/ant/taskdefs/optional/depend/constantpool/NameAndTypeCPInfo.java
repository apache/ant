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

import java.io.DataInputStream;
import java.io.IOException;

/**
 * A NameAndType CP Info
 *
 */
public class NameAndTypeCPInfo extends ConstantPoolEntry {
    /** the name component of this entry */
    private String name;
    /** the type component of this entry */
    private String type;
    /**
     * the index into the constant pool at which the name component's string
     * value is stored
     */
    private int nameIndex;
    /**
     * the index into the constant pool where the type descriptor string is
     * stored.
     */
    private int descriptorIndex;

    /** Constructor. */
    public NameAndTypeCPInfo() {
        super(CONSTANT_NAMEANDTYPE, 1);
    }

    /**
     * read a constant pool entry from a class stream.
     *
     * @param cpStream the DataInputStream which contains the constant pool
     *      entry to be read.
     * @exception IOException if there is a problem reading the entry from
     *      the stream.
     */
    @Override
    public void read(DataInputStream cpStream) throws IOException {
        nameIndex = cpStream.readUnsignedShort();
        descriptorIndex = cpStream.readUnsignedShort();
    }

    /**
     * Print a readable version of the constant pool entry.
     *
     * @return the string representation of this constant pool entry.
     */
    @Override
    public String toString() {
        if (isResolved()) {
            return "Name = " + name + ", type = " + type;
        }
        return "Name index = " + nameIndex + ", descriptor index = "
            + descriptorIndex;
    }

    /**
     * Resolve this constant pool entry with respect to its dependents in
     * the constant pool.
     *
     * @param constantPool the constant pool of which this entry is a member
     *      and against which this entry is to be resolved.
     */
    @Override
    public void resolve(ConstantPool constantPool) {
        name = ((Utf8CPInfo) constantPool.getEntry(nameIndex)).getValue();
        type = ((Utf8CPInfo) constantPool.getEntry(descriptorIndex)).getValue();

        super.resolve(constantPool);
    }

    /**
     * Get the name component of this entry
     *
     * @return the name of this name and type entry
     */
    public String getName() {
        return name;
    }

    /**
     * Get the type signature of this entry
     *
     * @return the type signature of this entry
     */
    public String getType() {
        return type;
    }

}
