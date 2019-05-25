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
 * A InterfaceMethodRef CP Info
 *
 */
public class InterfaceMethodRefCPInfo extends ConstantPoolEntry {
    /** the class name of the class defining the interface method */
    private String interfaceMethodClassName;
    /** the name of the interface nmethod */
    private String interfaceMethodName;
    /** the method signature of the interface method */
    private String interfaceMethodType;
    /**
     * the index into the constant pool of the class entry for the interface
     * class
     */
    private int classIndex;
    /**
     * the index into the constant pool of the name and type entry
     * describing the method
     */
    private int nameAndTypeIndex;

    /** Constructor. */
    public InterfaceMethodRefCPInfo() {
        super(CONSTANT_INTERFACEMETHODREF, 1);
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
        classIndex = cpStream.readUnsignedShort();
        nameAndTypeIndex = cpStream.readUnsignedShort();
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
        ClassCPInfo interfaceMethodClass
             = (ClassCPInfo) constantPool.getEntry(classIndex);

        interfaceMethodClass.resolve(constantPool);

        interfaceMethodClassName = interfaceMethodClass.getClassName();

        NameAndTypeCPInfo nt
             = (NameAndTypeCPInfo) constantPool.getEntry(nameAndTypeIndex);

        nt.resolve(constantPool);

        interfaceMethodName = nt.getName();
        interfaceMethodType = nt.getType();

        super.resolve(constantPool);
    }

    /**
     * Print a readable version of the constant pool entry.
     *
     * @return the string representation of this constant pool entry.
     */
    @Override
    public String toString() {
        if (isResolved()) {
            return "InterfaceMethod : Class = " + interfaceMethodClassName
                 + ", name = " + interfaceMethodName + ", type = "
                 + interfaceMethodType;
        }
        return "InterfaceMethod : Class index = " + classIndex
             + ", name and type index = " + nameAndTypeIndex;

    }

    /**
     * Gets the name of the class defining the interface method
     *
     * @return the name of the class defining the interface method
     */
    public String getInterfaceMethodClassName() {
        return interfaceMethodClassName;
    }

    /**
     * Get the name of the interface method
     *
     * @return the name of the interface method
     */
    public String getInterfaceMethodName() {
        return interfaceMethodName;
    }

    /**
     * Gets the type of the interface method
     *
     * @return the interface method's type signature
     */
    public String getInterfaceMethodType() {
        return interfaceMethodType;
    }

}
