/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.jlink;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides a quick and dirty way to determine the true name of a class given
 * just an InputStream. Reads in just enough to perform this minimal task only.
 *
 * @author RT
 */
public class ClassNameReader extends Object
{

    public static String getClassName( InputStream input )
        throws IOException
    {
        DataInputStream data = new DataInputStream( input );
        // verify this is a valid class file.
        int cookie = data.readInt();
        if( cookie != 0xCAFEBABE )
        {
            return null;
        }
        int version = data.readInt();
        // read the constant pool.
        ConstantPool constants = new ConstantPool( data );
        Object[] values = constants.values;
        // read access flags and class index.
        int accessFlags = data.readUnsignedShort();
        int classIndex = data.readUnsignedShort();
        Integer stringIndex = (Integer)values[ classIndex ];
        String className = (String)values[ stringIndex.intValue() ];
        return className;
    }

}

/**
 * Reads just enough of a class file to determine the class' full name. <p>
 *
 * Extremely minimal constant pool implementation, mainly to support extracting
 * strings from a class file.
 *
 * @author <a href="mailto:beard@netscape.com">Patrick C. Beard</a> .
 */
class ConstantPool extends Object
{

    final static byte UTF8 = 1, UNUSED = 2, INTEGER = 3, FLOAT = 4, LONG = 5, DOUBLE = 6,
    CLASS = 7, STRING = 8, FIELDREF = 9, METHODREF = 10,
    INTERFACEMETHODREF = 11, NAMEANDTYPE = 12;

    byte[] types;

    Object[] values;

    ConstantPool( DataInput data )
        throws IOException
    {
        super();

        int count = data.readUnsignedShort();
        types = new byte[ count ];
        values = new Object[ count ];
        // read in all constant pool entries.
        for( int i = 1; i < count; i++ )
        {
            byte type = data.readByte();
            types[ i ] = type;
            switch( type )
            {
                case UTF8:
                    values[ i ] = data.readUTF();
                    break;
                case UNUSED:
                    break;
                case INTEGER:
                    values[ i ] = new Integer( data.readInt() );
                    break;
                case FLOAT:
                    values[ i ] = new Float( data.readFloat() );
                    break;
                case LONG:
                    values[ i ] = new Long( data.readLong() );
                    ++i;
                    break;
                case DOUBLE:
                    values[ i ] = new Double( data.readDouble() );
                    ++i;
                    break;
                case CLASS:
                case STRING:
                    values[ i ] = new Integer( data.readUnsignedShort() );
                    break;
                case FIELDREF:
                case METHODREF:
                case INTERFACEMETHODREF:
                case NAMEANDTYPE:
                    values[ i ] = new Integer( data.readInt() );
                    break;
            }
        }
    }

}

