/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.archive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.PatternSet;
import org.apache.myrmidon.framework.PatternUtil;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ScannerUtil;

/**
 * Unzip a file.
 *
 * @author costin@dnt.ro
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public abstract class Expand
    extends MatchingTask
{
    private boolean m_overwrite = true;
    private ArrayList m_patternsets = new ArrayList();
    private ArrayList m_filesets = new ArrayList();
    private File m_dest;//req
    private File m_src;

    /**
     * Set the destination directory. File will be unzipped into the destination
     * directory.
     *
     * @param dest Path to the directory.
     */
    public void setDest( final File dest )
    {
        m_dest = dest;
    }

    /**
     * Should we overwrite files in dest, even if they are newer than the
     * corresponding entries in the archive?
     *
     * @param overwrite The new Overwrite value
     */
    public void setOverwrite( final boolean overwrite )
    {
        m_overwrite = overwrite;
    }

    /**
     * Set the path to zip-file.
     *
     * @param src Path to zip-file.
     */
    public void setSrc( final File src )
    {
        m_src = src;
    }

    /**
     * Add a fileset
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( final FileSet set )
    {
        m_filesets.add( set );
    }

    /**
     * Add a patternset
     *
     * @param set The feature to be added to the Patternset attribute
     */
    public void addPatternset( final PatternSet set )
    {
        m_patternsets.add( set );
    }

    /**
     * Do the work.
     *
     * @exception TaskException Thrown in unrecoverable error.
     */
    public void execute()
        throws TaskException
    {
        validate();

        if( m_src != null )
        {
            expandFile( m_src, m_dest );
        }

        final int size = m_filesets.size();
        if( size > 0 )
        {
            for( int j = 0; j < size; j++ )
            {
                final FileSet fileSet = (FileSet)m_filesets.get( j );
                final DirectoryScanner scanner = ScannerUtil.getDirectoryScanner( fileSet );
                final File fromDir = fileSet.getDir();

                final String[] files = scanner.getIncludedFiles();
                for( int i = 0; i < files.length; ++i )
                {
                    final File file = new File( fromDir, files[ i ] );
                    expandFile( file, m_dest );
                }
            }
        }
    }

    private void validate()
        throws TaskException
    {
        if( m_src == null && m_filesets.size() == 0 )
        {
            final String message = "src attribute and/or filesets must be specified";
            throw new TaskException( message );
        }

        if( m_dest == null )
        {
            final String message = "Dest attribute must be specified";
            throw new TaskException( message );
        }

        if( m_dest.exists() && !m_dest.isDirectory() )
        {
            final String message = "Dest must be a directory.";
            throw new TaskException( message );
        }

        if( m_src != null && m_src.isDirectory() )
        {
            final String message = "Src must not be a directory." +
                " Use nested filesets instead.";
            throw new TaskException( message );
        }
    }

    /*
     * This method is to be overridden by extending unarchival tasks.
     */
    protected void expandFile( final File src, final File dir )
        throws TaskException
    {
        if( getLogger().isInfoEnabled() )
        {
            final String message = "Expanding: " + src + " into " + dir;
            getLogger().info( message );
        }

        try
        {
            expandArchive( src, dir );
        }
        catch( final IOException ioe )
        {
            final String message = "Error while expanding " + src.getPath();
            throw new TaskException( message, ioe );
        }

        if( getLogger().isDebugEnabled() )
        {
            final String message = "expand complete";
            getLogger().debug( message );
        }
    }

    protected abstract void expandArchive( final File src, final File dir )
        throws IOException, TaskException;

    protected void extractFile( final File dir,
                                final InputStream input,
                                final String entryName,
                                final Date date,
                                final boolean isDirectory )
        throws IOException, TaskException
    {

        final int size = m_patternsets.size();
        if( m_patternsets != null && size > 0 )
        {
            boolean included = false;
            for( int i = 0; i < size; i++ )
            {
                PatternSet p = (PatternSet)m_patternsets.get( i );
                final TaskContext context = getContext();
                String[] incls = PatternUtil.getIncludePatterns( p, context );
                if( incls != null )
                {
                    for( int j = 0; j < incls.length; j++ )
                    {
                        boolean isIncl = ScannerUtil.match( incls[ j ], entryName );
                        if( isIncl )
                        {
                            included = true;
                            break;
                        }
                    }
                }
                final TaskContext context1 = getContext();
                String[] excls = PatternUtil.getExcludePatterns( p, context1 );
                if( excls != null )
                {
                    for( int j = 0; j < excls.length; j++ )
                    {
                        boolean isExcl = ScannerUtil.match( excls[ j ], entryName );
                        if( isExcl )
                        {
                            included = false;
                            break;
                        }
                    }
                }
            }

            if( !included )
            {
                //Do not process this file
                return;
            }
        }

        final File file = FileUtil.resolveFile( dir, entryName );
        try
        {
            if( !m_overwrite && file.exists() &&
                file.lastModified() >= date.getTime() )
            {
                final String message = "Skipping " + file + " as it is up-to-date";
                getLogger().debug( message );
                return;
            }

            getLogger().debug( "expanding " + entryName + " to " + file );

            // create intermediary directories - sometimes zip don't add them
            final File parent = file.getParentFile();
            parent.mkdirs();

            if( isDirectory )
            {
                file.mkdirs();
            }
            else
            {
                FileOutputStream fos = null;
                try
                {
                    fos = new FileOutputStream( file );
                    IOUtil.copy( input, fos );
                }
                finally
                {
                    IOUtil.shutdownStream( fos );
                }
            }

            file.setLastModified( date.getTime() );
        }
        catch( final FileNotFoundException fnfe )
        {
            final String message = "Unable to expand to file " + file.getPath();
            getLogger().warn( message );
        }
    }
}
