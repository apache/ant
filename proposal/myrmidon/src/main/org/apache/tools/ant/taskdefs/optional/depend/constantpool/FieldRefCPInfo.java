/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.depend.constantpool;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * A FieldRef CP Info
 *
 * @author Conor MacNeill
 */
public class FieldRefCPInfo extends ConstantPoolEntry
{
    private int classIndex;
    private String fieldClassName;
    private String fieldName;
    private String fieldType;
    private int nameAndTypeIndex;

    /**
     * Constructor.
     */
    public FieldRefCPInfo()
    {
        super( CONSTANT_FieldRef, 1 );
    }

    public String getFieldClassName()
    {
        return fieldClassName;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public String getFieldType()
    {
        return fieldType;
    }

    /**
     * read a constant pool entry from a class stream.
     *
     * @param cpStream the DataInputStream which contains the constant pool
     *      entry to be read.
     * @throws IOException if there is a problem reading the entry from the
     *      stream.
     */
    public void read( DataInputStream cpStream )
        throws IOException
    {
        classIndex = cpStream.readUnsignedShort();
        nameAndTypeIndex = cpStream.readUnsignedShort();
    }

    /**
     * Resolve this constant pool entry with respect to its dependents in the
     * constant pool.
     *
     * @param constantPool the constant pool of which this entry is a member and
     *      against which this entry is to be resolved.
     */
    public void resolve( ConstantPool constantPool )
    {
        ClassCPInfo fieldClass = (ClassCPInfo)constantPool.getEntry( classIndex );

        fieldClass.resolve( constantPool );

        fieldClassName = fieldClass.getClassName();

        NameAndTypeCPInfo nt = (NameAndTypeCPInfo)constantPool.getEntry( nameAndTypeIndex );

        nt.resolve( constantPool );

        fieldName = nt.getName();
        fieldType = nt.getType();

        super.resolve( constantPool );
    }

    /**
     * Print a readable version of the constant pool entry.
     *
     * @return the string representation of this constant pool entry.
     */
    public String toString()
    {
        String value;

        if( isResolved() )
        {
            value = "Field : Class = " + fieldClassName + ", name = " + fieldName + ", type = " + fieldType;
        }
        else
        {
            value = "Field : Class index = " + classIndex + ", name and type index = " + nameAndTypeIndex;
        }

        return value;
    }

}

