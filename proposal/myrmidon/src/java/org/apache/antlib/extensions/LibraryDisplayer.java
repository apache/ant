/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.extensions;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.avalon.excalibur.extension.Extension;
import org.apache.avalon.excalibur.extension.Specification;
import org.apache.myrmidon.api.TaskException;

/**
 * Utility class to output the information in a jar relating
 * to "Optional Packages" (formely known as "Extensions")
 * and Package Specifications.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class LibraryDisplayer
{
    /**
     * Display the extensions and specifications contained
     * within specified file.
     *
     * @param file the file
     * @throws TaskException if fail to read file
     */
    void displayLibrary( final File file )
        throws TaskException
    {
        final Manifest manifest = getManifest( file );
        displayLibrary( file, manifest );
    }

    /**
     * Display the extensions and specifications contained
     * within specified file.
     *
     * @param file the file to use while reporting
     * @param manifest the manifest of file
     * @throws TaskException if fail to read file
     */
    void displayLibrary( final File file,
                         final Manifest manifest )
        throws TaskException
    {
        final Extension[] available = Extension.getAvailable( manifest );
        final Extension[] required = Extension.getRequired( manifest );
        final Specification[] specifications = getSpecifications( manifest );

        if( 0 == available.length &&
            0 == required.length &&
            0 == specifications.length )
        {
            return;
        }

        final String message = "File: " + file;
        final int size = message.length();
        printLine( size );
        System.out.println( message );
        printLine( size );
        if( 0 != available.length )
        {
            System.out.println( "Extensions Supported By Library:" );
            for( int i = 0; i < available.length; i++ )
            {
                final Extension extension = available[ i ];
                System.out.println( extension.toString() );
            }
        }

        if( 0 != required.length )
        {
            System.out.println( "Extensions Required By Library:" );
            for( int i = 0; i < required.length; i++ )
            {
                final Extension extension = required[ i ];
                System.out.println( extension.toString() );
            }
        }

        if( 0 != specifications.length )
        {
            System.out.println( "Specifications Supported By Library:" );
            for( int i = 0; i < specifications.length; i++ )
            {
                final Specification specification = specifications[ i ];
                displaySpecification( specification );
            }
        }
    }

    /**
     * Print out a line of '-'s equal to specified size.
     *
     * @param size the number of dashes to printout
     */
    private void printLine( final int size )
    {
        for( int i = 0; i < size; i++ )
        {
            System.out.print( "-" );
        }
        System.out.println();
    }

    /**
     * Get specifications from manifest.
     *
     * @param manifest the manifest
     * @return the specifications or null if none
     * @throws TaskException if malformed specification sections
     */
    private Specification[] getSpecifications( final Manifest manifest )
        throws TaskException
    {
        try
        {
            return Specification.getSpecifications( manifest );
        }
        catch( final ParseException pe )
        {
            throw new TaskException( pe.getMessage(), pe );
        }
    }

    /**
     * Print out specification details.
     *
     * @param specification the specification
     */
    private void displaySpecification( final Specification specification )
    {
        final String[] sections = specification.getSections();
        if( null != sections )
        {
            final StringBuffer sb = new StringBuffer( "Sections: " );
            for( int i = 0; i < sections.length; i++ )
            {
                sb.append( " " );
                sb.append( sections[ i ] );
            }
            System.out.println( sb );
        }
        System.out.println( specification.toString() );
    }

    /**
     * retrieve manifest for specified file.
     *
     * @param file the file
     * @return the manifest
     * @throws org.apache.myrmidon.api.TaskException if errror occurs (file not exist,
     *         file not a jar, manifest not exist in file)
     */
    private Manifest getManifest( final File file )
        throws TaskException
    {
        try
        {
            final JarFile jarFile = new JarFile( file );
            return jarFile.getManifest();
        }
        catch( final IOException ioe )
        {
            throw new TaskException( ioe.getMessage(), ioe );
        }
    }
}
