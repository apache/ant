/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.file;

import java.io.File;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.DataType;
import org.apache.myrmidon.framework.file.ArrayFileList;
import org.apache.myrmidon.framework.file.FileList;
import org.apache.tools.todo.util.FileUtils;
import org.apache.tools.todo.types.FileSet;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.ScannerUtil;

/**
 * This object represents a path as used by CLASSPATH or PATH environment
 * variable. <p>
 *
 * <code>
 * &lt;sometask&gt;<br>
 * &nbsp;&nbsp;&lt;somepath&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement location="/path/to/file.jar" /&gt;
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement
 * path="/path/to/file2.jar:/path/to/class2;/path/to/class3" /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement location="/path/to/file3.jar" /&gt;
 * <br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pathelement location="/path/to/file4.jar" /&gt;
 * <br>
 * &nbsp;&nbsp;&lt;/somepath&gt;<br>
 * &lt;/sometask&gt;<br>
 * </code> <p>
 *
 * The object implemention <code>sometask</code> must provide a method called
 * <code>createSomepath</code> which returns an instance of <code>Path</code>.
 * Nested path definitions are handled by the Path object and must be labeled
 * <code>pathelement</code>.<p>
 *
 * The path element takes a parameter <code>path</code> which will be parsed and
 * split into single elements. It will usually be used to define a path from an
 * environment variable.
 *
 * @author Thomas.Haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 *
 * @ant:data-type name="path"
 */
public class Path
    implements DataType, FileList
{
    private final ArrayList m_elements = new ArrayList();

    public Path( final String path )
    {
        add( path );
    }

    public Path()
    {
    }

    /**
     * Adds a String to the ArrayList if it isn't already included.
     */
    private void addUnlessPresent( final ArrayList list, final String entry )
    {
        if( !list.contains( entry ) )
        {
            list.add( entry );
        }
    }

    /**
     * Adds an element to the path.
     */
    public void setLocation( final File location )
    {
        addLocation( location );
    }

    /**
     * Adds a element definition to the path.
     *
     * @param location the location of the element to add (must not be <code>null</code>
     *      nor empty.
     */
    public void addLocation( final File location )
    {
        final FileList pathElement = new ArrayFileList( location.getAbsolutePath() );
        m_elements.add( pathElement );
    }

    /**
     * Adds a nested <code>&lt;fileset&gt;</code> element.
     */
    public void addFileset( final FileSet fileSet )
    {
        m_elements.add( fileSet );
    }

    /**
     * Adds a path.
     */
    public void setPath( final String path )
    {
        add( path );
    }

    /**
     * Adds a path.
     */
    public void add( final String path )
    {
        final FileList pathElement = new ParsedPathElement( path );
        m_elements.add( pathElement );
    }

    /**
     * Adds a path.
     */
    public void add( final String[] path )
    {
        final FileList pathElement = new ArrayFileList( path );
        m_elements.add( pathElement );
    }

    /**
     * Adds a path.
     */
    public void add( final FileList list )
    {
        m_elements.add( list );
    }

    /**
     * Returns all path elements defined by this and nested path objects.
     * The paths returned by this method are absolute.
     */
    public String[] listFiles( final TaskContext context )
        throws TaskException
    {
        ArrayList result = new ArrayList( 2 * m_elements.size() );
        for( int i = 0; i < m_elements.size(); i++ )
        {
            Object o = m_elements.get( i );
            if( o instanceof FileList )
            {
                final FileList element = (FileList)o;
                final String[] parts = element.listFiles( context );
                for( int j = 0; j < parts.length; j++ )
                {
                    addUnlessPresent( result, parts[ j ] );
                }
            }
            else if( o instanceof FileSet )
            {
                final FileSet fs = (FileSet)o;
                final DirectoryScanner ds = ScannerUtil.getDirectoryScanner( fs );
                final String[] s = ds.getIncludedFiles();
                final File dir = fs.getDir();
                for( int j = 0; j < s.length; j++ )
                {
                    File f = new File( dir, s[ j ] );
                    String absolutePath = f.getAbsolutePath();
                    addUnlessPresent( result, FileUtils.translateFile( absolutePath ) );
                }
            }
        }
        return (String[])result.toArray( new String[ result.size() ] );
    }
}
