/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

import java.io.File;

/**
 * Tests for the local file system.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class LocalFileSystemTest extends AbstractWritableFileSystemTest
{
    private File m_baseDir;

    public LocalFileSystemTest( String name )
    {
        super( name );
        String baseDir = System.getProperty( "test.local.dir" );
        m_baseDir = new File( baseDir );
    }

    /**
     * Returns the URI for the base folder.
     */
    protected String getBaseFolderURI()
    {
        String testDir = new File( m_baseDir, "read-tests" ).getAbsolutePath();
        String uri = "file:/" + testDir;
        return uri;
    }

    /**
     * Returns the URI for the area to do tests in.
     */
    protected String getWriteFolderURI()
    {
        String testDir = new File( m_baseDir, "write-tests" ).getAbsolutePath();
        String uri = "file:/" + testDir;
        return uri;
    }

    /**
     * Tests resolution of an absolute file name.
     */
    public void testAbsoluteFileName() throws Exception
    {
        // Locate file by absolute file name
        String fileName = new File( "testdir" ).getAbsolutePath();
        FileObject absFile = m_manager.resolveFile( fileName );

        // Locate file by URI
        String uri = "file://" + fileName.replace( File.separatorChar, '/' );
        FileObject uriFile = m_manager.resolveFile( uri );

        assertSame( "file object", absFile, uriFile );
    }

}
