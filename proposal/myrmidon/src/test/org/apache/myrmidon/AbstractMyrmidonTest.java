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
import org.apache.avalon.framework.CascadingThrowable;
import org.apache.avalon.framework.logger.LogKitLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.log.Hierarchy;
import org.apache.log.LogTarget;
import org.apache.log.Priority;
import org.apache.log.format.PatternFormatter;
import org.apache.log.output.io.StreamTarget;

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

    private final static String PATTERN = "[%8.8{category}] %{message}\\n%{throwable}";

    public AbstractMyrmidonTest( String name )
    {
        super( name );
        final String baseDirProp = System.getProperty( "test.basedir" );
        m_baseDir = new File( baseDirProp );
        String packagePath = getClass().getName();
        int idx = packagePath.lastIndexOf('.');
        packagePath = packagePath.substring(0, idx);
        packagePath = packagePath.replace('.', File.separatorChar);
        m_testBaseDir = new File( m_baseDir, packagePath );
    }

    /**
     * Locates a test resource, and asserts that the resource exists
     */
    protected File getTestResource( final String name )
    {
        File file = new File( m_testBaseDir, name );
        file = getCanonicalFile( file );
        assertTrue( "Test file \"" + file + "\" does not exist.", file.exists() );
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
    protected Logger createLogger()
    {
        // Setup a logger
        final Priority priority = Priority.WARN;
        final org.apache.log.Logger targetLogger = Hierarchy.getDefaultHierarchy().getLoggerFor( "myrmidon" );

        final PatternFormatter formatter = new PatternFormatter( PATTERN );
        final StreamTarget target = new StreamTarget( System.out, formatter );
        targetLogger.setLogTargets( new LogTarget[]{target} );
        targetLogger.setPriority( priority );

        return new LogKitLogger( targetLogger );
    }

    /**
     * Asserts that an exception chain contains the expected messages.
     */
    protected void assertSameMessage( final String[] messages, final Throwable throwable )
    {
        Throwable current = throwable;
        for( int i = 0; i < messages.length; i++ )
        {
            String message = messages[ i ];
            assertNotNull( current );
            assertEquals( message, current.getMessage() );

            if( current instanceof CascadingThrowable )
            {
                current = ( (CascadingThrowable)current ).getCause();
            }
            else
            {
                current = null;
            }
        }
    }

    /**
     * Asserts that an exception contains the expected message.
     */
    protected void assertSameMessage( final String message, final Throwable throwable )
    {
        assertEquals( message, throwable.getMessage() );
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
