/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.extension;

import java.io.File;
import java.text.ParseException;
import java.util.jar.Manifest;
import org.apache.tools.ant.BuildException;

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
     * @throws BuildException if fail to read file
     */
    void displayLibrary( final File file )
        throws BuildException
    {
        final Manifest manifest = ExtensionUtil.getManifest( file );
        displayLibrary( file, manifest );
    }

    /**
     * Display the extensions and specifications contained
     * within specified file.
     *
     * @param file the file to use while reporting
     * @param manifest the manifest of file
     * @throws BuildException if fail to read file
     */
    void displayLibrary( final File file,
                         final Manifest manifest )
        throws BuildException
    {
        final Extension[] available = Extension.getAvailable( manifest );
        final Extension[] required = Extension.getRequired( manifest );
        final Extension[] options = Extension.getOptions( manifest );
        final Specification[] specifications = getSpecifications( manifest );

        if( 0 == available.length &&
            0 == required.length &&
            0 == options.length &&
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

        if( 0 != options.length )
        {
            System.out.println( "Extensions that will be used by Library if present:" );
            for( int i = 0; i < options.length; i++ )
            {
                final Extension extension = options[ i ];
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
     * @throws BuildException if malformed specification sections
     */
    private Specification[] getSpecifications( final Manifest manifest )
        throws BuildException
    {
        try
        {
            return Specification.getSpecifications( manifest );
        }
        catch( final ParseException pe )
        {
            throw new BuildException( pe.getMessage(), pe );
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
}
