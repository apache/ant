/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.file.test;

import java.io.File;
import org.apache.aut.nativelib.PathUtil;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.myrmidon.AbstractProjectTest;
import org.apache.myrmidon.LogMessageTracker;

/**
 * Test-cases for the <path> data type.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class PathTestCase
    extends AbstractProjectTest
{
    public PathTestCase( final String name )
    {
        super( name );
    }

    /**
     * Tests setting the location attribute.
     */
    public void testLocationAttribute() throws Exception
    {
        testPathContent( "set-location", new String[]{"location"} );
    }

    /**
     * Tests setting the path attribute.
     */
    public void testPathAttribute() throws Exception
    {
        // Test a path with a single file
        testPathContent( "set-path", new String[]{"single-file"} );

        // Test a path with several files, using ; separator
        testPathContent( "set-multi-path", new String[]{"file1", "file2", ".."} );

        // Test a path with several files, using : separator
        testPathContent( "set-multi-path2", new String[]{"file1", "file2", ".."} );
    }

    /**
     * Test using nested <path> elements.
     */
    public void testPathElement() throws Exception
    {
        testPathContent( "nested-path", new String[]{"some-file"} );
        testPathContent( "mixed-path", new String[]{"file1", "file2", "file3", "file4", "file5"} );
    }

    /**
     * Test using nested <fileset> elements.
     */
    public void testFilesetElement() throws Exception
    {
        testPathContent( "set-fileset", new String[]{"path.ant"} );
    }

    /**
     * Test using a nested custom file list implementation.
     */
    public void testCustomFileList() throws Exception
    {
        testPathContent( "test-custom-file-list", new String[]{"file1"} );
    }

    /**
     * Test converting between string and path.
     */
    public void testConvert() throws Exception
    {
        testPathContent( "convert-string-to-path", new String[]{"file1", "file2"} );

        // Test conversion from path -> string
        final File[] files = {
            getTestResource( "file1", false ),
            getTestResource( "file2", false )
        };
        final String path = PathUtil.formatPath( files );
        final LogMessageTracker listener = new LogMessageTracker();
        listener.addExpectedMessage( "convert-path-to-string", "test-path = " + path );

        final File projectFile = getTestResource( "path.ant" );
        executeTarget( projectFile, "convert-path-to-string", listener );
    }

    /**
     * Executes a target, and asserts that a particular list of file names
     * is logged.
     */
    private void testPathContent( final String targetName,
                                  final String[] files ) throws Exception
    {
        final File projectFile = getTestResource( "path.ant" );
        final File baseDir = projectFile.getParentFile();

        // Add each of the expected file names
        final LogMessageTracker listener = new LogMessageTracker();
        for( int i = 0; i < files.length; i++ )
        {
            final String fileName = files[ i ];
            final File file = FileUtil.resolveFile( baseDir, fileName );
            listener.addExpectedMessage( targetName, file.getAbsolutePath() );
        }

        // Execute the target
        executeTarget( projectFile, targetName, listener );
    }
}
