/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import org.apache.aut.nativelib.Os;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.util.mappers.FileNameMapper;

/**
 * Utility class that collects the functionality of the various scanDir methods
 * that have been scattered in several tasks before. <p>
 *
 * The only method returns an array of source files. The array is a subset of
 * the files given as a parameter and holds only those that are newer than their
 * corresponding target files.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class SourceFileScanner
    extends AbstractLogEnabled
{
    /**
     * Restrict the given set of files to those that are newer than their
     * corresponding target files.
     *
     * @param files the original set of files
     * @param srcDir all files are relative to this directory
     * @param destDir target files live here. if null file names returned by the
     *      mapper are assumed to be absolute.
     * @param mapper knows how to construct a target file names from source file
     *      names.
     */
    public String[] restrict( String[] files, File srcDir, File destDir,
                              FileNameMapper mapper )
        throws TaskException
    {

        long now = ( new Date() ).getTime();
        StringBuffer targetList = new StringBuffer();

        /*
         * If we're on Windows, we have to munge the time up to 2 secs to
         * be able to check file modification times.
         * (Windows has a max resolution of two secs for modification times)
         * Actually this is a feature of the FAT file system, NTFS does
         * not have it, so if we could reliably passively test for an NTFS
         * file systems we could turn this off...
         */
        if( Os.isFamily( Os.OS_FAMILY_WINDOWS ) )
        {
            now += 2000;
        }

        final ArrayList v = new ArrayList();
        for( int i = 0; i < files.length; i++ )
        {
            final String[] targets = mapper.mapFileName( files[ i ] );
            if( targets == null || targets.length == 0 )
            {
                final String message = files[ i ] + " skipped - don\'t know how to handle it";
                getLogger().debug( message );
                continue;
            }

            final File src = FileUtil.resolveFile( srcDir, files[ i ] );
            if( src.lastModified() > now )
            {
                final String message = "Warning: " + files[ i ] + " modified in the future.";
                getLogger().warn( message );
            }

            boolean added = false;
            targetList.setLength( 0 );
            for( int j = 0; !added && j < targets.length; j++ )
            {
                File dest = FileUtil.resolveFile( destDir, targets[ j ] );

                if( !dest.exists() )
                {
                    final String message =
                        files[ i ] + " added as " + dest.getAbsolutePath() + " doesn\'t exist.";
                    getLogger().debug( message );
                    v.add( files[ i ] );
                    added = true;
                }
                else if( src.lastModified() > dest.lastModified() )
                {
                    final String message =
                        files[ i ] + " added as " + dest.getAbsolutePath() + " is outdated.";
                    getLogger().debug( message );
                    v.add( files[ i ] );
                    added = true;
                }
                else
                {
                    if( targetList.length() > 0 )
                    {
                        targetList.append( ", " );
                    }
                    targetList.append( dest.getAbsolutePath() );
                }
            }

            if( !added )
            {
                final String message = files[ i ] + " omitted as " + targetList.toString() +
                    ( targets.length == 1 ? " is" : " are " ) + " up to date.";
                getLogger().debug( message );
            }

        }
        final String[] result = new String[ v.size() ];
        return (String[])v.toArray( result );
    }

    /**
     * Convinience layer on top of restrict that returns the source files as
     * File objects (containing absolute paths if srcDir is absolute).
     */
    public File[] restrictAsFiles( final String[] files,
                                   final File srcDir,
                                   final File destDir,
                                   final FileNameMapper mapper )
        throws TaskException
    {
        final String[] res = restrict( files, srcDir, destDir, mapper );
        final File[] result = new File[ res.length ];
        for( int i = 0; i < res.length; i++ )
        {
            result[ i ] = new File( srcDir, res[ i ] );
        }
        return result;
    }
}
