/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.types;

import java.io.File;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.DataType;
import org.apache.tools.todo.util.FileUtils;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.FileSet;

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
 */
public class Path
    implements DataType
{
    private final ArrayList m_elements = new ArrayList();
    private File m_baseDirectory;

    public Path( final String path )
    {
        addPath( path );
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
     * Sets the base directory for this path.
     */
    public void setBaseDirectory( final File baseDir )
    {
        m_baseDirectory = baseDir;
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
        final PathElement pathElement = new PathElement();
        m_elements.add( pathElement );
        pathElement.setLocation( location );
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
        addPath( path );
    }

    /**
     * Adds a path.
     */
    public void addPath( final String path )
    {
        final PathElement pathElement = new PathElement();
        m_elements.add( pathElement );
        pathElement.setPath( path );
    }

    /**
     * Creates a nested <code>&lt;path&gt;</code> element.
     */
    public void addPath( final Path path )
    {
        m_elements.add( path );
    }

    /**
     * Returns all path elements defined by this and nested path objects.
     * The paths returned by this method are absolute.
     */
    public String[] list()
        throws TaskException
    {
        ArrayList result = new ArrayList( 2 * m_elements.size() );
        for( int i = 0; i < m_elements.size(); i++ )
        {
            Object o = m_elements.get( i );
            if( o instanceof String )
            {
                // obtained via append
                addUnlessPresent( result, (String)o );
            }
            else if( o instanceof PathElement )
            {
                final PathElement element = (PathElement)o;
                final String[] parts = element.getParts( m_baseDirectory );
                if( parts == null )
                {
                    throw new NullPointerException( "You must either set location or path on <pathelement>" );
                }
                for( int j = 0; j < parts.length; j++ )
                {
                    addUnlessPresent( result, parts[ j ] );
                }
            }
            else if( o instanceof Path )
            {
                Path p = (Path)o;
                String[] parts = p.list();
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

    /**
     * Determines if this path is empty.
     */
    public boolean isEmpty()
        throws TaskException
    {
        return ( list().length == 0 );
    }
}
