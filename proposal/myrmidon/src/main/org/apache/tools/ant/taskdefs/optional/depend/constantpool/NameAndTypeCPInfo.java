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
 * A NameAndType CP Info
 *
 * @author Conor MacNeill
 */
public class NameAndTypeCPInfo extends ConstantPoolEntry
{
    private int descriptorIndex;

    private String name;
    private int nameIndex;
    private String type;

    /**
     * Constructor.
     */
    public NameAndTypeCPInfo()
    {
        super( CONSTANT_NameAndType, 1 );
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
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
        nameIndex = cpStream.readUnsignedShort();
        descriptorIndex = cpStream.readUnsignedShort();
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
        name = ( ( Utf8CPInfo )constantPool.getEntry( nameIndex ) ).getValue();
        type = ( ( Utf8CPInfo )constantPool.getEntry( descriptorIndex ) ).getValue();

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
            value = "Name = " + name + ", type = " + type;
        }
        else
        {
            value = "Name index = " + nameIndex + ", descriptor index = " + descriptorIndex;
        }

        return value;
    }
}

