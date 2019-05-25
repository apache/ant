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
 * A FieldRef CP Info
 *
 */
public class FieldRefCPInfo extends ConstantPoolEntry {
    /** Name of the field's class */
    private String fieldClassName;
    /** name of the field in that class */
    private String fieldName;
    /** The type of the field */
    private String fieldType;
    /** Index into the constant pool for the class */
    private int classIndex;
    /** Index into the constant pool for the name and type entry */
    private int nameAndTypeIndex;

    /** Constructor.  */
    public FieldRefCPInfo() {
        super(CONSTANT_FIELDREF, 1);
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
        ClassCPInfo fieldClass
            = (ClassCPInfo) constantPool.getEntry(classIndex);

        fieldClass.resolve(constantPool);

        fieldClassName = fieldClass.getClassName();

        NameAndTypeCPInfo nt
            = (NameAndTypeCPInfo) constantPool.getEntry(nameAndTypeIndex);

        nt.resolve(constantPool);

        fieldName = nt.getName();
        fieldType = nt.getType();

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
            return "Field : Class = " + fieldClassName + ", name = " + fieldName
                + ", type = " + fieldType;
        }
        return "Field : Class index = " + classIndex
            + ", name and type index = " + nameAndTypeIndex;
    }

    /**
     * Gets the name of the class defining the field
     *
     * @return the name of the class defining the field
     */
    public String getFieldClassName() {
        return fieldClassName;
    }

    /**
     * Get the name of the field
     *
     * @return the field's name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Get the type of the field
     *
     * @return the field's type in string format
     */
    public String getFieldType() {
        return fieldType;
    }

}
