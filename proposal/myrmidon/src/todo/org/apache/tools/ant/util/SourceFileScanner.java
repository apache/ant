/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.util;

import java.io.File;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Os;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.avalon.excalibur.io.FileUtil;

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
{
    private Task m_task;

    /**
     * @param task The task we should log messages through
     */
    public SourceFileScanner( Task task )
    {
        this.m_task = task;
    }

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
     * @return Description of the Returned Value
     */
    public String[] restrict( String[] files, File srcDir, File destDir,
                              FileNameMapper mapper )
        throws TaskException
    {

        long now = ( new java.util.Date() ).getTime();
        StringBuffer targetList = new StringBuffer();

        /*
         * If we're on Windows, we have to munge the time up to 2 secs to
         * be able to check file modification times.
         * (Windows has a max resolution of two secs for modification times)
         * Actually this is a feature of the FAT file system, NTFS does
         * not have it, so if we could reliably passively test for an NTFS
         * file systems we could turn this off...
         */
        if( Os.isFamily( "windows" ) )
        {
            now += 2000;
        }

        ArrayList v = new ArrayList();
        for( int i = 0; i < files.length; i++ )
        {

            String[] targets = mapper.mapFileName( files[ i ] );
            if( targets == null || targets.length == 0 )
            {
                m_task.log( files[ i ] + " skipped - don\'t know how to handle it",
                          Project.MSG_VERBOSE );
                continue;
            }

            File src = FileUtil.resolveFile( srcDir, files[ i ] );

            if( src.lastModified() > now )
            {
                m_task.log( "Warning: " + files[ i ] + " modified in the future.",
                          Project.MSG_WARN );
            }

            boolean added = false;
            targetList.setLength( 0 );
            for( int j = 0; !added && j < targets.length; j++ )
            {
                File dest = FileUtil.resolveFile( destDir, targets[ j ] );

                if( !dest.exists() )
                {
                    m_task.log( files[ i ] + " added as " + dest.getAbsolutePath() + " doesn\'t exist.",
                              Project.MSG_VERBOSE );
                    v.add( files[ i ] );
                    added = true;
                }
                else if( src.lastModified() > dest.lastModified() )
                {
                    m_task.log( files[ i ] + " added as " + dest.getAbsolutePath() + " is outdated.",
                              Project.MSG_VERBOSE );
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
                m_task.log( files[ i ] + " omitted as " + targetList.toString()
                          + ( targets.length == 1 ? " is" : " are " )
                          + " up to date.", Project.MSG_VERBOSE );
            }

        }
        final String[] result = new String[ v.size() ];
        return (String[])v.toArray( result );
    }

    /**
     * Convinience layer on top of restrict that returns the source files as
     * File objects (containing absolute paths if srcDir is absolute).
     *
     * @param files Description of Parameter
     * @param srcDir Description of Parameter
     * @param destDir Description of Parameter
     * @param mapper Description of Parameter
     * @return Description of the Returned Value
     */
    public File[] restrictAsFiles( String[] files, File srcDir, File destDir,
                                   FileNameMapper mapper )
        throws TaskException
    {
        String[] res = restrict( files, srcDir, destDir, mapper );
        File[] result = new File[ res.length ];
        for( int i = 0; i < res.length; i++ )
        {
            result[ i ] = new File( srcDir, res[ i ] );
        }
        return result;
    }
}
