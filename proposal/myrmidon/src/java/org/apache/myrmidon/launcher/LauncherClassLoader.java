/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.launcher;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;

/**
 * Basic classloader that allows addition of URLs at runtime.
 * Used from Main.class to inject Classes into ClassLoader.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public final class LauncherClassLoader
    extends URLClassLoader
{
    /**
     * Basic constructor.
     *
     * @param urls the Starting URLS
     */
    public LauncherClassLoader( final URL[] urls )
    {
        super( urls );
    }

    /**
     * Add a URL to classloader
     *
     * @param url the url
     */
    public void addURL( final URL url )
    {
        super.addURL( url );
    }

    /**
     * Add an array of URLs to classloader
     *
     * @param url the url
     */
/*
    void addURLs( final URL[] urls )
    {
        for( int i = 0; i < urls.length; i++ )
        {
            addURL( urls[ i ] );
        }
    }
*/
}
