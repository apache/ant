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
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.i18n.ResourceManager;
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

    public AbstractMyrmidonTest( final String name )
    {
        super( name );
        final String baseDirProp = System.getProperty( "test.basedir" );
        m_baseDir = getCanonicalFile( new File( baseDirProp ) );
        final String packagePath = getPackageName( getClass() ).replace( '.', File.separatorChar );
        m_testBaseDir = getCanonicalFile( new File( m_baseDir, packagePath ) );
    }

    /**
     * Locates the error message resources for a class.
     */
    protected static final Resources getResourcesForTested( final Class clazz )
    {
        String baseName = getPackageName( clazz );
        if( baseName.endsWith( ".test" ) )
        {
            baseName = baseName.substring( 0, baseName.length() - 5 );
        }

        return ResourceManager.getBaseResources( baseName + ".Resources", AbstractMyrmidonTest.class.getClassLoader() );
    }

    /**
     * Returns the name of the package containing a class.
     *
     * @return The . delimited package name, or an empty string if the class
     *         is in the default package.
     */
    protected static String getPackageName( final Class clazz )
    {
        final Package pkg = clazz.getPackage();
        if( null != pkg )
        {
            return pkg.getName();
        }

        final String name = clazz.getName();
        if( -1 == name.lastIndexOf( "." ) )
        {
            return "";
        }
        else
        {
            return name.substring( 0, name.lastIndexOf( "." ) );
        }
    }

    /**
     * Locates a test resource, and asserts that the resource exists
     *
     * @param name path of the resource, relative to this test's base directory.
     */
    protected File getTestResource( final String name )
    {
        return getTestResource( name, true );
    }

    /**
     * Locates a test resource.
     *
     * @param name path of the resource, relative to this test's base directory.
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
     * Locates the base directory for this test.
     */
    protected File getTestDirectory()
    {
        return m_testBaseDir;
    }

    /**
     * Locates a test directory, creating it if it does not exist.
     *
     * @param name path of the directory, relative to this test's base directory.
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
    protected File getInstallDirectory()
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
        assertSameMessage( new String[]{message}, throwable );
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
