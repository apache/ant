/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs;

import java.io.File;
import java.util.ArrayList;
import org.apache.aut.nativelib.Os;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.file.Path;

/**
 * This task converts path and classpath information to a specific target OS
 * format. The resulting formatted path is placed into a specified property. <p>
 *
 * LIMITATION: Currently this implementation groups all machines into one of two
 * types: Unix or Windows. Unix is defined as NOT windows.
 *
 * @author Larry Streepy <a href="mailto:streepy@healthlanguage.com">
 *      streepy@healthlanguage.com</a>
 */
public class PathConvert extends AbstractTask
{
    private Path m_path;// Path to be converted
    private String m_targetOS;// The target OS type
    private boolean m_targetWindows;// Set when targetOS is set
    private boolean m_onWindows;// Set if we're running on windows
    private String m_property;// The property to receive the results
    private ArrayList m_prefixMap = new ArrayList();// Path prefix map
    private String m_pathSep;// User override on path sep char
    private String m_dirSep;

    /**
     * Override the default directory separator string for the target os
     */
    public void setDirSep( final String dirSep )
    {
        m_dirSep = dirSep;
    }

    /**
     * Override the default path separator string for the target os
     *
     * @param pathSep The new PathSep value
     */
    public void setPathSep( final String pathSep )
    {
        m_pathSep = pathSep;
    }

    /**
     * Set the value of the proprty attribute - this is the property into which
     * our converted path will be placed.
     */
    public void setProperty( final String property )
    {
        m_property = property;
    }

    /**
     * Set the value of the targetos attribute
     *
     * @param targetOS The new Targetos value
     */
    public void setTargetos( String targetOS )
        throws TaskException
    {
        m_targetOS = targetOS.toLowerCase();

        if( !m_targetOS.equals( "windows" ) && !targetOS.equals( "unix" ) &&
            !m_targetOS.equals( "netware" ) )
        {
            throw new TaskException( "targetos must be one of 'unix', 'netware', or 'windows'" );
        }

        // Currently, we deal with only two path formats: Unix and Windows
        // And Unix is everything that is not Windows

        // for NetWare, piggy-back on Windows, since in the validateSetup code,
        // the same assumptions can be made as with windows -
        // that ; is the path separator

        m_targetWindows = ( m_targetOS.equals( "windows" ) || m_targetOS.equals( "netware" ) );
    }

    /**
     * Create a nested MAP element
     */
    public void addMap( final MapEntry entry )
    {
        m_prefixMap.add( entry );
    }

    /**
     * Adds a PATH element
     */
    public void addPath( Path path )
        throws TaskException
    {
        if( m_path == null )
        {
            m_path = path;
        }
        else
        {
            m_path.addPath( path );
        }
    }

    public void execute()
        throws TaskException
    {
        // If we are a reference, the create a Path from the reference
        validate();// validate our setup

        // Currently, we deal with only two path formats: Unix and Windows
        // And Unix is everything that is not Windows
        // (with the exception for NetWare below)

        // for NetWare, piggy-back on Windows, since here and in the
        // apply code, the same assumptions can be made as with windows -
        // that \\ is an OK separator, and do comparisons case-insensitive.
        m_onWindows = ( Os.isFamily( Os.OS_FAMILY_WINDOWS ) || Os.isFamily( Os.OS_FAMILY_NETWARE ) );

        // Determine the from/to char mappings for dir sep
        char fromDirSep = m_onWindows ? '\\' : '/';
        char toDirSep = m_dirSep.charAt( 0 );

        StringBuffer rslt = new StringBuffer( 100 );

        // Get the list of path components in canonical form
        String[] elems = m_path.listFiles( getContext() );

        for( int i = 0; i < elems.length; i++ )
        {
            String elem = elems[ i ];

            elem = mapElement( elem );// Apply the path prefix map

            // Now convert the path and file separator characters from the
            // current os to the target os.

            elem = elem.replace( fromDirSep, toDirSep );

            if( i != 0 )
            {
                rslt.append( m_pathSep );
            }
            rslt.append( elem );
        }

        // Place the result into the specified property
        final String value = rslt.toString();

        getContext().debug( "Set property " + m_property + " = " + value );

        final String name = m_property;
        getContext().setProperty( name, value );
    }

    /**
     * Apply the configured map to a path element. The map is used to convert
     * between Windows drive letters and Unix paths. If no map is configured,
     * then the input string is returned unchanged.
     *
     * @param elem The path element to apply the map to
     * @return String Updated element
     */
    private String mapElement( String elem )
        throws TaskException
    {
        int size = m_prefixMap.size();

        if( size != 0 )
        {

            // Iterate over the map entries and apply each one.  Stop when one of the
            // entries actually changes the element

            for( int i = 0; i < size; i++ )
            {
                MapEntry entry = (MapEntry)m_prefixMap.get( i );
                String newElem = entry.apply( elem );

                // Note I'm using "!=" to see if we got a new object back from
                // the apply method.

                if( newElem != elem )
                {
                    elem = newElem;
                    break;// We applied one, so we're done
                }
            }
        }

        return elem;
    }

    /**
     * Validate that all our parameters have been properly initialized.
     *
     * @throws org.apache.myrmidon.api.TaskException if something is not setup properly
     */
    private void validate()
        throws TaskException
    {

        if( m_path == null )
        {
            throw new TaskException( "You must specify a path to convert" );
        }

        if( m_property == null )
        {
            throw new TaskException( "You must specify a property" );
        }

        // Must either have a target OS or both a dirSep and pathSep

        if( m_targetOS == null && m_pathSep == null && m_dirSep == null )
        {
            throw new TaskException( "You must specify at least one of targetOS, dirSep, or pathSep" );
        }

        // Determine the separator strings.  The dirsep and pathsep attributes
        // override the targetOS settings.
        String dsep = File.separator;
        String psep = File.pathSeparator;

        if( m_targetOS != null )
        {
            psep = m_targetWindows ? ";" : ":";
            dsep = m_targetWindows ? "\\" : "/";
        }

        if( m_pathSep != null )
        {// override with pathsep=
            psep = m_pathSep;
        }

        if( m_dirSep != null )
        {// override with dirsep=
            dsep = m_dirSep;
        }

        m_pathSep = psep;
        m_dirSep = dsep;
    }

    /**
     * Helper class, holds the nested <map> values. Elements will look like
     * this: &lt;map from="d:" to="/foo"/> <p>
     *
     * When running on windows, the prefix comparison will be case insensitive.
     */
    public class MapEntry
    {
        private String m_from;
        private String m_to;

        /**
         * Set the "from" attribute of the map entry
         *
         * @param from The new From value
         */
        public void setFrom( final String from )
        {
            m_from = from;
        }

        /**
         * Set the "to" attribute of the map entry
         *
         * @param to The new To value
         */
        public void setTo( final String to )
        {
            m_to = to;
        }

        /**
         * Apply this map entry to a given path element
         *
         * @param elem Path element to process
         * @return String Updated path element after mapping
         */
        public String apply( String elem )
            throws TaskException
        {
            if( m_from == null || m_to == null )
            {
                throw new TaskException( "Both 'from' and 'to' must be set in a map entry" );
            }

            // If we're on windows, then do the comparison ignoring case
            final String cmpElem = m_onWindows ? elem.toLowerCase() : elem;
            final String cmpFrom = m_onWindows ? m_from.toLowerCase() : m_from;

            // If the element starts with the configured prefix, then convert the prefix
            // to the configured 'to' value.

            if( cmpElem.startsWith( cmpFrom ) )
            {
                final int len = m_from.length();
                if( len >= elem.length() )
                {
                    elem = m_to;
                }
                else
                {
                    elem = m_to + elem.substring( len );
                }
            }

            return elem;
        }
    }
}
