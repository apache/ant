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
 * A MethodRef CP Info
 *
 * @author Conor MacNeill
 */
public class MethodRefCPInfo extends ConstantPoolEntry
{
    private int classIndex;
    private String methodClassName;
    private String methodName;
    private String methodType;
    private int nameAndTypeIndex;

    /**
     * Constructor.
     */
    public MethodRefCPInfo()
    {
        super( CONSTANT_MethodRef, 1 );
    }

    public String getMethodClassName()
    {
        return methodClassName;
    }

    public String getMethodName()
    {
        return methodName;
    }

    public String getMethodType()
    {
        return methodType;
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
        ClassCPInfo methodClass = ( ClassCPInfo )constantPool.getEntry( classIndex );

        methodClass.resolve( constantPool );

        methodClassName = methodClass.getClassName();

        NameAndTypeCPInfo nt = ( NameAndTypeCPInfo )constantPool.getEntry( nameAndTypeIndex );

        nt.resolve( constantPool );

        methodName = nt.getName();
        methodType = nt.getType();

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
            value = "Method : Class = " + methodClassName + ", name = " + methodName + ", type = " + methodType;
        }
        else
        {
            value = "Method : Class index = " + classIndex + ", name and type index = " + nameAndTypeIndex;
        }

        return value;
    }

}

