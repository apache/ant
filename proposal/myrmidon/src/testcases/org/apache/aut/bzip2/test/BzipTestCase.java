/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.bzip2.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import junit.framework.TestCase;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.aut.bzip2.CBZip2OutputStream;
import org.apache.aut.bzip2.CBZip2InputStream;

/**
 * A test the stress tested the BZip implementation to verify
 * that it behaves correctly.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class BzipTestCase
    extends TestCase
{
    private final static byte[] HEADER = new byte[]{(byte)'B', (byte)'Z'};

    public BzipTestCase( final String name )
    {
        super( name );
    }

    public void testBzipOutputStream()
        throws Exception
    {
        final InputStream input = getInputStream( "asf-logo-huge.tar" );
        final File outputFile = getOutputFile( ".tar.bz2" );
        final OutputStream output = new FileOutputStream( outputFile );
        final CBZip2OutputStream packedOutput = getPackedOutput( output );
        IOUtil.copy( input, packedOutput );
        IOUtil.shutdownStream( input );
        IOUtil.shutdownStream( packedOutput );
        IOUtil.shutdownStream( output );
        compareContents( "asf-logo-huge.tar.bz2", outputFile );
        FileUtil.forceDelete( outputFile );
    }

    public void testBzipInputStream()
        throws Exception
    {
        final InputStream input = getInputStream( "asf-logo-huge.tar.bz2" );
        final File outputFile = getOutputFile( ".tar" );
        final OutputStream output = new FileOutputStream( outputFile );
        final CBZip2InputStream packedInput = getPackedInput( input );
        IOUtil.copy( packedInput, output );
        IOUtil.shutdownStream( input );
        IOUtil.shutdownStream( packedInput );
        IOUtil.shutdownStream( output );
        compareContents( "asf-logo-huge.tar", outputFile );
        FileUtil.forceDelete( outputFile );
    }

    private void compareContents( final String initial, final File generated )
        throws Exception
    {
        final InputStream input1 = getInputStream( initial );
        final InputStream input2 = new FileInputStream( generated );
        final boolean test = IOUtil.contentEquals( input1, input2 );
        IOUtil.shutdownStream( input1 );
        IOUtil.shutdownStream( input2 );
        assertTrue( "Contents of " + initial + " matches generated version " + generated, test );
    }

    private CBZip2InputStream getPackedInput( final InputStream input )
        throws IOException
    {
        final int b1 = input.read();
        final int b2 = input.read();
        assertEquals( "Equal header byte1", b1, 'B' );
        assertEquals( "Equal header byte2", b2, 'Z' );
        return new CBZip2InputStream( input );
    }

    private CBZip2OutputStream getPackedOutput( final OutputStream output )
        throws IOException
    {
        output.write( HEADER );
        return new CBZip2OutputStream( output );
    }

    private File getOutputFile( final String postfix )
        throws IOException
    {
        final File cwd = new File( "." );
        return File.createTempFile( "ant-test", postfix, cwd );
    }

    private InputStream getInputStream( final String resource )
        throws Exception
    {
        final String filename = "src" + File.separator + "testcases" + File.separator +
            getClass().getName().replace( '.', File.separatorChar );
        final String path = FileUtil.getPath( filename );
        final File input = new File( path, resource );
        return new FileInputStream( input );
        //final ClassLoader loader = getClass().getClassLoader();
        //return loader.getResourceAsStream( resource );
    }
}
