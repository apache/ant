/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;
import java.util.StringTokenizer;
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
}

