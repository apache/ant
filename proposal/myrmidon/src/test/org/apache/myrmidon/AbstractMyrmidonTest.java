/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon;

import junit.framework.TestCase;
import java.io.File;

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

    public AbstractMyrmidonTest( String name )
    {
        super( name );
        final String baseDirProp = System.getProperty( "test.basedir" );
        String packagePath = getClass().getName();
        int idx = packagePath.lastIndexOf('.');
        packagePath = packagePath.substring(0, idx);
        packagePath = packagePath.replace('.', File.separatorChar);
        m_testBaseDir = new File( baseDirProp, packagePath ).getAbsoluteFile();
    }

    /**
     * Locates a test resource.
     */
    protected File getTestResource( final String name )
    {
        return new File( m_testBaseDir, name );
    }

    /**
     * Asserts that an exception contains the expected message.
     *
     * TODO - should take the expected exception, rather than the message,
     * to check the entire cause chain.
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
