/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider;

import java.util.HashSet;
import java.util.Iterator;
import org.apache.aut.vfs.FileSystemException;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * A name parser which parses absolute URIs.  See RFC 2396 for details.
 *
 * @author Adam Murdoch
 */
public class UriParser
{
    private static final Resources REZ
        = ResourceManager.getPackageResources( UriParser.class );

    /** The normalised separator to use. */
    private char m_separatorChar;
    private String m_separator;

    /**
     * The set of valid separators.  These are all converted to the normalised one.
     * Does <i>not</i> contain the normalised separator
     */
    private char[] m_separators;

    /**
     * Creates a parser, using '/' and '\' as the path separators.
     */
    public UriParser()
    {
        this( null );
    }

    /**
     * Creates a parser, using '/' and '\' as the path separators, along with
     * a provider-specific set of separators.
     *
     * @param separators
     *          Additional legal separator characters.  Any occurrences of
     *          these in paths are replaced with the separator char.
     */
    protected UriParser( char[] separators )
    {
        m_separatorChar = '/';

        // Remove the separator char from the separators array
        HashSet set = new HashSet();
        set.add( new Character( '\\' ) );
        if( separators != null )
        {
            for( int i = 0; i < separators.length; i++ )
            {
                char separator = separators[ i ];
                if( separator == m_separatorChar )
                {
                    continue;
                }
                set.add( new Character( separator ) );
            }
        }
        m_separators = new char[ set.size() ];
        Iterator iter = set.iterator();
        for( int i = 0; i < m_separators.length; i++ )
        {
            Character ch = (Character)iter.next();
            m_separators[ i ] = ch.charValue();
        }

        m_separator = String.valueOf( m_separatorChar );
    }

    /**
     * Parses an absolute URI, splitting it into its components.  This
     * implementation assumes a "generic URI", as defined by RFC 2396.  See
     * {@link #parseGenericUri} for more info.
     *
     * <p>Sub-classes should override this method.
     */
    public ParsedUri parseUri( String uriStr ) throws FileSystemException
    {
        ParsedUri retval = new ParsedUri();
        parseGenericUri( uriStr, retval );
        return retval;
    }

    /**
     * Parses a generic URI, as defined by RFC 2396.  Briefly, a generic URI
     * looks like:
     *
     * <pre>
     * &lt;scheme> '://' [ &lt;userinfo> '@' ] &lt;hostname> [ ':' &lt;port> ] '/' &lt;path>
     * </pre>
     *
     * <p>This method differs from the RFC, in that either / or \ is allowed
     * as a path separator.
     *
     * @param uriStr
     *          The URI to parse.
     * @param uri
     *          Used to return the parsed components of the URI.
     */
    protected void parseGenericUri( String uriStr, ParsedUri uri ) throws FileSystemException
    {
        StringBuffer name = new StringBuffer();

        // Extract the scheme and authority parts
        extractToPath( uriStr, name, uri );

        // Normalise the file name
        normalisePath( name );
        uri.setPath( name.toString() );

        // Build the root uri
        StringBuffer rootUri = new StringBuffer();
        rootUri.append( uri.getScheme() );
        rootUri.append( "://" );
        rootUri.append( uri.getHostName() );
        uri.setRootURI( rootUri.toString() );
    }

    /**
     * Extracts the scheme, userinfo, hostname and port components of an
     * absolute "generic URI".
     *
     * @param uri
     *          The absolute URI to parse.
     *
     * @param name
     *          Used to return the remainder of the URI.
     *
     * @parsedUri
     *          Used to return the extracted components.
     */
    protected void extractToPath( String uri, StringBuffer name, ParsedUri parsedUri )
        throws FileSystemException
    {
        // Extract the scheme
        String scheme = extractScheme( uri, name );
        parsedUri.setScheme( scheme );

        // Expecting "//"
        if( name.length() < 2 || name.charAt( 0 ) != '/' || name.charAt( 1 ) != '/' )
        {
            final String message = REZ.getString( "missing-double-slashes.error", uri );
            throw new FileSystemException( message );
        }
        name.delete( 0, 2 );

        // Extract userinfo
        String userInfo = extractUserInfo( name );
        parsedUri.setUserInfo( userInfo );

        // Extract hostname
        String hostName = extractHostName( name );
        if( hostName == null )
        {
            final String message = REZ.getString( "missing-hostname.error", uri );
            throw new FileSystemException( message );
        }
        parsedUri.setHostName( hostName );

        // Extract port
        String port = extractPort( name );
        if( port != null && port.length() == 0 )
        {
            final String message = REZ.getString( "missing-port.error", uri );
            throw new FileSystemException( message );
        }
        parsedUri.setPort( port );

        // Expecting '/' or empty name
        if( name.length() > 0 && name.charAt( 0 ) != '/' )
        {
            final String message = REZ.getString( "missing-hostname-path-sep.error", uri );
            throw new FileSystemException( message );
        }
    }

    /**
     * Extracts the user info from a URI.  The <scheme>:// part has been removed
     * already.
     */
    protected String extractUserInfo( StringBuffer name )
    {
        int maxlen = name.length();
        for( int pos = 0; pos < maxlen; pos++ )
        {
            char ch = name.charAt( pos );
            if( ch == '@' )
            {
                // Found the end of the user info
                String userInfo = name.substring( 0, pos );
                name.delete( 0, pos + 1 );
                return userInfo;
            }
            if( ch == '/' || ch == '?' )
            {
                // Not allowed in user info
                break;
            }
        }

        // Not found
        return null;
    }

    /**
     * Extracts the hostname from a URI.  The <scheme>://<userinfo>@ part has
     * been removed.
     */
    protected String extractHostName( StringBuffer name )
    {
        int maxlen = name.length();
        int pos = 0;
        for( ; pos < maxlen; pos++ )
        {
            char ch = name.charAt( pos );
            if( ch == '/' || ch == ';' || ch == '?' || ch == ':'
                || ch == '@' || ch == '&' || ch == '=' || ch == '+'
                || ch == '$' || ch == ',' )
            {
                break;
            }
        }
        if( pos == 0 )
        {
            return null;
        }

        String hostname = name.substring( 0, pos );
        name.delete( 0, pos );
        return hostname;
    }

    /**
     * Extracts the port from a URI.  The <scheme>://<userinfo>@<hostname>
     * part has been removed.
     */
    protected String extractPort( StringBuffer name )
    {
        if( name.length() < 1 || name.charAt( 0 ) != ':' )
        {
            return null;
        }
        int maxlen = name.length();
        int pos = 1;
        for( ; pos < maxlen; pos++ )
        {
            char ch = name.charAt( pos );
            if( ch < '0' || ch > '9' )
            {
                break;
            }
        }
        String port = name.substring( 1, pos );
        name.delete( 0, pos );
        return port;
    }

    /**
     * Extracts the first element of a path.
     */
    protected String extractFirstElement( StringBuffer name )
    {
        int len = name.length();
        if( len < 1 )
        {
            return null;
        }
        int startPos = 0;
        if( name.charAt( 0 ) == m_separatorChar )
        {
            startPos = 1;
        }
        for( int pos = startPos; pos < len; pos++ )
        {
            if( name.charAt( pos ) == m_separatorChar )
            {
                // Found a separator
                String elem = name.substring( startPos, pos );
                name.delete( startPos, pos + 1 );
                return elem;
            }
        }

        // No separator
        String elem = name.substring( startPos );
        name.setLength( 0 );
        return elem;
    }

    /**
     * Builds a URI from a root URI and path.
     *
     * @param rootUri
     *          The root URI.
     *
     * @param path
     *          A <i>normalised</i> path.
     */
    public String getUri( String rootUri, String path )
    {
        StringBuffer uri = new StringBuffer( rootUri );
        int len = uri.length();
        if( uri.charAt( len - 1 ) == m_separatorChar )
        {
            uri.delete( len - 1, len );
        }
        if( !path.startsWith( m_separator ) )
        {
            uri.append( m_separatorChar );
        }
        uri.append( path );
        return uri.toString();
    }

    /**
     * Returns the base name of a path.
     *
     * @param path
     *          A <i>normalised</i> path.
     */
    public String getBaseName( String path )
    {
        int idx = path.lastIndexOf( m_separatorChar );
        if( idx == -1 )
        {
            return path;
        }
        return path.substring( idx + 1 );
    }

    /**
     * Resolves a path, relative to a base path.  If the supplied path
     * is an absolute path, it is normalised and returned.  If the supplied
     * path is a relative path, it is resolved relative to the base path.
     *
     * @param basePath
     *          A <i>normalised</i> path.
     *
     * @param path
     *          The path to resolve.  Does not need to be normalised, but
     *          does need to be a path (i.e. not an absolute URI).
     *
     */
    public String resolvePath( String basePath, String path ) throws FileSystemException
    {
        StringBuffer buffer = new StringBuffer( path );

        // Adjust separators
        fixSeparators( buffer );

        // Determine whether to prepend the base path
        if( path.length() == 0 || path.charAt( 0 ) != m_separatorChar )
        {
            // Supplied path is not absolute
            buffer.insert( 0, m_separatorChar );
            buffer.insert( 0, basePath );
        }

        // Normalise the path
        normalisePath( buffer );
        return buffer.toString();
    }

    /**
     * Returns a child path.
     *
     * @param parent
     *          A <i>normalised</i> path.
     *
     * @param name
     *          The child name.  Must be a valid element name (i.e. no separators, etc).
     */
    public String getChildPath( String parent, String name ) throws FileSystemException
    {
        // Validate the child name
        if( name.length() == 0
            || name.equals( "." )
            || name.equals( ".." ) )
        {
            final String message = REZ.getString( "invalid-childname.error", name );
            throw new FileSystemException( message );
        }

        // Check for separators
        if( name.indexOf( m_separatorChar ) != -1 )
        {
            final String message = REZ.getString( "invalid-childname.error", name );
            throw new FileSystemException( message );
        }
        for( int i = 0; i < m_separators.length; i++ )
        {
            char separator = m_separators[ i ];
            if( name.indexOf( separator ) != -1 )
            {
                final String message = REZ.getString( "invalid-childname.error", name );
                throw new FileSystemException( message );
            }
        }

        if( parent.endsWith( m_separator ) )
        {
            // Either root, or the parent name already ends with the separator
            return parent + name;
        }
        return parent + m_separatorChar + name;
    }

    /**
     * Returns a parent path, or null if the path has no parent.
     *
     * @param path
     *          A <i>normalised</i> path.
     */
    public String getParentPath( String path )
    {
        int idx = path.lastIndexOf( m_separatorChar );
        if( idx == -1 || idx == path.length() - 1 )
        {
            // No parent
            return null;
        }
        if( idx == 0 )
        {
            // Root is the parent
            return m_separator;
        }
        return path.substring( 0, idx );
    }

    /**
     * Normalises a path.  Does the following:
     * <ul>
     * <li>Normalises separators, where more than one can be used.
     * <li>Removes empty path elements.
     * <li>Handles '.' and '..' elements.
     * <li>Removes trailing separator.
     * </ul>
     */
    public void normalisePath( StringBuffer path ) throws FileSystemException
    {
        if( path.length() == 0 )
        {
            return;
        }

        // Adjust separators
        fixSeparators( path );

        // Determine the start of the first element
        int startFirstElem = 0;
        if( path.charAt( 0 ) == m_separatorChar )
        {
            if( path.length() == 1 )
            {
                return;
            }
            startFirstElem = 1;
        }

        // Iterate over each element
        int startElem = startFirstElem;
        int maxlen = path.length();
        while( startElem < maxlen )
        {
            // Find the end of the element
            int endElem = startElem;
            for( ; endElem < maxlen && path.charAt( endElem ) != m_separatorChar; endElem++ )
            {
            }

            int elemLen = endElem - startElem;
            if( elemLen == 0 )
            {
                // An empty element - axe it
                path.delete( endElem, endElem + 1 );
                maxlen = path.length();
                continue;
            }
            if( elemLen == 1 && path.charAt( startElem ) == '.' )
            {
                // A '.' element - axe it
                path.delete( startElem, endElem + 1 );
                maxlen = path.length();
                continue;
            }
            if( elemLen == 2
                && path.charAt( startElem ) == '.'
                && path.charAt( startElem + 1 ) == '.' )
            {
                // A '..' element - remove the previous element
                if( startElem > startFirstElem )
                {
                    int pos = startElem - 2;
                    for( ; path.charAt( pos ) != m_separatorChar; pos-- )
                    {
                    }
                    startElem = pos + 1;
                }
                path.delete( startElem, endElem + 1 );
                maxlen = path.length();
                continue;
            }

            // A regular element
            startElem = endElem + 1;
        }

        // Remove trailing separator
        if( path.charAt( maxlen - 1 ) == m_separatorChar && maxlen > 1 )
        {
            path.delete( maxlen - 1, maxlen );
        }
    }

    /**
     * Adjusts the separators in a name.
     */
    protected boolean fixSeparators( StringBuffer name )
    {
        if( m_separators.length == 0 )
        {
            // Only one valid separator, so don't need to do anything
            return false;
        }

        boolean changed = false;
        int maxlen = name.length();
        for( int i = 0; i < maxlen; i++ )
        {
            char ch = name.charAt( i );
            for( int j = 0; j < m_separators.length; j++ )
            {
                char separator = m_separators[ j ];
                if( ch == separator )
                {
                    name.setCharAt( i, m_separatorChar );
                    changed = true;
                    break;
                }
            }
        }
        return changed;
    }

    /**
     * Extracts the scheme from a URI.
     *
     * @param uri
     *          The URI.
     *
     * @return
     *          The scheme name.  Returns null if there is no scheme.
     */
    public static String extractScheme( String uri )
    {
        return extractScheme( uri, null );
    }

    /**
     * Extracts the scheme from a URI.
     *
     * @param uri
     *          The URI.
     *
     * @param buffer
     *          Returns the remainder of the URI.
     *
     * @return
     *          The scheme name.  Returns null if there is no scheme.
     */
    protected static String extractScheme( String uri, StringBuffer buffer )
    {
        if( buffer != null )
        {
            buffer.setLength( 0 );
            buffer.append( uri );
        }

        int maxPos = uri.length();
        for( int pos = 0; pos < maxPos; pos++ )
        {
            char ch = uri.charAt( pos );

            if( ch == ':' )
            {
                // Found the end of the scheme
                String scheme = uri.substring( 0, pos );
                if( buffer != null )
                {
                    buffer.delete( 0, pos + 1 );
                }
                return scheme;
            }

            if( ( ch >= 'a' && ch <= 'z' )
                || ( ch >= 'A' && ch <= 'Z' ) )
            {
                // A scheme character
                continue;
            }
            if( pos > 0 &&
                ( ( ch >= '0' && ch <= '9' )
                || ch == '+' || ch == '-' || ch == '.' ) )
            {
                // A scheme character (these are not allowed as the first
                // character of the scheme, but can be used as subsequent
                // characters.
                continue;
            }

            // Not a scheme character
            break;
        }

        // No scheme in URI
        return null;
    }
}
