/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.depend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A class file iterator which iterates through the contents of a Java jar file.
 *
 * @author Conor MacNeill
 */
public class JarFileIterator implements ClassFileIterator
{
    private ZipInputStream jarStream;

    public JarFileIterator( InputStream stream )
        throws IOException
    {
        super();

        jarStream = new ZipInputStream( stream );
    }

    public ClassFile getNextClassFile()
    {
        ZipEntry jarEntry;
        ClassFile nextElement = null;

        try
        {
            jarEntry = jarStream.getNextEntry();

            while( nextElement == null && jarEntry != null )
            {
                String entryName = jarEntry.getName();

                if( !jarEntry.isDirectory() && entryName.endsWith( ".class" ) )
                {

                    // create a data input stream from the jar input stream
                    ClassFile javaClass = new ClassFile();

                    javaClass.read( jarStream );

                    nextElement = javaClass;
                }
                else
                {

                    jarEntry = jarStream.getNextEntry();
                }
            }
        }
        catch( IOException e )
        {
            String message = e.getMessage();
            String text = e.getClass().getName();

            if( message != null )
            {
                text += ": " + message;
            }

            throw new RuntimeException( "Problem reading JAR file: " + text );
        }

        return nextElement;
    }

    private byte[] getEntryBytes( InputStream stream )
        throws IOException
    {
        byte[] buffer = new byte[ 8192 ];
        ByteArrayOutputStream baos = new ByteArrayOutputStream( 2048 );
        int n;

        while( ( n = stream.read( buffer, 0, buffer.length ) ) != -1 )
        {
            baos.write( buffer, 0, n );
        }

        return baos.toByteArray();
    }

}

