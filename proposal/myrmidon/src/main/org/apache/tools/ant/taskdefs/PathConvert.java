/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.File;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

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
public class PathConvert extends Task
{

    // Members
    private Path path = null;// Path to be converted
    private Reference refid = null;// Reference to path/fileset to convert
    private String targetOS = null;// The target OS type
    private boolean targetWindows = false;// Set when targetOS is set
    private boolean onWindows = false;// Set if we're running on windows
    private String property = null;// The property to receive the results
    private Vector prefixMap = new Vector();// Path prefix map
    private String pathSep = null;// User override on path sep char
    private String dirSep = null;

    /**
     * Override the default directory separator string for the target os
     *
     * @param sep The new DirSep value
     */
    public void setDirSep( String sep )
    {
        dirSep = sep;
    }

    /**
     * Override the default path separator string for the target os
     *
     * @param sep The new PathSep value
     */
    public void setPathSep( String sep )
    {
        pathSep = sep;
    }

    /**
     * Set the value of the proprty attribute - this is the property into which
     * our converted path will be placed.
     *
     * @param p The new Property value
     */
    public void setProperty( String p )
    {
        property = p;
    }

    /**
     * Adds a reference to a PATH or FILESET defined elsewhere.
     *
     * @param r The new Refid value
     */
    public void setRefid( Reference r )
    {
        if( path != null )
            throw noChildrenAllowed();

        refid = r;
    }

    /**
     * Set the value of the targetos attribute
     *
     * @param target The new Targetos value
     */
    public void setTargetos( String target )
    {

        targetOS = target.toLowerCase();

        if( !targetOS.equals( "windows" ) && !target.equals( "unix" ) &&
            !targetOS.equals( "netware" ) )
        {
            throw new BuildException( "targetos must be one of 'unix', 'netware', or 'windows'" );
        }

        // Currently, we deal with only two path formats: Unix and Windows
        // And Unix is everything that is not Windows

        // for NetWare, piggy-back on Windows, since in the validateSetup code,
        // the same assumptions can be made as with windows -
        // that ; is the path separator

        targetWindows = ( targetOS.equals( "windows" ) || targetOS.equals( "netware" ) );
    }

    /**
     * Has the refid attribute of this element been set?
     *
     * @return The Reference value
     */
    public boolean isReference()
    {
        return refid != null;
    }

    /**
     * Create a nested MAP element
     *
     * @return Description of the Returned Value
     */
    public MapEntry createMap()
    {

        MapEntry entry = new MapEntry();
        prefixMap.addElement( entry );
        return entry;
    }

    /**
     * Create a nested PATH element
     *
     * @return Description of the Returned Value
     */
    public Path createPath()
    {

        if( isReference() )
            throw noChildrenAllowed();

        if( path == null )
        {
            path = new Path( getProject() );
        }
        return path.createPath();
    }

    /**
     * Do the execution.
     *
     * @exception BuildException Description of Exception
     */
    public void execute()
        throws BuildException
    {

        // If we are a reference, the create a Path from the reference
        if( isReference() )
        {
            path = new Path( getProject() ).createPath();

            Object obj = refid.getReferencedObject( getProject() );

            if( obj instanceof Path )
            {
                path.setRefid( refid );
            }
            else if( obj instanceof FileSet )
            {
                FileSet fs = ( FileSet )obj;
                path.addFileset( fs );
            }
            else
            {
                throw new BuildException( "'refid' does not refer to a path or fileset" );
            }
        }

        validateSetup();// validate our setup

        // Currently, we deal with only two path formats: Unix and Windows
        // And Unix is everything that is not Windows
        // (with the exception for NetWare below)

        String osname = System.getProperty( "os.name" ).toLowerCase();

        // for NetWare, piggy-back on Windows, since here and in the
        // apply code, the same assumptions can be made as with windows -
        // that \\ is an OK separator, and do comparisons case-insensitive.
        onWindows = ( ( osname.indexOf( "windows" ) >= 0 ) ||
            ( osname.indexOf( "netware" ) >= 0 ) );

        // Determine the from/to char mappings for dir sep
        char fromDirSep = onWindows ? '\\' : '/';
        char toDirSep = dirSep.charAt( 0 );

        StringBuffer rslt = new StringBuffer( 100 );

        // Get the list of path components in canonical form
        String[] elems = path.list();

        for( int i = 0; i < elems.length; i++ )
        {
            String elem = elems[i];

            elem = mapElement( elem );// Apply the path prefix map

            // Now convert the path and file separator characters from the
            // current os to the target os.

            elem = elem.replace( fromDirSep, toDirSep );

            if( i != 0 )
                rslt.append( pathSep );
            rslt.append( elem );
        }

        // Place the result into the specified property
        String value = rslt.toString();

        log( "Set property " + property + " = " + value, Project.MSG_VERBOSE );

        getProject().setNewProperty( property, value );
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
    {

        int size = prefixMap.size();

        if( size != 0 )
        {

            // Iterate over the map entries and apply each one.  Stop when one of the
            // entries actually changes the element

            for( int i = 0; i < size; i++ )
            {
                MapEntry entry = ( MapEntry )prefixMap.elementAt( i );
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
     * Creates an exception that indicates that this XML element must not have
     * child elements if the refid attribute is set.
     *
     * @return Description of the Returned Value
     */
    private BuildException noChildrenAllowed()
    {
        return new BuildException( "You must not specify nested PATH elements when using refid" );
    }

    /**
     * Validate that all our parameters have been properly initialized.
     *
     * @throws BuildException if something is not setup properly
     */
    private void validateSetup()
        throws BuildException
    {

        if( path == null )
            throw new BuildException( "You must specify a path to convert" );

        if( property == null )
            throw new BuildException( "You must specify a property" );

        // Must either have a target OS or both a dirSep and pathSep

        if( targetOS == null && pathSep == null && dirSep == null )
            throw new BuildException( "You must specify at least one of targetOS, dirSep, or pathSep" );

        // Determine the separator strings.  The dirsep and pathsep attributes
        // override the targetOS settings.
        String dsep = File.separator;
        String psep = File.pathSeparator;

        if( targetOS != null )
        {
            psep = targetWindows ? ";" : ":";
            dsep = targetWindows ? "\\" : "/";
        }

        if( pathSep != null )
        {// override with pathsep=
            psep = pathSep;
        }

        if( dirSep != null )
        {// override with dirsep=
            dsep = dirSep;
        }

        pathSep = psep;
        dirSep = dsep;
    }

    /**
     * Helper class, holds the nested <map> values. Elements will look like
     * this: &lt;map from="d:" to="/foo"/> <p>
     *
     * When running on windows, the prefix comparison will be case insensitive.
     *
     * @author RT
     */
    public class MapEntry
    {

        // Members
        private String from = null;
        private String to = null;

        /**
         * Set the "from" attribute of the map entry
         *
         * @param from The new From value
         */
        public void setFrom( String from )
        {
            this.from = from;
        }

        /**
         * Set the "to" attribute of the map entry
         *
         * @param to The new To value
         */
        public void setTo( String to )
        {
            this.to = to;
        }

        /**
         * Apply this map entry to a given path element
         *
         * @param elem Path element to process
         * @return String Updated path element after mapping
         */
        public String apply( String elem )
        {
            if( from == null || to == null )
            {
                throw new BuildException( "Both 'from' and 'to' must be set in a map entry" );
            }

            // If we're on windows, then do the comparison ignoring case
            String cmpElem = onWindows ? elem.toLowerCase() : elem;
            String cmpFrom = onWindows ? from.toLowerCase() : from;

            // If the element starts with the configured prefix, then convert the prefix
            // to the configured 'to' value.

            if( cmpElem.startsWith( cmpFrom ) )
            {
                int len = from.length();

                if( len >= elem.length() )
                {
                    elem = to;
                }
                else
                {
                    elem = to + elem.substring( len );
                }
            }

            return elem;
        }
    }// User override on directory sep char
}
