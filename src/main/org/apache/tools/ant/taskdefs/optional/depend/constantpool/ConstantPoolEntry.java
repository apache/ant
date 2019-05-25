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
 * An entry in the constant pool. This class contains a representation of the
 * constant pool entries. It is an abstract base class for all the different
 * forms of constant pool entry.
 *
 * @see ConstantPool
 */
public abstract class ConstantPoolEntry {

    /** Tag value for UTF8 entries. */
    public static final int CONSTANT_UTF8 = 1;

    /** Tag value for Integer entries. */
    public static final int CONSTANT_INTEGER = 3;

    /** Tag value for Float entries. */
    public static final int CONSTANT_FLOAT = 4;

    /** Tag value for Long entries. */
    public static final int CONSTANT_LONG = 5;

    /** Tag value for Double entries. */
    public static final int CONSTANT_DOUBLE = 6;

    /** Tag value for Class entries. */
    public static final int CONSTANT_CLASS = 7;

    /** Tag value for String entries. */
    public static final int CONSTANT_STRING = 8;

    /** Tag value for Field Reference entries. */
    public static final int CONSTANT_FIELDREF = 9;

    /** Tag value for Method Reference entries. */
    public static final int CONSTANT_METHODREF = 10;

    /** Tag value for Interface Method Reference entries. */
    public static final int CONSTANT_INTERFACEMETHODREF = 11;

    /** Tag value for Name and Type entries. */
    public static final int CONSTANT_NAMEANDTYPE = 12;

    /** Tag value for Method Handle entries */
    public static final int CONSTANT_METHODHANDLE  = 15;

    /** Tag value for Method Type entries */
    public static final int CONSTANT_METHODTYPE = 16;

    /** Tag value for InvokeDynamic entries*/
    public static final int CONSTANT_INVOKEDYNAMIC = 18;

    /** Tag value for CONSTANT_Module_info entry */
    public static final int CONSTANT_MODULEINFO = 19;

    /** Tag value for CONSTANT_Package_info entry (within a module) */
    public static final int CONSTANT_PACKAGEINFO = 20;


    /**
     * This entry's tag which identifies the type of this constant pool
     * entry.
     */
    private int tag;

    /**
     * The number of slots in the constant pool, occupied by this entry.
     */
    private int numEntries;

    /**
     * A flag which indicates if this entry has been resolved or not.
     */
    private boolean resolved;

    /**
     * Initialise the constant pool entry.
     *
     * @param tagValue the tag value which identifies which type of constant
     *      pool entry this is.
     * @param entries the number of constant pool entry slots this entry
     *      occupies.
     */
    public ConstantPoolEntry(int tagValue, int entries) {
        tag = tagValue;
        numEntries = entries;
        resolved = false;
    }

    /**
     * Read a constant pool entry from a stream. This is a factory method
     * which reads a constant pool entry form a stream and returns the
     * appropriate subclass for the entry.
     *
     * @param cpStream the stream from which the constant pool entry is to
     *      be read.
     * @return the appropriate ConstantPoolEntry subclass representing the
     *      constant pool entry from the stream.
     * @exception IOException if the constant pool entry cannot be read
     *      from the stream
     */
    public static ConstantPoolEntry readEntry(DataInputStream cpStream)
         throws IOException {
        int cpTag = cpStream.readUnsignedByte();

        ConstantPoolEntry cpInfo;
        switch (cpTag) {

            case CONSTANT_UTF8:
                cpInfo = new Utf8CPInfo();
                break;
            case CONSTANT_INTEGER:
                cpInfo = new IntegerCPInfo();
                break;
            case CONSTANT_FLOAT:
                cpInfo = new FloatCPInfo();
                break;
            case CONSTANT_LONG:
                cpInfo = new LongCPInfo();
                break;
            case CONSTANT_DOUBLE:
                cpInfo = new DoubleCPInfo();
                break;
            case CONSTANT_CLASS:
                cpInfo = new ClassCPInfo();
                break;
            case CONSTANT_STRING:
                cpInfo = new StringCPInfo();
                break;
            case CONSTANT_FIELDREF:
                cpInfo = new FieldRefCPInfo();
                break;
            case CONSTANT_METHODREF:
                cpInfo = new MethodRefCPInfo();
                break;
            case CONSTANT_INTERFACEMETHODREF:
                cpInfo = new InterfaceMethodRefCPInfo();
                break;
            case CONSTANT_NAMEANDTYPE:
                cpInfo = new NameAndTypeCPInfo();
                break;
            case CONSTANT_METHODHANDLE:
                cpInfo = new MethodHandleCPInfo();
                break;
            case CONSTANT_METHODTYPE:
                cpInfo = new MethodTypeCPInfo();
                break;
            case CONSTANT_INVOKEDYNAMIC:
                cpInfo = new InvokeDynamicCPInfo();
                break;
            case CONSTANT_MODULEINFO:
                cpInfo = new ModuleCPInfo();
                break;
            case CONSTANT_PACKAGEINFO:
                cpInfo = new PackageCPInfo();
                break;
            default:
                throw new ClassFormatError("Invalid Constant Pool entry Type "
                     + cpTag);
        }
        cpInfo.read(cpStream);

        return cpInfo;
    }

    /**
     * Indicates whether this entry has been resolved. In general a constant
     * pool entry can reference another constant pool entry by its index
     * value. Resolution involves replacing this index value with the
     * constant pool entry at that index.
     *
     * @return true if this entry has been resolved.
     */
    public boolean isResolved() {
        return resolved;
    }

    /**
     * Resolve this constant pool entry with respect to its dependents in
     * the constant pool.
     *
     * @param constantPool the constant pool of which this entry is a member
     *      and against which this entry is to be resolved.
     */
    public void resolve(ConstantPool constantPool) {
        resolved = true;
    }

    /**
     * read a constant pool entry from a class stream.
     *
     * @param cpStream the DataInputStream which contains the constant pool
     *      entry to be read.
     * @exception IOException if there is a problem reading the entry from
     *      the stream.
     */
    public abstract void read(DataInputStream cpStream) throws IOException;

    /**
     * Get the Entry's type tag.
     *
     * @return The Tag value of this entry
     */
    public int getTag() {
        return tag;
    }

    /**
     * Get the number of Constant Pool Entry slots within the constant pool
     * occupied by this entry.
     *
     * @return the number of slots used.
     */
    public final int getNumEntries() {
        return numEntries;
    }

}
