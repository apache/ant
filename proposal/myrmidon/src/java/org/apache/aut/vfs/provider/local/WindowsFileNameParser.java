/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider.local;

import org.apache.aut.vfs.FileSystemException;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * A parser for Windows file names.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class WindowsFileNameParser
    extends LocalFileNameParser
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( WindowsFileNameParser.class );

    /**
     * Pops the root prefix off a URI, which has had the scheme removed.
     */
    protected String extractRootPrefix( final String uri,
                                        final StringBuffer name )
        throws FileSystemException
    {
        return extractWindowsRootPrefix( uri, name );
    }

    /**
     * Extracts a Windows root prefix from a name.
     */
    private String extractWindowsRootPrefix( final String uri,
                                             final StringBuffer name )
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
    private String extractDrivePrefix( final StringBuffer name )
        throws FileSystemException
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
    private String extractUNCPrefix( final String uri,
                                     final StringBuffer name )
        throws FileSystemException
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
