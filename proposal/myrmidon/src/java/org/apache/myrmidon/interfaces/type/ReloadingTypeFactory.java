/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.type;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * This is a TypeFactory that will recreate the ClassLoader each time the
 * create() method is called. This is to provide support for types that
 * use static variables to cache information. While this is a extremely bad
 * practice, sometimes this is unavoidable - especially when using third party
 * libraries.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version CVS $Revision$ $Date$
 */
public class ReloadingTypeFactory
    extends DefaultTypeFactory
{
    /**
     * The URLs that are used to construct the ClassLoader.
     */
    private URL[] m_urls;

    ///The parent classLoader (if any)
    private ClassLoader m_parent;

    /**
     * Construct a factory that recreats a ClassLoader from specified
     * URLs and with specified parent ClassLoader. The specified urls must
     * not be null.
     */
    public ReloadingTypeFactory( final URL[] urls,
                                 final ClassLoader parent )
    {
        if( null == urls )
        {
            throw new NullPointerException( "urls" );
        }
        m_urls = urls;
        m_parent = parent;
    }

    protected ClassLoader getClassLoader()
    {
        return new URLClassLoader( m_urls, m_parent );
    }
}
