/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.local;

import java.io.File;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.provider.ParsedUri;
import org.apache.aut.vfs.provider.UriParser;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.i18n.ResourceManager;

/**
 * A name parser.
 *
 * @author Adam Murdoch
 */
class LocalFileNameParser extends UriParser
{
    private static final Resources REZ
        = ResourceManager.getPackageResources( LocalFileNameParser.class );

    private boolean m_windowsNames;

    public LocalFileNameParser()
    {
        super( new char[]{File.separatorChar, '/', '\\'} );
        m_windowsNames = ( System.getProperty( "os.name" ).toLowerCase().indexOf( "windows" ) != -1 );
    }

    /**
     * Determines if a name is an absolute file name.
     */
    public boolean isAbsoluteName( String name )
    {
        // TODO - this is yucky
        StringBuffer b = new StringBuffer( name );
        try
        {
            fixSeparators( b );
            extractRootPrefix( name, b );
            return true;
        }
        catch( FileSystemException e )
        {
            return false;
        }
    }

    /**
     * Parses an absolute URI, splitting it into its components.
     *
     * @param name
     *          The URI.
     */
    public ParsedUri parseUri( String uriStr ) throws FileSystemException
    {
        StringBuffer name = new StringBuffer();
        ParsedFileUri uri = new ParsedFileUri();

        // Extract the scheme
        String scheme = extractScheme( uriStr, name );
        uri.setScheme( scheme );

        // Adjust the separators
        fixSeparators( name );

        // Extract the root prefix
        String rootFile = extractRootPrefix( uriStr, name );
        uri.setRootFile( rootFile );

        // Normalise the path
        normalisePath( name );
        uri.setPath( name.toString() );

        // Build the root URI
        StringBuffer rootUri = new StringBuffer();
        rootUri.append( scheme );
        rootUri.append( "://" );
        rootUri.append( rootFile );
        uri.setRootURI( rootUri.toString() );

        return uri;
    }

    /**
     * Pops the root prefix off a URI, which has had the scheme removed.
     */
    private String extractRootPrefix( String uri,
                                      StringBuffer name )
        throws FileSystemException
    {
        // TODO - split this into sub-classes
        if( m_windowsNames )
        {
            return extractWindowsRootPrefix( uri, name );
        }
        else
        {
            // Looking for <sep>
            if( name.length() == 0 || name.charAt( 0 ) != '/' )
            {
                final String message = REZ.getString( "not-absolute-file-name.error", uri );
                throw new FileSystemException( message );
            }

            // TODO - this isn't always true
            return "/";
        }
    }

    /**
     * Extracts a Windows root prefix from a name.
     */
    private String extractWindowsRootPrefix( String uri,
                                             StringBuffer name )
        throws FileSystemException
    {
        // Looking for:
        // ('/'){0, 3} <letter> ':' '/'
        // ['/'] '//' <name> '/' <name> ( '/' | <end> )

        // Skip over first 3 leading '/' chars
        int startPos = 0;
        int maxlen = Math.min( 3, name.length() );
        for( ; startPos < maxlen && name.charAt( startPos ) == '/'; startPos++ )
        {
        }
        if( startPos == maxlen )
        {
            // Too many '/'
            final String message = REZ.getString( "not-absolute-file-name.error", uri );
            throw new FileSystemException( message );
        }
        name.delete( 0, startPos );

        // Look for drive name
        String driveName = extractDrivePrefix( name );
        if( driveName != null )
        {
            return driveName;
        }

        // Look for UNC name
        if( startPos < 2 )
        {
            final String message = REZ.getString( "not-absolute-file-name.error", uri );
            throw new FileSystemException( message );
        }

        return "//" + extractUNCPrefix( uri, name );
    }

    /**
     * Extracts a drive prefix from a path.  Leading '/' chars have been removed.
     */
    private String extractDrivePrefix( StringBuffer name ) throws FileSystemException
    {
        // Looking for <letter> ':' '/'
        if( name.length() < 3 )
        {
            // Too short
            return null;
        }
        char ch = name.charAt( 0 );
        if( ch == '/' || ch == ':' )
        {
            // Missing drive letter
            return null;
        }
        if( name.charAt( 1 ) != ':' )
        {
            // Missing ':'
            return null;
        }
        if( name.charAt( 2 ) != '/' )
        {
            // Missing separator
            return null;
        }

        String prefix = name.substring( 0, 2 );
        name.delete( 0, 2 );
        return prefix;
    }

    /**
     * Extracts a UNC name from a path.  Leading '/' chars have been removed.
     */
    private String extractUNCPrefix( String uri,
                                     StringBuffer name ) throws FileSystemException
    {
        // Looking for <name> '/' <name> ( '/' | <end> )

        // Look for first separator
        int maxpos = name.length();
        int pos = 0;
        for( ; pos < maxpos && name.charAt( pos ) != '/'; pos++ )
        {
        }
        pos++;
        if( pos >= maxpos )
        {
            final String message = REZ.getString( "missing-share-name.error", uri );
            throw new FileSystemException( message );
        }

        // Now have <name> '/'
        int startShareName = pos;
        for( ; pos < maxpos && name.charAt( pos ) != '/'; pos++ )
        {
        }
        if( pos == startShareName )
        {
            final String message = REZ.getString( "missing-share-name.error", uri );
            throw new FileSystemException( message );
        }

        // Now have <name> '/' <name> ( '/' | <end> )
        String prefix = name.substring( 0, pos );
        name.delete( 0, pos );
        return prefix;
    }
}
