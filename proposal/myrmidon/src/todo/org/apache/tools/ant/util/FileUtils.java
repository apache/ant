/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;
import java.util.StringTokenizer;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.framework.logger.Logger;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.FilterSetCollection;

/**
 * This class also encapsulates methods which allow Files to be refered to using
 * abstract path names which are translated to native system file paths at
 * runtime as well as copying files or setting there last modification time.
 *
 * @author duncan@x180.com
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public class FileUtils
{
    /**
     * Parse out a path as appropriate for current OS.
     */
    public static String[] parsePath( final String path )
    {
        final PathTokenizer elements = new PathTokenizer( path );

        final ArrayList result = new ArrayList();
        while( elements.hasNext() )
        {
            result.add( elements.next() );
        }

        return (String[])result.toArray( new String[ result.size() ] );
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used, if source files may overwrite
     * newer destination files and the last modified time of <code>destFile</code>
     * file should be made equal to the last modified time of <code>sourceFile</code>
     * .
     */
    public static void copyFile( final File sourceFile,
                                 final File destFile,
                                 final FilterSetCollection filters )
        throws IOException, TaskException
    {
        if( !destFile.exists() ||
            destFile.lastModified() < sourceFile.lastModified() )
        {
            if( destFile.exists() && destFile.isFile() )
            {
                destFile.delete();
            }

            // ensure that parent dir of dest file exists!
            // not using getParentFile method to stay 1.1 compat
            File parent = destFile.getParentFile();
            if( !parent.exists() )
            {
                parent.mkdirs();
            }

            if( filters != null && filters.hasFilters() )
            {
                BufferedReader in = new BufferedReader( new FileReader( sourceFile ) );
                BufferedWriter out = new BufferedWriter( new FileWriter( destFile ) );

                int length;
                String newline = null;
                String line = in.readLine();
                while( line != null )
                {
                    if( line.length() == 0 )
                    {
                        out.newLine();
                    }
                    else
                    {
                        newline = filters.replaceTokens( line );
                        out.write( newline );
                        out.newLine();
                    }
                    line = in.readLine();
                }

                out.close();
                in.close();
            }
            else
            {
                FileInputStream in = new FileInputStream( sourceFile );
                FileOutputStream out = new FileOutputStream( destFile );

                byte[] buffer = new byte[ 8 * 1024 ];
                int count = 0;
                do
                {
                    out.write( buffer, 0, count );
                    count = in.read( buffer, 0, buffer.length );
                } while( count != -1 );

                in.close();
                out.close();
            }
        }
    }

    /**
     * &quot;normalize&quot; the given absolute path. <p>
     *
     * This includes:
     * <ul>
     *   <li> Uppercase the drive letter if there is one.</li>
     *   <li> Remove redundant slashes after the drive spec.</li>
     *   <li> resolve all ./, .\, ../ and ..\ sequences.</li>
     *   <li> DOS style paths that start with a drive letter will have \ as the
     *   separator.</li>
     * </ul>
     *
     *
     * @param path Description of Parameter
     * @return Description of the Returned Value
     * @throws java.lang.NullPointerException if the file path is equal to null.
     */
    public static File normalize( String path )
        throws TaskException
    {
        String orig = path;

        path = path.replace( '/', File.separatorChar )
            .replace( '\\', File.separatorChar );

        // make sure we are dealing with an absolute path
        if( !path.startsWith( File.separator ) &&
            !( path.length() >= 2 &&
            Character.isLetter( path.charAt( 0 ) ) &&
            path.charAt( 1 ) == ':' )
        )
        {
            String msg = path + " is not an absolute path";
            throw new TaskException( msg );
        }

        boolean dosWithDrive = false;
        String root = null;
        // Eliminate consecutive slashes after the drive spec
        if( path.length() >= 2 &&
            Character.isLetter( path.charAt( 0 ) ) &&
            path.charAt( 1 ) == ':' )
        {

            dosWithDrive = true;

            char[] ca = path.replace( '/', '\\' ).toCharArray();
            StringBuffer sb = new StringBuffer();
            sb.append( Character.toUpperCase( ca[ 0 ] ) ).append( ':' );

            for( int i = 2; i < ca.length; i++ )
            {
                if( ( ca[ i ] != '\\' ) ||
                    ( ca[ i ] == '\\' && ca[ i - 1 ] != '\\' )
                )
                {
                    sb.append( ca[ i ] );
                }
            }

            path = sb.toString().replace( '\\', File.separatorChar );
            if( path.length() == 2 )
            {
                root = path;
                path = "";
            }
            else
            {
                root = path.substring( 0, 3 );
                path = path.substring( 3 );
            }

        }
        else
        {
            if( path.length() == 1 )
            {
                root = File.separator;
                path = "";
            }
            else if( path.charAt( 1 ) == File.separatorChar )
            {
                // UNC drive
                root = File.separator + File.separator;
                path = path.substring( 2 );
            }
            else
            {
                root = File.separator;
                path = path.substring( 1 );
            }
        }

        Stack s = new Stack();
        s.push( root );
        StringTokenizer tok = new StringTokenizer( path, File.separator );
        while( tok.hasMoreTokens() )
        {
            String thisToken = tok.nextToken();
            if( ".".equals( thisToken ) )
            {
                continue;
            }
            else if( "..".equals( thisToken ) )
            {
                if( s.size() < 2 )
                {
                    throw new TaskException( "Cannot resolve path " + orig );
                }
                else
                {
                    s.pop();
                }
            }
            else
            {// plain component
                s.push( thisToken );
            }
        }

        StringBuffer sb = new StringBuffer();
        for( int i = 0; i < s.size(); i++ )
        {
            if( i > 1 )
            {
                // not before the filesystem root and not after it, since root
                // already contains one
                sb.append( File.separatorChar );
            }
            sb.append( s.get( i ) );
        }

        path = sb.toString();
        if( dosWithDrive )
        {
            path = path.replace( '/', '\\' );
        }
        return new File( path );
    }

    /**
     * Put quotes around the given String if necessary. <p>
     *
     * If the argument doesn't include spaces or quotes, return it as is. If it
     * contains double quotes, use single quotes - else surround the argument by
     * double quotes.</p>
     *
     * @param argument Description of Parameter
     * @return Description of the Returned Value
     */
    public static String quoteArgument( String argument )
        throws TaskException
    {
        if( argument.indexOf( "\"" ) > -1 )
        {
            if( argument.indexOf( "\'" ) > -1 )
            {
                throw new TaskException( "Can\'t handle single and double quotes in same argument" );
            }
            else
            {
                return '\'' + argument + '\'';
            }
        }
        else if( argument.indexOf( "\'" ) > -1 || argument.indexOf( " " ) > -1 )
        {
            return '\"' + argument + '\"';
        }
        else
        {
            return argument;
        }
    }

    public static String[] translateCommandline( String to_process )
        throws TaskException
    {
        if( to_process == null || to_process.length() == 0 )
        {
            return new String[ 0 ];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        StringTokenizer tok = new StringTokenizer( to_process, "\"\' ", true );
        ArrayList v = new ArrayList();
        StringBuffer current = new StringBuffer();

        while( tok.hasMoreTokens() )
        {
            String nextTok = tok.nextToken();
            switch( state )
            {
                case inQuote:
                    if( "\'".equals( nextTok ) )
                    {
                        state = normal;
                    }
                    else
                    {
                        current.append( nextTok );
                    }
                    break;
                case inDoubleQuote:
                    if( "\"".equals( nextTok ) )
                    {
                        state = normal;
                    }
                    else
                    {
                        current.append( nextTok );
                    }
                    break;
                default:
                    if( "\'".equals( nextTok ) )
                    {
                        state = inQuote;
                    }
                    else if( "\"".equals( nextTok ) )
                    {
                        state = inDoubleQuote;
                    }
                    else if( " ".equals( nextTok ) )
                    {
                        if( current.length() != 0 )
                        {
                            v.add( current.toString() );
                            current.setLength( 0 );
                        }
                    }
                    else
                    {
                        current.append( nextTok );
                    }
                    break;
            }
        }

        if( current.length() != 0 )
        {
            v.add( current.toString() );
        }

        if( state == inQuote || state == inDoubleQuote )
        {
            throw new TaskException( "unbalanced quotes in " + to_process );
        }

        final String[] args = new String[ v.size() ];
        return (String[])v.toArray( args );
    }

    /**
     * Returns its argument with all file separator characters replaced so that
     * they match the local OS conventions.
     */
    public static String translateFile( final String source )
    {
        if( source == null )
            return "";

        final StringBuffer result = new StringBuffer( source );
        for( int i = 0; i < result.length(); i++ )
        {
            translateFileSep( result, i );
        }

        return result.toString();
    }

    /**
     * Translates all occurrences of / or \ to correct separator of the current
     * platform and returns whether it had to do any replacements.
     *
     * @param buffer Description of Parameter
     * @param pos Description of Parameter
     * @return Description of the Returned Value
     */
    public static boolean translateFileSep( StringBuffer buffer, int pos )
    {
        if( buffer.charAt( pos ) == '/' || buffer.charAt( pos ) == '\\' )
        {
            buffer.setCharAt( pos, File.separatorChar );
            return true;
        }
        return false;
    }

    /**
     * Splits a PATH (with : or ; as separators) into its parts.
     */
    public static String[] translatePath( final File baseDirectory,
                                          String source,
                                          final Logger logger )
    {
        final ArrayList result = new ArrayList();
        if( source == null )
            return new String[ 0 ];

        final String[] elements = parsePath( source );
        StringBuffer element = new StringBuffer();
        for( int i = 0; i < elements.length; i++ )
        {
            element.setLength( 0 );
            final String pathElement = elements[ i ];
            try
            {
                element.append( resolveFile( baseDirectory, pathElement ) );
            }
            catch( TaskException e )
            {
                final String message =
                    "Dropping path element " + pathElement + " as it is not valid relative to the project";
                logger.debug( message );
            }

            for( int j = 0; j < element.length(); j++ )
            {
                translateFileSep( element, j );
            }
            result.add( element.toString() );
        }

        return (String[])result.toArray( new String[ result.size() ] );
    }

    /**
     * Resolve a filename with Project's help - if we know one that is. <p>
     *
     * Assume the filename is absolute if project is null.</p>
     */
    public static String resolveFile( final File baseDirectory, final String relativeName )
        throws TaskException
    {
        if( null != baseDirectory )
        {
            final File file = FileUtil.resolveFile( baseDirectory, relativeName );
            return file.getAbsolutePath();
        }
        return relativeName;
    }
}

