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
 * A String Constant Pool Entry. The String info contains an index into the
 * constant pool where a UTF8 string is stored.
 *
 * @author Conor MacNeill
 */
public class StringCPInfo extends ConstantCPInfo
{

    private int index;

    /**
     * Constructor.
     */
    public StringCPInfo()
    {
        super( CONSTANT_String, 1 );
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
        index = cpStream.readUnsignedShort();

        setValue( "unresolved" );
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
        setValue( ( (Utf8CPInfo)constantPool.getEntry( index ) ).getValue() );
        super.resolve( constantPool );
    }

    /**
     * Print a readable version of the constant pool entry.
     *
     * @return the string representation of this constant pool entry.
     */
    public String toString()
    {
        return "String Constant Pool Entry for " + getValue() + "[" + index + "]";
    }
}

