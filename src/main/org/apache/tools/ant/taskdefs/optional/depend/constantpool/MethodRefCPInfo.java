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
 * A MethodRef CP Info
 *
 */
public class MethodRefCPInfo extends ConstantPoolEntry {
    /** the name of the class defining this method */
    private String methodClassName;
    /** the name of the method */
    private String methodName;
    /** the method's type descriptor */
    private String methodType;
    /** The index into the constant pool which defines the class of this method. */
    private int classIndex;
    /**
     * the index into the constant pool which defined the name and type
     * signature of the method
     */
    private int nameAndTypeIndex;

    /** Constructor. */
    public MethodRefCPInfo() {
        super(CONSTANT_METHODREF, 1);
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
     * Print a readable version of the constant pool entry.
     *
     * @return the string representation of this constant pool entry.
     */
    @Override
    public String toString() {
        if (isResolved()) {
            return "Method : Class = " + methodClassName + ", name = "
                + methodName + ", type = " + methodType;
        }
        return "Method : Class index = " + classIndex
            + ", name and type index = " + nameAndTypeIndex;
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
        ClassCPInfo methodClass
             = (ClassCPInfo) constantPool.getEntry(classIndex);

        methodClass.resolve(constantPool);

        methodClassName = methodClass.getClassName();

        NameAndTypeCPInfo nt
             = (NameAndTypeCPInfo) constantPool.getEntry(nameAndTypeIndex);

        nt.resolve(constantPool);

        methodName = nt.getName();
        methodType = nt.getType();

        super.resolve(constantPool);
    }

    /**
     * Get the name of the class defining the method
     *
     * @return the name of the class defining this method
     */
    public String getMethodClassName() {
        return methodClassName;
    }

    /**
     * Get the name of the method.
     *
     * @return the name of the method.
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Get the type signature of the method.
     *
     * @return the type signature of the method.
     */
    public String getMethodType() {
        return methodType;
    }

}
