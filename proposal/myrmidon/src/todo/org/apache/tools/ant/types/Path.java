/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.PathTokenizer;
import org.apache.tools.ant.ProjectComponent;

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
    extends ProjectComponent
    implements Cloneable
{
    public final static Path systemClasspath = createSystemClasspath();

    private ArrayList elements;

    private static Path createSystemClasspath()
    {
        try
        {
            return new Path( System.getProperty( "java.class.path" ) );
        }
        catch( final TaskException te )
        {
            throw new Error( te.toString() );
        }
    }

    /**
     * Invoked by IntrospectionHelper for <code>setXXX(Path p)</code> attribute
     * setters.
     */
    public Path( final String path )
        throws TaskException
    {
        this();
        createPathElement().setPath( path );
    }

    public Path()
    {
        elements = new ArrayList();
    }

    /**
     * Returns its argument with all file separator characters replaced so that
     * they match the local OS conventions.
     */
    private static String translateFile( final String source )
    {
        if( source == null )
            return "";

        final StringBuffer result = new StringBuffer( source );
        for( int i = 0; i < result.length(); i++ )
        {
            translateFileSep( result, i );
        }

        return result.toString();
    }

    /**
     * Splits a PATH (with : or ; as separators) into its parts.
     */
    private String[] translatePath( final File baseDirectory, String source )
    {
        final ArrayList result = new ArrayList();
        if( source == null )
            return new String[ 0 ];

        PathTokenizer tok = new PathTokenizer( source );
        StringBuffer element = new StringBuffer();
        while( tok.hasMoreTokens() )
        {
            element.setLength( 0 );
            final String pathElement = tok.nextToken();
            try
            {
                element.append( resolveFile( baseDirectory, pathElement ) );
            }
            catch( TaskException e )
            {
                final String message =
                    "Dropping path element " + pathElement + " as it is not valid relative to the project";
                getLogger().debug( message );
            }

            for( int i = 0; i < element.length(); i++ )
            {
                translateFileSep( element, i );
            }
            result.add( element.toString() );
        }

        return (String[])result.toArray( new String[ result.size() ] );
    }

    /**
     * Translates all occurrences of / or \ to correct separator of the current
     * platform and returns whether it had to do any replacements.
     *
     * @param buffer Description of Parameter
     * @param pos Description of Parameter
     * @return Description of the Returned Value
     */
    private static boolean translateFileSep( StringBuffer buffer, int pos )
    {
        if( buffer.charAt( pos ) == '/' || buffer.charAt( pos ) == '\\' )
        {
            buffer.setCharAt( pos, File.separatorChar );
            return true;
        }
        return false;
    }

    /**
     * Adds a String to the ArrayList if it isn't already included.
     */
    private static void addUnlessPresent( final ArrayList list, final String entry )
    {
        if( !list.contains( entry ) )
        {
            list.add( entry );
        }
    }

    /**
     * Resolve a filename with Project's help - if we know one that is. <p>
     *
     * Assume the filename is absolute if project is null.</p>
     */
    private static String resolveFile( final File baseDirectory, final String relativeName )
        throws TaskException
    {
        if( null != baseDirectory )
        {
            final File file = FileUtil.resolveFile( baseDirectory, relativeName );
            return file.getAbsolutePath();
        }
        return relativeName;
    }

    /**
     * Adds a element definition to the path.
     *
     * @param location the location of the element to add (must not be <code>null</code>
     *      nor empty.
     */
    public void setLocation( final File location )
    {
        createPathElement().setLocation( location );
    }

    /**
     * Parses a path definition and creates single PathElements.
     *
     * @param path the path definition.
     */
    public void setPath( String path )
    {
        createPathElement().setPath( path );
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
                setLocation( file );
            }
        }
    }

    /**
     * Emulation of extdirs feature in java >= 1.2. This method adds all files
     * in the given directories (but not in sub-directories!) to the classpath,
     * so that you don't have to specify them all one by one.
     */
    public void addExtdirs( Path extdirs )
        throws TaskException
    {
        if( extdirs == null )
        {
            String extProp = System.getProperty( "java.ext.dirs" );
            if( extProp != null )
            {
                extdirs = new Path( extProp );
            }
            else
            {
                return;
            }
        }

        final String[] dirs = extdirs.list();
        for( int i = 0; i < dirs.length; i++ )
        {
            final File dir = resolveFile( dirs[ i ] );
            if( dir.exists() && dir.isDirectory() )
            {
                final FileSet fileSet = new FileSet();
                fileSet.setDir( dir );
                fileSet.setIncludes( "*" );
                addFileset( fileSet );
            }
        }
    }

    /**
     * Adds a nested <code>&lt;fileset&gt;</code> element.
     *
     * @param fs The feature to be added to the Fileset attribute
     * @exception TaskException Description of Exception
     */
    public void addFileset( final FileSet fileSet )
    {
        elements.add( fileSet );
    }

    /**
     * Add the Java Runtime classes to this Path instance.
     */
    public void addJavaRuntime()
        throws TaskException
    {
        if( System.getProperty( "java.vendor" ).toLowerCase( Locale.US ).indexOf( "microsoft" ) >= 0 )
        {
            // Pull in *.zip from packages directory
            FileSet msZipFiles = new FileSet();
            msZipFiles.setDir( new File( System.getProperty( "java.home" ) + File.separator + "Packages" ) );
            msZipFiles.setIncludes( "*.ZIP" );
            addFileset( msZipFiles );
        }
        else if( "Kaffe".equals( System.getProperty( "java.vm.name" ) ) )
        {
            FileSet kaffeJarFiles = new FileSet();
            kaffeJarFiles.setDir( new File( System.getProperty( "java.home" )
                                            + File.separator + "share"
                                            + File.separator + "kaffe" ) );

            kaffeJarFiles.setIncludes( "*.jar" );
            addFileset( kaffeJarFiles );
        }
        else
        {
            // JDK > 1.1 seems to set java.home to the JRE directory.
            final String rt = System.getProperty( "java.home" ) +
                File.separator + "lib" + File.separator + "rt.jar";
            addExisting( new Path( rt ) );
            // Just keep the old version as well and let addExisting
            // sort it out.
            final String rt2 = System.getProperty( "java.home" ) +
                File.separator + "jre" + File.separator + "lib" +
                File.separator + "rt.jar";
            addExisting( new Path( rt2 ) );

            // Added for MacOS X
            final String classes = System.getProperty( "java.home" ) +
                File.separator + ".." + File.separator + "Classes" +
                File.separator + "classes.jar";
            addExisting( new Path( classes ) );
            final String ui = System.getProperty( "java.home" ) +
                File.separator + ".." + File.separator + "Classes" +
                File.separator + "ui.jar";
            addExisting( new Path( ui ) );
        }
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
            if( elements.contains( file ) )
            {
                elements.add( file );
            }
        }
    }

    /**
     * Concatenates the system class path in the order specified by the
     * ${build.sysclasspath} property - using &quot;last&quot; as default value.
     *
     * @return Description of the Returned Value
     */
    public Path concatSystemClasspath()
        throws TaskException
    {
        return concatSystemClasspath( "last" );
    }

    /**
     * Concatenates the system class path in the order specified by the
     * ${build.sysclasspath} property - using the supplied value if
     * ${build.sysclasspath} has not been set.
     *
     * @param defValue Description of Parameter
     * @return Description of the Returned Value
     */
    public Path concatSystemClasspath( String defValue )
        throws TaskException
    {
        Path result = new Path();

        String order = defValue;
        if( getProject() != null )
        {
            String o = getProject().getProperty( "build.sysclasspath" );
            if( o != null )
            {
                order = o;
            }
        }

        if( order.equals( "only" ) )
        {
            // only: the developer knows what (s)he is doing
            result.addExisting( Path.systemClasspath );
        }
        else if( order.equals( "first" ) )
        {
            // first: developer could use a little help
            result.addExisting( Path.systemClasspath );
            result.addExisting( this );
        }
        else if( order.equals( "ignore" ) )
        {
            // ignore: don't trust anyone
            result.addExisting( this );
        }
        else
        {
            // last: don't trust the developer
            if( !order.equals( "last" ) )
            {
                final String message = "invalid value for build.sysclasspath: " + order;
                getLogger().warn( message );
            }

            result.addExisting( this );
            result.addExisting( Path.systemClasspath );
        }

        return result;
    }

    /**
     * Creates a nested <code>&lt;path&gt;</code> element.
     *
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    public Path createPath()
    {
        final Path other = new Path();
        elements.add( other );
        return other;
    }

    /**
     * Creates the nested <code>&lt;pathelement&gt;</code> element.
     */
    public PathElement createPathElement()
    {
        final PathElement pathElement = new PathElement();
        elements.add( pathElement );
        return pathElement;
    }

    /**
     * Returns all path elements defined by this and nested path objects.
     */
    public String[] list()
        throws TaskException
    {
        ArrayList result = new ArrayList( 2 * elements.size() );
        for( int i = 0; i < elements.size(); i++ )
        {
            Object o = elements.get( i );
            if( o instanceof String )
            {
                // obtained via append
                addUnlessPresent( result, (String)o );
            }
            else if( o instanceof PathElement )
            {
                String[] parts = ( (PathElement)o ).getParts();
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
                final DirectoryScanner ds = fs.getDirectoryScanner();
                final String[] s = ds.getIncludedFiles();
                final File dir = fs.getDir();
                for( int j = 0; j < s.length; j++ )
                {
                    File f = new File( dir, s[ j ] );
                    String absolutePath = f.getAbsolutePath();
                    addUnlessPresent( result, translateFile( absolutePath ) );
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
                return "";

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

    /**
     * Returns an array of URLs - useful for building a ClassLoader.
     */
    public URL[] toURLs()
        throws TaskException
    {
        try
        {
            final String[] list = list();

            final URL[] result = new URL[ list.length ];

            // path containing one or more elements
            for( int i = 0; i < list.length; i++ )
            {
                result[ i ] = new File( list[ i ] ).toURL();
            }

            return result;
        }
        catch( final IOException ioe )
        {
            final String message = "Malformed path entry. Reason:" + ioe;
            throw new TaskException( message, ioe );
        }
    }

    /**
     * Helper class, holds the nested <code>&lt;pathelement&gt;</code> values.
     */
    public class PathElement
    {
        private String[] parts;

        public void setLocation( File loc )
        {
            parts = new String[]{translateFile( loc.getAbsolutePath() )};
        }

        public void setPath( String path )
        {
            parts = translatePath( getProject().getBaseDir(), path );
        }

        public String[] getParts()
        {
            return parts;
        }
    }
}
