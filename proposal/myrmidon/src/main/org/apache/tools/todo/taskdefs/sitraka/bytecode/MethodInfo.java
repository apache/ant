/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.sitraka.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ConstantPool;
import org.apache.tools.todo.taskdefs.sitraka.bytecode.attributes.AttributeInfo;

/**
 * Method info structure.
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 * @todo give a more appropriate name to methods.
 */
public final class MethodInfo
{
    private int loc = -1;
    private int access_flags;
    private String descriptor;
    private String name;

    public MethodInfo()
    {
    }

    public String getAccess()
    {
        return Utils.getMethodAccess( access_flags );
    }

    public int getAccessFlags()
    {
        return access_flags;
    }

    public String getDescriptor()
    {
        return descriptor;
    }

    public String getFullSignature()
    {
        return getReturnType() + " " + getShortSignature();
    }

    public String getName()
    {
        return name;
    }

    public int getNumberOfLines()
    {
        return loc;
    }

    public String[] getParametersType()
    {
        return Utils.getMethodParams( getDescriptor() );
    }

    public String getReturnType()
    {
        return Utils.getMethodReturnType( getDescriptor() );
    }

    public String getShortSignature()
    {
        StringBuffer buf = new StringBuffer( getName() );
        buf.append( "(" );
        String[] params = getParametersType();
        for( int i = 0; i < params.length; i++ )
        {
            buf.append( params[ i ] );
            if( i != params.length - 1 )
            {
                buf.append( ", " );
            }
        }
        buf.append( ")" );
        return buf.toString();
    }

    public void read( ConstantPool constantPool, DataInputStream dis )
        throws IOException
    {
        access_flags = dis.readShort();

        int name_index = dis.readShort();
        name = Utils.getUTF8Value( constantPool, name_index );

        int descriptor_index = dis.readShort();
        descriptor = Utils.getUTF8Value( constantPool, descriptor_index );

        int attributes_count = dis.readUnsignedShort();
        for( int i = 0; i < attributes_count; i++ )
        {
            int attr_id = dis.readShort();
            String attr_name = Utils.getUTF8Value( constantPool, attr_id );
            int len = dis.readInt();
            if( AttributeInfo.CODE.equals( attr_name ) )
            {
                readCode( constantPool, dis );
            }
            else
            {
                dis.skipBytes( len );
            }
        }

    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "Method: " ).append( getAccess() ).append( " " );
        sb.append( getFullSignature() );
        return sb.toString();
    }

    protected void readCode( ConstantPool constantPool, DataInputStream dis )
        throws IOException
    {
        // skip max_stack (short), max_local (short)
        dis.skipBytes( 2 * 2 );

        // skip bytecode...
        int bytecode_len = dis.readInt();
        dis.skip( bytecode_len );

        // skip exceptions... 1 exception = 4 short.
        int exception_count = dis.readShort();
        dis.skipBytes( exception_count * 4 * 2 );

        // read attributes...
        int attributes_count = dis.readUnsignedShort();
        for( int i = 0; i < attributes_count; i++ )
        {
            int attr_id = dis.readShort();
            String attr_name = Utils.getUTF8Value( constantPool, attr_id );
            int len = dis.readInt();
            if( AttributeInfo.LINE_NUMBER_TABLE.equals( attr_name ) )
            {
                // we're only interested in lines of code...
                loc = dis.readShort();
                // skip the table which is 2*loc*short
                dis.skip( loc * 2 * 2 );
            }
            else
            {
                dis.skipBytes( len );
            }
        }
    }
}

