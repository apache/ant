/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.vfs.provider;

import org.apache.aut.vfs.FileName;
import org.apache.aut.vfs.FileSystemException;
import org.apache.aut.vfs.NameScope;

/**
 * A default file name implementation.
 *
 * @author Adam Murdoch
 */
public class DefaultFileName implements FileName
{
    private UriParser m_parser;
    private String m_rootPrefix;
    private String m_absPath;

    // Cached stuff
    private String m_uri;
    private String m_baseName;

    public DefaultFileName( UriParser parser, String rootPrefix, String absPath )
    {
        m_parser = parser;
        m_rootPrefix = rootPrefix;
        m_absPath = absPath;
    }

    // TODO - make these usable as hash keys

    /**
     * Returns the URI of the file.
     */
    public String toString()
    {
        return getURI();
    }

    /**
     * Returns the base name of the file.
     */
    public String getBaseName()
    {
        if( m_baseName == null )
        {
            m_baseName = m_parser.getBaseName( m_absPath );
        }
        return m_baseName;
    }

    /**
     * Returns the absolute path of the file, relative to the root of the
     * file system that the file belongs to.
     */
    public String getPath()
    {
        return m_absPath;
    }

    /**
     * Returns the name of a child of the file.
     */
    public FileName resolveName( String name, NameScope scope ) throws FileSystemException
    {
        if( scope == NameScope.CHILD )
        {
            String childPath = m_parser.getChildPath( m_absPath, name );
            return new DefaultFileName( m_parser, m_rootPrefix, childPath );
        }
        else if( scope == NameScope.FILE_SYSTEM )
        {
            String absPath = m_parser.resolvePath( m_absPath, name );
            return new DefaultFileName( m_parser, m_rootPrefix, absPath );
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the name of the parent of the file.
     */
    public FileName getParent()
    {
        String parentPath = m_parser.getParentPath( m_absPath );
        if( parentPath == null )
        {
            return null;
        }
        return new DefaultFileName( m_parser, m_rootPrefix, parentPath );
    }

    /**
     * Resolves a name, relative to the file.  If the supplied name is an
     * absolute path, then it is resolved relative to the root of the
     * file system that the file belongs to.  If a relative name is supplied,
     * then it is resolved relative to this file name.
     */
    public FileName resolveName( String path ) throws FileSystemException
    {
        return resolveName( path, NameScope.FILE_SYSTEM );
    }

    /**
     * Returns the absolute URI of the file.
     */
    public String getURI()
    {
        if( m_uri == null )
        {
            m_uri = m_parser.getUri( m_rootPrefix, m_absPath );
        }
        return m_uri;
    }
}
