/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.extensions;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.jar.JarFile;
import org.apache.avalon.excalibur.extension.Extension;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.ScannerUtil;

/**
 * A set of useful methods relating to extensions.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ExtensionUtil
{
    /**
     * Convert a list of extensionAdapter objects to extensions.
     *
     * @param adapters the list of ExtensionAdapterss to add to convert
     * @throws TaskException if an error occurs
     */
    static ArrayList toExtensions( final ArrayList adapters )
        throws TaskException
    {
        final ArrayList results = new ArrayList();

        final int size = adapters.size();
        for( int i = 0; i < size; i++ )
        {
            final ExtensionAdapter adapter =
                (ExtensionAdapter)adapters.get( i );
            final Extension extension = adapter.toExtension();
            results.add( extension );
        }

        return results;
    }

    /**
     * Generate a list of extensions from a specified fileset.
     *
     * @param librarys the list to add extensions to
     * @param fileset the filesets containing librarys
     * @throws TaskException if an error occurs
     */
    static void extractExtensions( final ArrayList librarys,
                                   final ArrayList fileset )
        throws TaskException
    {
        if( !fileset.isEmpty() )
        {
            final Extension[] extensions = getExtensions( fileset );
            for( int i = 0; i < extensions.length; i++ )
            {
                librarys.add( extensions[ i ] );
            }
        }
    }

    /**
     * Retrieve extensions from the specified librarys.
     *
     * @param librarys the filesets for librarys
     * @return the extensions contained in librarys
     * @throws TaskException if failing to scan librarys
     */
    private static Extension[] getExtensions( final ArrayList librarys )
        throws TaskException
    {
        final ArrayList extensions = new ArrayList();
        final Iterator iterator = librarys.iterator();
        while( iterator.hasNext() )
        {
            final LibFileSet fileSet = (LibFileSet)iterator.next();
            final DirectoryScanner scanner = ScannerUtil.getDirectoryScanner( fileSet );
            final File basedir = scanner.getBasedir();
            final String[] files = scanner.getIncludedFiles();
            for( int i = 0; i < files.length; i++ )
            {
                final File file = new File( basedir, files[ i ] );
                loadExtensions( file, extensions );
            }
        }
        return (Extension[])extensions.toArray( new Extension[ extensions.size() ] );
    }

    /**
     * Load list of available extensions from specified file.
     *
     * @param file the file
     * @param extensions the list to add available extensions to
     * @throws TaskException if there is an error
     */
    private static void loadExtensions( final File file,
                                        final ArrayList extensions )
        throws TaskException
    {
        try
        {
            final JarFile jarFile = new JarFile( file );
            final Extension[] extension =
                Extension.getAvailable( jarFile.getManifest() );
            for( int i = 0; i < extension.length; i++ )
            {
                extensions.add( extension[ i ] );
            }
        }
        catch( final Exception e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }
}
