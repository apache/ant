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
 * A Long CP Info
 *
 * @author Conor MacNeill
 */
public class LongCPInfo extends ConstantCPInfo
{

    /**
     * Constructor.
     */
    public LongCPInfo()
    {
        super( CONSTANT_Long, 2 );
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
        setValue( new Long( cpStream.readLong() ) );
    }

    /**
     * Print a readable version of the constant pool entry.
     *
     * @return the string representation of this constant pool entry.
     */
    public String toString()
    {
        return "Long Constant Pool Entry: " + getValue();
    }

}

