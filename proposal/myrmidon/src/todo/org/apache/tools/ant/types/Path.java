/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.io.File;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.DataType;
import org.apache.tools.ant.util.FileUtils;

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
    private ArrayList m_elements = new ArrayList();
    private File m_baseDirectory;

    /**
     * Invoked by IntrospectionHelper for <code>setXXX(Path p)</code> attribute
     * setters.
     */
    public Path( final String path )
    {
        final PathElement pathElement = new PathElement();
        m_elements.add( pathElement );
        pathElement.setPath( path );
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
     * Adds the components on the given path which exist to this Path.
     * Components that don't exist, aren't added.
     *
     * @param source - source path whose components are examined for existence
     */
    public void addExisting( final Path source )
        throws TaskException
    {
        final String[] list = source.list();
        for( int i = 0; i < list.length; i++ )
        {
            final File file = new File( list[ i ] );
            if( file.exists() )
            {
                addLocation( file );
            }
        }
    }

    /**
     * Adds a nested <code>&lt;fileset&gt;</code> element.
     */
    public void addFileset( final FileSet fileSet )
    {
        m_elements.add( fileSet );
    }

    /**
     * Append the contents of the other Path instance to this.
     */
    public void append( final Path other )
        throws TaskException
    {
        if( null == other )
        {
            throw new NullPointerException( "other" );
        }

        final String[] list = other.list();
        for( int i = 0; i < list.length; i++ )
        {
            final String file = list[ i ];
            if( m_elements.contains( file ) )
            {
                m_elements.add( file );
            }
        }
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
     * How many parts does this Path instance consist of.
     */
    public int size()
        throws TaskException
    {
        return list().length;
    }

    /**
     * Returns a textual representation of the path, which can be used as
     * CLASSPATH or PATH environment variable definition.
     *
     * @return a textual representation of the path.
     */
    public String toString()
    {
        try
        {
            final String[] list = list();

            // empty path return empty string
            if( list.length == 0 )
            {
                return "";
            }

            // path containing one or more elements
            final StringBuffer result = new StringBuffer( list[ 0 ].toString() );
            for( int i = 1; i < list.length; i++ )
            {
                result.append( File.pathSeparatorChar );
                result.append( list[ i ] );
            }

            return result.toString();
        }
        catch( final TaskException te )
        {
            throw new Error( te.toString() );
        }
    }
}
