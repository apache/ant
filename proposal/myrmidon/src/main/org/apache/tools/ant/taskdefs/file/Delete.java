/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.file;

import java.io.File;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Deletes a file or directory, or set of files defined by a fileset. The
 * original delete task would delete a file, or a set of files using the
 * include/exclude syntax. The deltree task would delete a directory tree. This
 * task combines the functionality of these two originally distinct tasks. <p>
 *
 * Currently Delete extends MatchingTask. This is intend <i>only</i> to provide
 * backwards compatibility for a release. The future position is to use nested
 * filesets exclusively.</p>
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author Tom Dimock <a href="mailto:tad1@cornell.edu">tad1@cornell.edu</a>
 * @author Glenn McAllister <a href="mailto:glennm@ca.ibm.com">glennm@ca.ibm.com
 *      </a>
 * @author Jon S. Stevens <a href="mailto:jon@latchkey.com">jon@latchkey.com</a>
 */
public class Delete
    extends Task
{
    private final ArrayList filesets = new ArrayList();
    private File m_dir;
    private File m_file;
    private boolean includeEmpty;// by default, remove matching empty dirs

    /**
     * Set the directory from which files are to be deleted
     *
     * @param dir the directory path.
     */
    public void setDir( final File dir )
    {
        m_dir = dir;
    }

    /**
     * Set the name of a single file to be removed.
     *
     * @param file the file to be deleted
     */
    public void setFile( final File file )
    {
        m_file = file;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet set )
    {
        filesets.add( set );
    }

    /**
     * Delete the file(s).
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        if( m_file == null && m_dir == null && filesets.size() == 0 )
        {
            final String message = "At least one of the file or dir attributes, " +
                "or a fileset element, must be set.";
            throw new TaskException( message );
        }

        // delete the single file
        if( null != m_file )
        {
            if( m_file.exists() )
            {
                if( m_file.isDirectory() )
                {
                    final String message = "Directory " + m_file.getAbsolutePath() +
                        " cannot be removed using the file attribute.  Use dir instead.";
                    getLogger().info( message );
                }
                else
                {
                    getLogger().info( "Deleting: " + m_file.getAbsolutePath() );
                    if( !m_file.delete() )
                    {
                        final String message = "Unable to delete file " + m_file.getAbsolutePath();
                        throw new TaskException( message );
                    }
                }
            }
            else
            {
                final String message =
                    "Could not find file " + m_file.getAbsolutePath() + " to delete.";
                getLogger().debug( message );
            }
        }

        // delete the directory
        if( m_dir != null && m_dir.exists() && m_dir.isDirectory() )
        {
            getLogger().info( "Deleting directory " + m_dir.getAbsolutePath() );
            removeDir( m_dir );
        }

        // delete the files in the filesets
        for( int i = 0; i < filesets.size(); i++ )
        {
            FileSet fs = (FileSet)filesets.get( i );
            try
            {
                DirectoryScanner ds = fs.getDirectoryScanner();
                String[] files = ds.getIncludedFiles();
                String[] dirs = ds.getIncludedDirectories();
                removeFiles( fs.getDir(), files, dirs );
            }
            catch( TaskException be )
            {
                // directory doesn't exist or is not readable
                throw be;
            }
        }
    }

    //************************************************************************
    //  protected and private methods
    //************************************************************************

    protected void removeDir( final File baseDir )
        throws TaskException
    {
        final File[] list = baseDir.listFiles();
        if( list != null )
        {
            deleteFiles( list );
        }
        getLogger().debug( "Deleting directory " + baseDir.getAbsolutePath() );
        if( !baseDir.delete() )
        {
            String message = "Unable to delete directory " + m_dir.getAbsolutePath();
            throw new TaskException( message );
        }
    }

    private void deleteFiles( final File[] list )
        throws TaskException
    {
        for( int i = 0; i < list.length; i++ )
        {
            final File file = list[ i ];
            if( file.isDirectory() )
            {
                removeDir( file );
            }
            else
            {
                getLogger().debug( "Deleting " + file.getAbsolutePath() );
                if( !file.delete() )
                {
                    String message = "Unable to delete file " + file.getAbsolutePath();
                    throw new TaskException( message );
                }
            }
        }
    }

    /**
     * remove an array of files in a directory, and a list of subdirectories
     * which will only be deleted if 'includeEmpty' is true
     *
     * @param d directory to work from
     * @param files array of files to delete; can be of zero length
     * @param dirs array of directories to delete; can of zero length
     */
    protected void removeFiles( final File baseDir,
                                final String[] files,
                                final String[] dirs )
        throws TaskException
    {
        if( files.length > 0 )
        {
            final String message = "Deleting " + files.length + " files from " + baseDir.getAbsolutePath();
            getLogger().info( message );
            for( int i = 0; i < files.length; i++ )
            {
                final File file = new File( baseDir, files[ i ] );
                getLogger().debug( "Deleting " + file.getAbsolutePath() );
                if( !file.delete() )
                {
                    String message2 = "Unable to delete file " + file.getAbsolutePath();
                    throw new TaskException( message2 );
                }
            }
        }

        if( dirs.length > 0 && includeEmpty )
        {
            int dirCount = 0;
            for( int j = dirs.length - 1; j >= 0; j-- )
            {
                File dir = new File( baseDir, dirs[ j ] );
                String[] dirFiles = dir.list();
                if( dirFiles == null || dirFiles.length == 0 )
                {
                    getLogger().debug( "Deleting " + dir.getAbsolutePath() );
                    if( !dir.delete() )
                    {
                        final String message =
                            "Unable to delete directory " + dir.getAbsolutePath();
                        throw new TaskException( message );
                    }
                    else
                    {
                        dirCount++;
                    }
                }
            }

            if( dirCount > 0 )
            {
                final String message = "Deleted " + dirCount + " director" +
                    ( dirCount == 1 ? "y" : "ies" ) + " from " + baseDir.getAbsolutePath();
                getLogger().info( message );
            }
        }
    }
}

