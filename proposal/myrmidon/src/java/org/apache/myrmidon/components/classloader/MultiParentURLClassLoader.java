/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * A URLClassLoader with more than one parent.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class MultiParentURLClassLoader
    extends URLClassLoader
{
    private final ClassLoader[] m_parents;

    /**
     * Constructs a new URLClassLoader for the given URLs.
     *
     * @param urls the URLs from which to load classes and resources
     * @param parents the parent class loaderer for delegation
     */
    public MultiParentURLClassLoader( final URL[] urls, final ClassLoader[] parents )
    {
        super( urls );
        m_parents = parents;
    }

    /**
     * Finds a class.
     *
     * @param name the name of the class
     * @return the resulting class
     * @exception ClassNotFoundException if the class could not be found
     */
    protected Class findClass( final String name )
        throws ClassNotFoundException
    {
        // Try the parent classloaders first
        for( int i = 0; i < m_parents.length; i++ )
        {
            try
            {
                final ClassLoader parent = m_parents[ i ];
                return parent.loadClass( name );
            }
            catch( ClassNotFoundException e )
            {
                // Ignore - continue to the next ClassLoader
            }
        }

        // Now this classloader
        return super.findClass( name );
    }

    /**
     * Finds a resource.
     *
     * @param name the name of the resource
     * @return a <code>URL</code> for the resource, or <code>null</code>
     * if the resource could not be found.
     */
    public URL findResource( final String name )
    {
        // Try the parent classloaders first
        for( int i = 0; i < m_parents.length; i++ )
        {
            final ClassLoader parent = m_parents[ i ];
            final URL resource = parent.getResource( name );
            if( resource != null )
            {
                return resource;
            }
        }

        // Now this classloader
        return super.findResource( name );
    }

    /**
     * Returns an Enumeration of URLs representing all of the resources
     * having the specified name.
     *
     * @param name the resource name
     * @throws IOException if an I/O exception occurs
     * @return an <code>Enumeration</code> of <code>URL</code>s
     */
    public Enumeration findResources( final String name ) throws IOException
    {
        // Need to filter out duplicate resources
        final ArrayList urls = new ArrayList();
        final Set urlSet = new HashSet();

        // Gather the resources from the parent classloaders
        for( int i = 0; i < m_parents.length; i++ )
        {
            final ClassLoader parent = m_parents[ i ];
            final Enumeration enum = parent.getResources( name );
            addUrls( enum, urls, urlSet );
        }

        // Gather the resources from this classloader
        addUrls( super.findResources( name ), urls, urlSet );

        return Collections.enumeration( urls );
    }

    /**
     * Adds those URLs not already present.
     */
    private void addUrls( final Enumeration enum,
                          final List urls,
                          final Set urlSet )
    {
        while( enum.hasMoreElements() )
        {
            final URL url = (URL)enum.nextElement();
            final String urlStr = url.toExternalForm();
            if( !urlSet.contains( urlStr ) )
            {
                urls.add( url );
                urlSet.add( urlStr );
            }
        }
    }
}
