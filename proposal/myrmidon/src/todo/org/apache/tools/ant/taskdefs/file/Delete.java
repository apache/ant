/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.file;

import java.io.File;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;

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
public class Delete extends MatchingTask
{
    protected File file = null;
    protected File dir = null;
    protected Vector filesets = new Vector();
    protected boolean usedMatchingTask = false;
    protected boolean includeEmpty = false;// by default, remove matching empty dirs

    private int verbosity = Project.MSG_VERBOSE;
    private boolean quiet = false;
    private boolean failonerror = true;

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions
     *      should be used, "false"|"off"|"no" when they shouldn't be used.
     */
    public void setDefaultexcludes( boolean useDefaultExcludes )
    {
        usedMatchingTask = true;
        super.setDefaultexcludes( useDefaultExcludes );
    }

    /**
     * Set the directory from which files are to be deleted
     *
     * @param dir the directory path.
     */
    public void setDir( File dir )
    {
        this.dir = dir;
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes( String excludes )
        throws TaskException
    {
        usedMatchingTask = true;
        super.setExcludes( excludes );
    }

    /**
     * Sets the name of the file containing the includes patterns.
     *
     * @param excludesfile A string containing the filename to fetch the include
     *      patterns from.
     */
    public void setExcludesfile( File excludesfile )
        throws TaskException
    {
        usedMatchingTask = true;
        super.setExcludesfile( excludesfile );
    }

    /**
     * this flag means 'note errors to the output, but keep going'
     *
     * @param failonerror true or false
     */
    public void setFailOnError( boolean failonerror )
    {
        this.failonerror = failonerror;
    }

    /**
     * Set the name of a single file to be removed.
     *
     * @param file the file to be deleted
     */
    public void setFile( File file )
    {
        this.file = file;
    }

    /**
     * Used to delete empty directories.
     *
     * @param includeEmpty The new IncludeEmptyDirs value
     */
    public void setIncludeEmptyDirs( boolean includeEmpty )
    {
        this.includeEmpty = includeEmpty;
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes( String includes )
        throws TaskException
    {
        usedMatchingTask = true;
        super.setIncludes( includes );
    }

    /**
     * Sets the name of the file containing the includes patterns.
     *
     * @param includesfile A string containing the filename to fetch the include
     *      patterns from.
     */
    public void setIncludesfile( File includesfile )
        throws TaskException
    {
        usedMatchingTask = true;
        super.setIncludesfile( includesfile );
    }

    /**
     * If the file does not exist, do not display a diagnostic message or modify
     * the exit status to reflect an error. This means that if a file or
     * directory cannot be deleted, then no error is reported. This setting
     * emulates the -f option to the Unix &quot;rm&quot; command. Default is
     * false meaning things are &quot;noisy&quot;
     *
     * @param quiet "true" or "on"
     */
    public void setQuiet( boolean quiet )
    {
        this.quiet = quiet;
        if( quiet )
        {
            this.failonerror = false;
        }
    }

    /**
     * Used to force listing of all names of deleted files.
     *
     * @param verbose "true" or "on"
     */
    public void setVerbose( boolean verbose )
    {
        if( verbose )
        {
            this.verbosity = Project.MSG_INFO;
        }
        else
        {
            this.verbosity = Project.MSG_VERBOSE;
        }
    }

    /**
     * Adds a set of files (nested fileset attribute).
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet set )
    {
        filesets.addElement( set );
    }

    /**
     * add a name entry on the exclude list
     *
     * @return Description of the Returned Value
     */
    public PatternSet.NameEntry createExclude()
        throws TaskException
    {
        usedMatchingTask = true;
        return super.createExclude();
    }

    /**
     * add a name entry on the include list
     *
     * @return Description of the Returned Value
     */
    public PatternSet.NameEntry createInclude()
        throws TaskException
    {
        usedMatchingTask = true;
        return super.createInclude();
    }

    /**
     * add a set of patterns
     *
     * @return Description of the Returned Value
     */
    public PatternSet createPatternSet()
        throws TaskException
    {
        usedMatchingTask = true;
        return super.createPatternSet();
    }

    /**
     * Delete the file(s).
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        if( usedMatchingTask )
        {
            getLogger().info( "DEPRECATED - Use of the implicit FileSet is deprecated.  Use a nested fileset element instead." );
        }

        if( file == null && dir == null && filesets.size() == 0 )
        {
            throw new TaskException( "At least one of the file or dir attributes, or a fileset element, must be set." );
        }

        if( quiet && failonerror )
        {
            throw new TaskException( "quiet and failonerror cannot both be set to true" );
        }

        // delete the single file
        if( file != null )
        {
            if( file.exists() )
            {
                if( file.isDirectory() )
                {
                    getLogger().info( "Directory " + file.getAbsolutePath() + " cannot be removed using the file attribute.  Use dir instead." );
                }
                else
                {
                    getLogger().info( "Deleting: " + file.getAbsolutePath() );

                    if( !file.delete() )
                    {
                        String message = "Unable to delete file " + file.getAbsolutePath();
                        if( failonerror )
                            throw new TaskException( message );
                        else
                            log( message,
                                 quiet ? Project.MSG_VERBOSE : Project.MSG_WARN );
                    }
                }
            }
            else
            {
                log( "Could not find file " + file.getAbsolutePath() + " to delete.",
                     Project.MSG_VERBOSE );
            }
        }

        // delete the directory
        if( dir != null && dir.exists() && dir.isDirectory() && !usedMatchingTask )
        {
            /*
             * If verbosity is MSG_VERBOSE, that mean we are doing regular logging
             * (backwards as that sounds).  In that case, we want to print one
             * message about deleting the top of the directory tree.  Otherwise,
             * the removeDir method will handle messages for _all_ directories.
             */
            if( verbosity == Project.MSG_VERBOSE )
            {
                getLogger().info( "Deleting directory " + dir.getAbsolutePath() );
            }
            removeDir( dir );
        }

        // delete the files in the filesets
        for( int i = 0; i < filesets.size(); i++ )
        {
            FileSet fs = (FileSet)filesets.elementAt( i );
            try
            {
                DirectoryScanner ds = fs.getDirectoryScanner( project );
                String[] files = ds.getIncludedFiles();
                String[] dirs = ds.getIncludedDirectories();
                removeFiles( fs.getDir( project ), files, dirs );
            }
            catch( TaskException be )
            {
                // directory doesn't exist or is not readable
                if( failonerror )
                {
                    throw be;
                }
                else
                {
                    log( be.getMessage(),
                         quiet ? Project.MSG_VERBOSE : Project.MSG_WARN );
                }
            }
        }

        // delete the files from the default fileset
        if( usedMatchingTask && dir != null )
        {
            try
            {
                DirectoryScanner ds = super.getDirectoryScanner( dir );
                String[] files = ds.getIncludedFiles();
                String[] dirs = ds.getIncludedDirectories();
                removeFiles( dir, files, dirs );
            }
            catch( TaskException be )
            {
                // directory doesn't exist or is not readable
                if( failonerror )
                {
                    throw be;
                }
                else
                {
                    log( be.getMessage(),
                         quiet ? Project.MSG_VERBOSE : Project.MSG_WARN );
                }
            }
        }
    }

    //************************************************************************
    //  protected and private methods
    //************************************************************************

    protected void removeDir( File d )
        throws TaskException
    {
        String[] list = d.list();
        if( list == null )
            list = new String[ 0 ];
        for( int i = 0; i < list.length; i++ )
        {
            String s = list[ i ];
            File f = new File( d, s );
            if( f.isDirectory() )
            {
                removeDir( f );
            }
            else
            {
                log( "Deleting " + f.getAbsolutePath(), verbosity );
                if( !f.delete() )
                {
                    String message = "Unable to delete file " + f.getAbsolutePath();
                    if( failonerror )
                        throw new TaskException( message );
                    else
                        log( message,
                             quiet ? Project.MSG_VERBOSE : Project.MSG_WARN );
                }
            }
        }
        log( "Deleting directory " + d.getAbsolutePath(), verbosity );
        if( !d.delete() )
        {
            String message = "Unable to delete directory " + dir.getAbsolutePath();
            if( failonerror )
                throw new TaskException( message );
            else
                log( message,
                     quiet ? Project.MSG_VERBOSE : Project.MSG_WARN );
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
    protected void removeFiles( File d, String[] files, String[] dirs )
        throws TaskException
    {
        if( files.length > 0 )
        {
            getLogger().info( "Deleting " + files.length + " files from " + d.getAbsolutePath() );
            for( int j = 0; j < files.length; j++ )
            {
                File f = new File( d, files[ j ] );
                log( "Deleting " + f.getAbsolutePath(), verbosity );
                if( !f.delete() )
                {
                    String message = "Unable to delete file " + f.getAbsolutePath();
                    if( failonerror )
                        throw new TaskException( message );
                    else
                        log( message,
                             quiet ? Project.MSG_VERBOSE : Project.MSG_WARN );
                }
            }
        }

        if( dirs.length > 0 && includeEmpty )
        {
            int dirCount = 0;
            for( int j = dirs.length - 1; j >= 0; j-- )
            {
                File dir = new File( d, dirs[ j ] );
                String[] dirFiles = dir.list();
                if( dirFiles == null || dirFiles.length == 0 )
                {
                    log( "Deleting " + dir.getAbsolutePath(), verbosity );
                    if( !dir.delete() )
                    {
                        String message = "Unable to delete directory "
                            + dir.getAbsolutePath();
                        if( failonerror )
                            throw new TaskException( message );
                        else
                            log( message,
                                 quiet ? Project.MSG_VERBOSE : Project.MSG_WARN );
                    }
                    else
                    {
                        dirCount++;
                    }
                }
            }

            if( dirCount > 0 )
            {
                getLogger().info( "Deleted " + dirCount + " director" +
                     ( dirCount == 1 ? "y" : "ies" ) +
                     " from " + d.getAbsolutePath() );
            }
        }
    }
}

