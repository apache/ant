/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon;

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import org.apache.avalon.framework.ExceptionUtil;
import org.apache.avalon.framework.logger.Logger;
import org.apache.myrmidon.frontends.BasicLogger;

/**
 * A base class for Myrmidon tests.  Provides utility methods for locating
 * test resources.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public abstract class AbstractMyrmidonTest
    extends TestCase
{
    private final File m_testBaseDir;
    private final File m_baseDir;
    private Logger m_logger;

    public AbstractMyrmidonTest( String name )
    {
        super( name );
        final String baseDirProp = System.getProperty( "test.basedir" );
        m_baseDir = new File( baseDirProp );
        String packagePath = getClass().getName();
        int idx = packagePath.lastIndexOf( '.' );
        packagePath = packagePath.substring( 0, idx );
        packagePath = packagePath.replace( '.', File.separatorChar );
        m_testBaseDir = new File( m_baseDir, packagePath );
    }

    /**
     * Locates a test resource, and asserts that the resource exists
     */
    protected File getTestResource( final String name )
    {
        return getTestResource( name, true );
    }

    /**
     * Locates a test resource.
     */
    protected File getTestResource( final String name, final boolean mustExist )
    {
        File file = new File( m_testBaseDir, name );
        file = getCanonicalFile( file );
        if( mustExist )
        {
            assertTrue( "Test file \"" + file + "\" does not exist.", file.exists() );
        }
        else
        {
            assertTrue( "Test file \"" + file + "\" should not exist.", !file.exists() );
        }

        return file;
    }

    /**
     * Locates a test directory, creating it if it does not exist.
     */
    protected File getTestDirectory( final String name )
    {
        File file = new File( m_testBaseDir, name );
        file = getCanonicalFile( file );

        assertTrue( "Test directory \"" + file + "\" does not exist or is not a directory.",
                    file.isDirectory() || file.mkdirs() );
        return file;
    }

    /**
     * Returns the directory containing a Myrmidon install.
     */
    protected File getHomeDirectory()
    {
        final File file = new File( m_baseDir, "dist" );
        return getCanonicalFile( file );
    }

    /**
     * Makes a file canonical
     */
    private File getCanonicalFile( final File file )
    {
        try
        {
            return file.getCanonicalFile();
        }
        catch( IOException e )
        {
            return file.getAbsoluteFile();
        }
    }

    /**
     * Creates a logger.
     */
    protected Logger getLogger()
    {
        if( m_logger == null )
        {
            m_logger = new BasicLogger( "[test]", BasicLogger.LEVEL_WARN );
        }
        return m_logger;
    }

    /**
     * Asserts that an exception chain contains the expected messages.
     *
     * @param messages The messages, in order.  A null entry in this array
     *                 indicates that the message should be ignored.
     */
    protected void assertSameMessage( final String[] messages, final Throwable throwable )
    {
        //System.out.println( "exception:" );
        //for( Throwable t = throwable; t != null; t = ExceptionUtil.getCause( t, true ) )
        //{
        //    System.out.println( "  " + t.getMessage() );
        //}

        Throwable current = throwable;
        for( int i = 0; i < messages.length; i++ )
        {
            String message = messages[ i ];
            assertNotNull( current );
            if( message != null )
            {
                assertEquals( message, current.getMessage() );
            }

            // Get the next exception in the chain
            current = ExceptionUtil.getCause( current, true );
        }
    }

    /**
     * Asserts that an exception contains the expected message.
     */
    protected void assertSameMessage( final String message, final Throwable throwable )
    {
        assertSameMessage( new String[] { message }, throwable );
    }

    /**
     * Compares 2 objects for equality, nulls are equal.  Used by the test
     * classes' equals() methods.
     */
    public static boolean equals( final Object o1, final Object o2 )
    {
        if( o1 == null && o2 == null )
        {
            return true;
        }
        if( o1 == null || o2 == null )
        {
            return false;
        }
        return o1.equals( o2 );
    }

}
