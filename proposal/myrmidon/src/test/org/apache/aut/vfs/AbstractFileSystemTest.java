/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.apache.aut.vfs.impl.DefaultFileSystemManager;

/**
 * File system test cases, which verifies the structure and naming
 * functionality.
 *
 * Works from a base folder, and assumes a particular structure under
 * that base folder.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public abstract class AbstractFileSystemTest extends TestCase
{
    protected FileObject m_baseFolder;
    protected DefaultFileSystemManager m_manager;

    // Contents of "file1.txt"
    private String m_charContent;

    public AbstractFileSystemTest( String name )
    {
        super( name );
    }

    /**
     * Builds the expected folder structure.
     */
    private FileInfo buildExpectedStructure()
    {
        // Build the expected structure
        FileInfo base = new FileInfo( "test", FileType.FOLDER );
        base.addChild( new FileInfo( "file1.txt", FileType.FILE ) );
        base.addChild( new FileInfo( "empty.txt", FileType.FILE ) );
        base.addChild( new FileInfo( "emptydir", FileType.FOLDER ) );

        FileInfo dir = new FileInfo( "dir1", FileType.FOLDER );
        base.addChild( dir );
        dir.addChild( new FileInfo( "file1.txt", FileType.FILE ) );
        dir.addChild( new FileInfo( "file2.txt", FileType.FILE ) );
        dir.addChild( new FileInfo( "file3.txt", FileType.FILE ) );
        return base;
    }

    /**
     * Returns the URI for the base folder.
     */
    protected abstract String getBaseFolderURI();

    /**
     * Sets up the test
     */
    protected void setUp() throws Exception
    {
        // Create the file system manager
        m_manager = new DefaultFileSystemManager();

        // Locate the base folder
        m_baseFolder = m_manager.resolveFile( getBaseFolderURI() );

        // Build the expected content of "file1.txt"
        String eol = System.getProperty( "line.separator" );
        m_charContent = "This is a test file." + eol + "With 2 lines in it." + eol;
    }

    /**
     * Tests resolution of absolute URI.
     */
    public void testAbsoluteURI() throws Exception
    {
        // Try fetching base folder again by its URI
        String uri = m_baseFolder.getName().getURI();
        FileObject file = m_manager.resolveFile( uri );

        assertSame( "file object", m_baseFolder, file );
    }

    /**
     * Tests resolution of relative file names via the FS manager
     */
    public void testRelativeURI() throws Exception
    {
        // Build base dir
        m_manager.setBaseFile( m_baseFolder );

        // Locate the base dir
        FileObject file = m_manager.resolveFile( "." );
        assertSame( "file object", m_baseFolder, file );

        // Locate a child
        file = m_manager.resolveFile( "some-child" );
        assertSame( "file object", m_baseFolder, file.getParent() );

        // Locate a descendent
        file = m_manager.resolveFile( "some-folder/some-file" );
        assertSame( "file object", m_baseFolder, file.getParent().getParent() );

        // Locate parent
        file = m_manager.resolveFile( ".." );
        assertSame( "file object", m_baseFolder.getParent(), file );
    }

    /**
     * Tests the root file name.
     */
    public void testRootFileName() throws Exception
    {
        // Locate the root file
        FileName rootName = m_baseFolder.getRoot().getName();

        // Test that the root path is "/"
        assertEquals( "root path", "/", rootName.getPath() );

        // Test that the root basname is ""
        assertEquals( "root base name", "", rootName.getBaseName() );

        // Test that the root name has no parent
        assertNull( "root parent", rootName.getParent() );
    }

    /**
     * Tests child file names.
     */
    public void testChildName() throws Exception
    {
        FileName baseName = m_baseFolder.getName();
        String basePath = baseName.getPath();
        FileName name = baseName.resolveName( "some-child", NameScope.CHILD );

        // Test path is absolute
        assertTrue( "is absolute", basePath.startsWith( "/" ) );

        // Test base name
        assertEquals( "base name", "some-child", name.getBaseName() );

        // Test absolute path
        assertEquals( "absolute path", basePath + "/some-child", name.getPath() );

        // Test parent path
        assertEquals( "parent absolute path", basePath, name.getParent().getPath() );

        // Try using a compound name to find a child
        try
        {
            FileName name2 = name.resolveName( "a/b", NameScope.CHILD );
            assertTrue( false );
        }
        catch( FileSystemException e )
        {
        }

        // Try using a empty name to find a child
        try
        {
            FileName name2 = name.resolveName( "", NameScope.CHILD );
            assertTrue( false );
        }
        catch( FileSystemException e )
        {
        }

        // Try using '.' to find a child
        try
        {
            FileName name2 = name.resolveName( ".", NameScope.CHILD );
            assertTrue( false );
        }
        catch( FileSystemException e )
        {
        }

        // Try using '..' to find a child
        try
        {
            FileName name2 = name.resolveName( "..", NameScope.CHILD );
            assertTrue( false );
        }
        catch( FileSystemException e )
        {
        }
    }

    /**
     * Checks that a relative name resolves to the expected absolute path.
     */
    private void assertSameName(String expectedPath,
                                FileName baseName,
                                String relName ) throws Exception
    {
        FileName name = baseName.resolveName(relName);
        assertEquals( expectedPath, name.getPath() );

        // Replace the separators
        relName.replace('\\', '/');
        name = baseName.resolveName(relName);
        assertEquals( expectedPath, name.getPath() );

        // And again
        relName.replace('/', '\\');
        name = baseName.resolveName(relName);
        assertEquals( expectedPath, name.getPath() );
    }

    /**
     * Tests relative name resolution, relative to the base folder.
     */
    public void testNameResolution() throws Exception
    {
        FileName baseName = m_baseFolder.getName();
        String parentPath = baseName.getParent().getPath();
        String path = baseName.getPath();
        String childPath = path + "/some-child";

        // Test empty relative path
        assertSameName( path, baseName, "" );

        // Test . relative path
        assertSameName( path, baseName, "." );

        // Test ./ relative path
        assertSameName( path, baseName, "./" );

        // Test .// relative path
        assertSameName( path, baseName, ".//" );

        // Test .///.///. relative path
        assertSameName( path, baseName, ".///.///." );
        assertSameName( path, baseName, "./\\/.\\//." );

        // Test <elem>/.. relative path
        assertSameName( path, baseName, "a/.." );

        // Test .. relative path
        assertSameName( parentPath, baseName, ".." );

        // Test ../ relative path
        assertSameName( parentPath, baseName, "../" );

        // Test ..//./ relative path
        assertSameName( parentPath, baseName, "..//./" );
        assertSameName( parentPath, baseName, "..//.\\" );

        // Test <elem>/../.. relative path
        assertSameName( parentPath, baseName, "a/../.." );

        // Test <elem> relative path
        assertSameName( childPath, baseName, "some-child" );

        // Test ./<elem> relative path
        assertSameName( childPath, baseName, "./some-child" );

        // Test ./<elem>/ relative path
        assertSameName( childPath, baseName, "./some-child/" );

        // Test <elem>/././././ relative path
        assertSameName( childPath, baseName, "./some-child/././././" );

        // Test <elem>/../<elem> relative path
        assertSameName( childPath, baseName, "a/../some-child" );

        // Test <elem>/<elem>/../../<elem> relative path
        assertSameName( childPath, baseName, "a/b/../../some-child" );
    }

    /**
     * Tests relative name resolution, relative to the root file.
     */
    public void testNameResolutionRoot() throws Exception
    {
        FileName rootName = m_baseFolder.getRoot().getName();
    }

    /**
     * Walks the folder structure, asserting it contains exactly the
     * expected files and folders.
     */
    public void testStructure() throws Exception
    {
        // Setup the structure
        List queueExpected = new ArrayList();
        FileInfo baseInfo = buildExpectedStructure();
        queueExpected.add( baseInfo );

        List queueActual = new ArrayList();
        queueActual.add( m_baseFolder );

        while( queueActual.size() > 0 )
        {
            FileObject file = (FileObject)queueActual.remove( 0 );
            FileInfo info = (FileInfo)queueExpected.remove( 0 );

            // Check the type is correct
            assertSame( file.getType(), info._type );

            if( info._type == FileType.FILE )
            {
                continue;
            }

            // Check children
            FileObject[] children = file.getChildren();

            // Make sure all children were found
            assertNotNull( children );
            assertEquals( "count children of \"" + file.getName() + "\"", info._children.size(), children.length );

            // Recursively check each child
            for( int i = 0; i < children.length; i++ )
            {
                FileObject child = children[ i ];
                FileInfo childInfo = (FileInfo)info._children.get( child.getName().getBaseName() );

                // Make sure the child is expected
                assertNotNull( childInfo );

                // Add to the queue of files to check
                queueExpected.add( childInfo );
                queueActual.add( child );
            }
        }
    }

    /**
     * Tests existence determination.
     */
    public void testExists() throws Exception
    {
        // Test a file
        FileObject file = m_baseFolder.resolveFile( "file1.txt" );
        assertTrue( "file exists", file.exists() );

        // Test a folder
        file = m_baseFolder.resolveFile( "dir1" );
        assertTrue( "folder exists", file.exists() );

        // Test an unknown file
        file = m_baseFolder.resolveFile( "unknown-child" );
        assertTrue( "unknown file does not exist", !file.exists() );

        // Test an unknown file in an unknown folder
        file = m_baseFolder.resolveFile( "unknown-folder/unknown-child" );
        assertTrue( "unknown file does not exist", !file.exists() );
    }

    /**
     * Tests type determination.
     */
    public void testType() throws Exception
    {
        // Test a file
        FileObject file = m_baseFolder.resolveFile( "file1.txt" );
        assertSame( FileType.FILE, file.getType() );

        // Test a folder
        file = m_baseFolder.resolveFile( "dir1" );
        assertSame( FileType.FOLDER, file.getType() );

        // Test an unknown file
        file = m_baseFolder.resolveFile( "unknown-child" );
        FileSystemException exc = null;
        try
        {
            file.getType();
        }
        catch( FileSystemException e )
        {
            exc = e;
        }
        assertNotNull( exc );
    }

    /**
     * Tests parent identity
     */
    public void testParent() throws FileSystemException
    {
        // Test when both exist
        FileObject folder = m_baseFolder.resolveFile( "dir1" );
        FileObject child = folder.resolveFile( "file3.txt" );
        assertTrue( "folder exists", folder.exists() );
        assertTrue( "child exists", child.exists() );
        assertSame( folder, child.getParent() );

        // Test when file does not exist
        child = folder.resolveFile( "unknown-file" );
        assertTrue( "folder exists", folder.exists() );
        assertTrue( "child does not exist", !child.exists() );
        assertSame( folder, child.getParent() );

        // Test when neither exists
        folder = m_baseFolder.resolveFile( "unknown-folder" );
        child = folder.resolveFile( "unknown-file" );
        assertTrue( "folder does not exist", !folder.exists() );
        assertTrue( "child does not exist", !child.exists() );
        assertSame( folder, child.getParent() );

        // Test root of the file system has no parent
        FileObject root = m_baseFolder.getRoot();
        assertNull( "root has null parent", root.getParent() );
    }

    /**
     * Tests that children cannot be listed for non-folders.
     */
    public void testChildren() throws FileSystemException
    {
        // Check for file
        FileObject file = m_baseFolder.resolveFile( "file1.txt" );
        assertSame( FileType.FILE, file.getType() );
        try
        {
            file.getChildren();
            assertTrue( false );
        }
        catch( FileSystemException e )
        {
        }

        // Should be able to get child by name
        file = file.resolveFile( "some-child" );
        assertNotNull( file );

        // Check for unknown file
        file = m_baseFolder.resolveFile( "unknown-file" );
        assertTrue( !file.exists() );
        try
        {
            file.getChildren();
            assertTrue( false );
        }
        catch( FileSystemException e )
        {
        }

        // Should be able to get child by name
        FileObject child = file.resolveFile( "some-child" );
        assertNotNull( child );
    }

    /**
     * Tests content.
     */
    public void testContent() throws Exception
    {
        // Test non-empty file
        FileObject file = m_baseFolder.resolveFile( "file1.txt" );
        FileContent content = file.getContent();
        assertSameContent(m_charContent, content);

        // Test empty file
        file = m_baseFolder.resolveFile( "empty.txt" );
        content = file.getContent();
        assertSameContent("", content);
    }

    /**
     * Asserts that the content of a file is the same as expected. Checks the
     * length reported by getSize() is correct, then reads the content as
     * a byte stream, and as a char stream, and compares the result with
     * the expected content.  Assumes files are encoded using UTF-8.
     */
    public void assertSameContent( String expected, FileContent content ) throws Exception
    {
        // Get file content as a binary stream
        byte[] expectedBin = expected.getBytes( "utf-8" );

        // Check lengths
        assertEquals( "same content length", expectedBin.length, content.getSize() );

        // Read content into byte array
        InputStream instr = content.getInputStream();
        ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        byte[] buffer = new byte[ 256 ];
        int nread = 0;
        while( nread >= 0 )
        {
            outstr.write( buffer, 0, nread );
            nread = instr.read( buffer );
        }

        // Compare
        assertTrue( "same binary content", Arrays.equals( expectedBin, outstr.toByteArray() ) );
    }

    /**
     * Tests that folders and unknown files have no content.
     */
    public void testNoContent() throws Exception
    {
        // Try getting the content of a folder
        FileObject folder = m_baseFolder.resolveFile( "dir1" );
        try
        {
            folder.getContent();
            assertTrue( false );
        }
        catch( FileSystemException e )
        {
        }

        // Try getting the content of an unknown file
        FileObject unknownFile = m_baseFolder.resolveFile( "unknown-file" );
        FileContent content = unknownFile.getContent();
        try
        {
            content.getInputStream();
            assertTrue( false );
        }
        catch( FileSystemException e )
        {
        }
        try
        {
            content.getSize();
            assertTrue( false );
        }
        catch( FileSystemException e )
        {
        }
    }

    /**
     * Tests that content and file objects are usable after being closed.
     */
    public void testReuse() throws Exception
    {
        // Get the test file
        FileObject file = m_baseFolder.resolveFile( "file1.txt" );
        assertEquals( FileType.FILE, file.getType() );

        // Get the file content
        FileContent content = file.getContent();
        assertSameContent( m_charContent, content );

        // Read the content again
        content = file.getContent();
        assertSameContent( m_charContent, content );

        // Close the content + file
        content.close();
        file.close();

        // Read the content again
        content = file.getContent();
        assertSameContent( m_charContent, content );
    }

    /**
     * Info about a file.
     */
    private static final class FileInfo
    {
        String _baseName;
        FileType _type;
        Map _children = new HashMap();

        public FileInfo( String name, FileType type )
        {
            _baseName = name;
            _type = type;
        }

        /** Adds a child. */
        public void addChild( FileInfo child )
        {
            _children.put( child._baseName, child );
        }
    }
}
