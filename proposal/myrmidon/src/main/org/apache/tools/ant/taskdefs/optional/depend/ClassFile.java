/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.depend;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ClassCPInfo;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ConstantPool;
import org.apache.tools.ant.taskdefs.optional.depend.constantpool.ConstantPoolEntry;

/**
 * A ClassFile object stores information about a Java class. The class may be
 * read from a DataInputStream.and written to a DataOutputStream. These are
 * usually streams from a Java class file or a class file component of a Jar
 * file.
 *
 * @author Conor MacNeill
 */
public class ClassFile
{

    /**
     * The Magic Value that marks the start of a Java class file
     */
    private final static int CLASS_MAGIC = 0xCAFEBABE;

    /**
     * The class name for this class.
     */
    private String className;

    /**
     * This class' constant pool.
     */
    private ConstantPool constantPool;

    /**
     * Get the classes which this class references.
     *
     * @return The ClassRefs value
     */
    public Vector getClassRefs()
    {

        Vector classRefs = new Vector();

        for( int i = 0; i < constantPool.size(); ++i )
        {
            ConstantPoolEntry entry = constantPool.getEntry( i );

            if( entry != null && entry.getTag() == ConstantPoolEntry.CONSTANT_Class )
            {
                ClassCPInfo classEntry = (ClassCPInfo)entry;

                if( !classEntry.getClassName().equals( className ) )
                {
                    classRefs.addElement( ClassFileUtils.convertSlashName( classEntry.getClassName() ) );
                }
            }
        }

        return classRefs;
    }

    /**
     * Get the class' fully qualified name in dot format.
     *
     * @return the class name in dot format (eg. java.lang.Object)
     */
    public String getFullClassName()
    {
        return ClassFileUtils.convertSlashName( className );
    }

    /**
     * Read the class from a data stream. This method takes an InputStream as
     * input and parses the class from the stream. <p>
     *
     *
     *
     * @param stream an InputStream from which the class will be read
     * @throws IOException if there is a problem reading from the given stream.
     * @throws ClassFormatError if the class cannot be parsed correctly
     */
    public void read( InputStream stream )
        throws IOException, ClassFormatError
    {
        DataInputStream classStream = new DataInputStream( stream );

        if( classStream.readInt() != CLASS_MAGIC )
        {
            throw new ClassFormatError( "No Magic Code Found - probably not a Java class file." );
        }

        // right we have a good looking class file.
        int minorVersion = classStream.readUnsignedShort();
        int majorVersion = classStream.readUnsignedShort();

        // read the constant pool in and resolve it
        constantPool = new ConstantPool();

        constantPool.read( classStream );
        constantPool.resolve();

        int accessFlags = classStream.readUnsignedShort();
        int thisClassIndex = classStream.readUnsignedShort();
        int superClassIndex = classStream.readUnsignedShort();
        className = ( (ClassCPInfo)constantPool.getEntry( thisClassIndex ) ).getClassName();
    }
}

