/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.sitraka.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ClassCPInfo;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ConstantPool;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.Utf8CPInfo;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.attributes.AttributeInfo;

/**
 * Object representing a class. Information are kept to the strict minimum for
 * JProbe reports so that not too many objects are created for a class,
 * otherwise the JVM can quickly run out of memory when analyzing a great deal
 * of classes and keeping them in memory for global analysis.
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public final class ClassFile
{

    private int access_flags;

    private String fullname;

    private MethodInfo[] methods;

    private String sourceFile;

    public ClassFile( InputStream is )
        throws IOException
    {
        DataInputStream dis = new DataInputStream( is );
        ConstantPool constantPool = new ConstantPool();

        int magic = dis.readInt();// 0xCAFEBABE
        int minor = dis.readShort();
        int major = dis.readShort();

        constantPool.read( dis );
        constantPool.resolve();

        // class information
        access_flags = dis.readShort();
        int this_class = dis.readShort();
        fullname = ( (ClassCPInfo)constantPool.getEntry( this_class ) ).getClassName().replace( '/', '.' );
        int super_class = dis.readShort();

        // skip interfaces...
        int count = dis.readShort();
        dis.skipBytes( count * 2 );// short

        // skip fields...
        int numFields = dis.readShort();
        for( int i = 0; i < numFields; i++ )
        {
            // 3 short: access flags, name index, descriptor index
            dis.skip( 2 * 3 );
            // attribute list...
            int attributes_count = dis.readUnsignedShort();
            for( int j = 0; j < attributes_count; j++ )
            {
                dis.skipBytes( 2 );// skip attr_id (short)
                int len = dis.readInt();
                dis.skipBytes( len );
            }
        }

        // read methods
        int method_count = dis.readShort();
        methods = new MethodInfo[ method_count ];
        for( int i = 0; i < method_count; i++ )
        {
            methods[ i ] = new MethodInfo();
            methods[ i ].read( constantPool, dis );
        }

        // get interesting attributes.
        int attributes_count = dis.readUnsignedShort();
        for( int j = 0; j < attributes_count; j++ )
        {
            int attr_id = dis.readShort();
            int len = dis.readInt();
            String attr_name = Utils.getUTF8Value( constantPool, attr_id );
            if( AttributeInfo.SOURCE_FILE.equals( attr_name ) )
            {
                int name_index = dis.readShort();
                sourceFile = ( (Utf8CPInfo)constantPool.getEntry( name_index ) ).getValue();
            }
            else
            {
                dis.skipBytes( len );
            }
        }
    }

    public int getAccess()
    {
        return access_flags;
    }

    public String getFullName()
    {
        return fullname;
    }

    public MethodInfo[] getMethods()
    {
        return methods;
    }

    public String getName()
    {
        String name = getFullName();
        int pos = name.lastIndexOf( '.' );
        if( pos == -1 )
        {
            return "";
        }
        return name.substring( pos + 1 );
    }

    public String getPackage()
    {
        String name = getFullName();
        int pos = name.lastIndexOf( '.' );
        if( pos == -1 )
        {
            return "";
        }
        return name.substring( 0, pos );
    }

    public String getSourceFile()
    {
        return sourceFile;
    }

}



