/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.FlatFileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.SourceFileScanner;

/**
 * A consolidated copy task. Copies a file or directory to a new file or
 * directory. Files are only copied if the source file is newer than the
 * destination file, or when the destination file does not exist. It is possible
 * to explicitly overwrite existing files.</p> <p>
 *
 * This implementation is based on Arnout Kuiper's initial design document, the
 * following mailing list discussions, and the copyfile/copydir tasks.</p>
 *
 * @author Glenn McAllister <a href="mailto:glennm@ca.ibm.com">glennm@ca.ibm.com
 *      </a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <A href="gholam@xtra.co.nz">Michael McCallum</A>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class Copy
    extends Task
{
    private File m_file;// the source file
    private File m_destFile;// the destination file
    private File m_destDir;// the destination directory
    private Vector m_filesets = new Vector();

    private boolean m_filtering;
    private boolean m_preserveLastModified;
    private boolean m_forceOverwrite;
    private boolean m_flatten;
    private int m_verbosity = Project.MSG_VERBOSE;
    private boolean m_includeEmpty = true;

    private Hashtable m_fileCopyMap = new Hashtable();
    private Hashtable m_dirCopyMap = new Hashtable();
    private Hashtable m_completeDirMap = new Hashtable();

    private Mapper m_mapperElement;
    private Vector m_filterSets = new Vector();

    /**
     * Sets a single source file to copy.
     *
     * @param file The new File value
     */
    public void setFile( final File file )
    {
        m_file = file;
    }

    /**
     * Sets filtering.
     *
     * @param filtering The new Filtering value
     */
    public void setFiltering( final boolean filtering )
    {
        m_filtering = filtering;
    }

    /**
     * When copying directory trees, the files can be "flattened" into a single
     * directory. If there are multiple files with the same name in the source
     * directory tree, only the first file will be copied into the "flattened"
     * directory, unless the forceoverwrite attribute is true.
     *
     * @param flatten The new Flatten value
     */
    public void setFlatten( final boolean flatten )
    {
        m_flatten = flatten;
    }

    /**
     * Used to copy empty directories.
     *
     * @param includeEmpty The new IncludeEmptyDirs value
     */
    public void setIncludeEmptyDirs( final boolean includeEmpty )
    {
        m_includeEmpty = includeEmpty;
    }

    /**
     * Overwrite any existing destination file(s).
     *
     * @param overwrite The new Overwrite value
     */
    public void setOverwrite( final boolean overwrite )
    {
        m_forceOverwrite = overwrite;
    }

    /**
     * Give the copied files the same last modified time as the original files.
     *
     * @param preserve The new PreserveLastModified value
     */
    public void setPreserveLastModified( final boolean preserve )
    {
        m_preserveLastModified = preserve;
    }

    /**
     * Sets the destination directory.
     *
     * @param destDir The new Todir value
     */
    public void setTodir( final File destDir )
    {
        m_destDir = destDir;
    }

    /**
     * Sets the destination file.
     *
     * @param destFile The new Tofile value
     */
    public void setTofile( final File destFile )
    {
        m_destFile = destFile;
    }

    /**
     * Used to force listing of all names of copied files.
     *
     * @param verbose The new Verbose value
     */
    public void setVerbose( final boolean verbose )
    {
        if( verbose )
        {
            m_verbosity = Project.MSG_INFO;
        }
        else
        {
            m_verbosity = Project.MSG_VERBOSE;
        }
    }

    /**
     * Adds a set of files (nested fileset attribute).
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( final FileSet set )
    {
        m_filesets.addElement( set );
    }

    /**
     * Create a nested filterset
     *
     * @return Description of the Returned Value
     */
    public FilterSet createFilterSet()
    {
        final FilterSet filterSet = new FilterSet();
        m_filterSets.addElement( filterSet );
        return filterSet;
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     *
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    public Mapper createMapper()
        throws TaskException
    {
        if( m_mapperElement != null )
        {
            throw new TaskException( "Cannot define more than one mapper" );
        }
        m_mapperElement = new Mapper( project );
        return m_mapperElement;
    }

    /**
     * Performs the copy operation.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        // make sure we don't have an illegal set of options
        validateAttributes();

        // deal with the single file
        if( m_file != null )
        {
            if( m_file.exists() )
            {
                if( m_destFile == null )
                {
                    m_destFile = new File( m_destDir, m_file.getName() );
                }

                if( m_forceOverwrite ||
                    ( m_file.lastModified() > m_destFile.lastModified() ) )
                {
                    m_fileCopyMap.put( m_file.getAbsolutePath(), m_destFile.getAbsolutePath() );
                }
                else
                {
                    log( m_file + " omitted as " + m_destFile + " is up to date.",
                         Project.MSG_VERBOSE );
                }
            }
            else
            {
                String message = "Could not find file "
                    + m_file.getAbsolutePath() + " to copy.";
                getLogger().info( message );
                throw new TaskException( message );
            }
        }

        // deal with the filesets
        for( int i = 0; i < m_filesets.size(); i++ )
        {
            final FileSet fileSet = (FileSet)m_filesets.elementAt( i );
            final DirectoryScanner scanner = fileSet.getDirectoryScanner( project );
            final File fromDir = fileSet.getDir( project );

            final String[] srcFiles = scanner.getIncludedFiles();
            final String[] srcDirs = scanner.getIncludedDirectories();
            final boolean isEverythingIncluded = scanner.isEverythingIncluded();

            if( isEverythingIncluded && !m_flatten && null == m_mapperElement )
            {
                m_completeDirMap.put( fromDir, m_destDir );
            }

            scan( fromDir, m_destDir, srcFiles, srcDirs );
        }

        // do all the copy operations now...
        doFileOperations();

        // clean up destDir again - so this instance can be used a second
        // time without throwing an exception
        if( null != m_destFile )
        {
            m_destDir = null;
        }
    }

    /**
     * Get the filtersets being applied to this operation.
     *
     * @return a vector of FilterSet objects
     */
    protected Vector getFilterSets()
    {
        return m_filterSets;
    }

    protected void buildMap( final File fromDir,
                             final File toDir,
                             final String[] names,
                             final FileNameMapper mapper,
                             final Hashtable map )
        throws TaskException
    {
        final String[] toCopy = buildFilenameList( names, mapper, fromDir, toDir );
        for( int i = 0; i < toCopy.length; i++ )
        {
            final String destFilename = mapper.mapFileName( toCopy[ i ] )[ 0 ];

            final File src = new File( fromDir, toCopy[ i ] );
            final File dest = new File( toDir, destFilename );
            map.put( src.getAbsolutePath(), dest.getAbsolutePath() );
        }
    }

    private String[] buildFilenameList( final String[] names,
                                        final FileNameMapper mapper,
                                        final File fromDir,
                                        final File toDir )
        throws TaskException
    {
        if( m_forceOverwrite )
        {
            final ArrayList list = new ArrayList( names.length );
            for( int i = 0; i < names.length; i++ )
            {
                final String name = names[ i ];
                if( null != mapper.mapFileName( name ) )
                {
                    list.add( name );
                }
            }

            return (String[])list.toArray( new String[ list.size() ] );
        }
        else
        {
            final SourceFileScanner scanner = new SourceFileScanner( this );
            return scanner.restrict( names, fromDir, toDir, mapper );
        }
    }

    /**
     * Actually does the file (and possibly empty directory) copies. This is a
     * good method for subclasses to override.
     */
    protected void doFileOperations()
        throws TaskException
    {
        if( m_fileCopyMap.size() > 0 )
        {
            getLogger().info( "Copying " + m_fileCopyMap.size() +
                              " file" + ( m_fileCopyMap.size() == 1 ? "" : "s" ) +
                              " to " + m_destDir.getAbsolutePath() );

            Enumeration e = m_fileCopyMap.keys();
            while( e.hasMoreElements() )
            {
                String fromFile = (String)e.nextElement();
                String toFile = (String)m_fileCopyMap.get( fromFile );

                if( fromFile.equals( toFile ) )
                {
                    getLogger().info( "Skipping self-copy of " + fromFile );
                    continue;
                }

                try
                {
                    getLogger().info( "Copying " + fromFile + " to " + toFile );

                    final FilterSetCollection executionFilters = buildFilterSet();
                    final File src = new File( fromFile );
                    final File dest = new File( toFile );

                    if( m_forceOverwrite )
                    {
                        FileUtil.forceDelete( dest );
                    }

                    FileUtils.newFileUtils().copyFile( src, dest, executionFilters );

                    if( m_preserveLastModified )
                    {
                        dest.setLastModified( src.lastModified() );
                    }
                }
                catch( final IOException ioe )
                {
                    final String msg = "Failed to copy " + fromFile + " to " +
                        toFile + " due to " + ioe.getMessage();
                    throw new TaskException( msg, ioe );
                }
            }
        }

        if( m_includeEmpty )
        {
            Enumeration e = m_dirCopyMap.elements();
            int count = 0;
            while( e.hasMoreElements() )
            {
                File d = new File( (String)e.nextElement() );
                if( !d.exists() )
                {
                    if( !d.mkdirs() )
                    {
                        log( "Unable to create directory " + d.getAbsolutePath(), Project.MSG_ERR );
                    }
                    else
                    {
                        count++;
                    }
                }
            }

            if( count > 0 )
            {
                getLogger().info( "Copied " + count +
                                  " empty director" +
                                  ( count == 1 ? "y" : "ies" ) +
                                  " to " + m_destDir.getAbsolutePath() );
            }
        }
    }

    private FilterSetCollection buildFilterSet()
    {
        final FilterSetCollection executionFilters = new FilterSetCollection();
        if( m_filtering )
        {
            executionFilters.addFilterSet( project.getGlobalFilterSet() );
        }

        for( final Enumeration filterEnum = m_filterSets.elements(); filterEnum.hasMoreElements(); )
        {
            executionFilters.addFilterSet( (FilterSet)filterEnum.nextElement() );
        }
        return executionFilters;
    }

    /**
     * Compares source files to destination files to see if they should be
     * copied.
     *
     * @param fromDir Description of Parameter
     * @param toDir Description of Parameter
     * @param files Description of Parameter
     * @param dirs Description of Parameter
     */
    protected void scan( File fromDir, File toDir, String[] files, String[] dirs )
        throws TaskException
    {
        FileNameMapper mapper = null;
        if( m_mapperElement != null )
        {
            mapper = m_mapperElement.getImplementation();
        }
        else if( m_flatten )
        {
            mapper = new FlatFileNameMapper();
        }
        else
        {
            mapper = new IdentityMapper();
        }

        buildMap( fromDir, toDir, files, mapper, m_fileCopyMap );

        if( m_includeEmpty )
        {
            buildMap( fromDir, toDir, dirs, mapper, m_dirCopyMap );
        }
    }

    //************************************************************************
    //  protected and private methods
    //************************************************************************

    /**
     * Ensure we have a consistent and legal set of attributes, and set any
     * internal flags necessary based on different combinations of attributes.
     *
     * @exception TaskException Description of Exception
     */
    protected void validateAttributes()
        throws TaskException
    {
        if( m_file == null && m_filesets.size() == 0 )
        {
            throw new TaskException( "Specify at least one source - a file or a fileset." );
        }

        if( m_destFile != null && m_destDir != null )
        {
            throw new TaskException( "Only one of tofile and todir may be set." );
        }

        if( m_destFile == null && m_destDir == null )
        {
            throw new TaskException( "One of tofile or todir must be set." );
        }

        if( m_file != null && m_file.exists() && m_file.isDirectory() )
        {
            throw new TaskException( "Use a fileset to copy directories." );
        }

        if( m_destFile != null && m_filesets.size() > 0 )
        {
            if( m_filesets.size() > 1 )
            {
                throw new TaskException(
                    "Cannot concatenate multiple files into a single file." );
            }
            else
            {
                FileSet fs = (FileSet)m_filesets.elementAt( 0 );
                DirectoryScanner ds = fs.getDirectoryScanner( project );
                String[] srcFiles = ds.getIncludedFiles();

                if( srcFiles.length > 0 )
                {
                    if( m_file == null )
                    {
                        m_file = new File( srcFiles[ 0 ] );
                        m_filesets.removeElementAt( 0 );
                    }
                    else
                    {
                        throw new TaskException(
                            "Cannot concatenate multiple files into a single file." );
                    }
                }
                else
                {
                    throw new TaskException(
                        "Cannot perform operation from directory to file." );
                }
            }
        }

        if( m_destFile != null )
        {
            m_destDir = new File( m_destFile.getParent() );// be 1.1 friendly
        }
    }

    protected Vector getFilesets()
    {
        return m_filesets;
    }

    protected boolean isFiltering()
    {
        return m_filtering;
    }

    protected boolean isForceOverwrite()
    {
        return m_forceOverwrite;
    }

    protected int getVerbosity()
    {
        return m_verbosity;
    }

    protected boolean isIncludeEmpty()
    {
        return m_includeEmpty;
    }

    protected Hashtable getFileCopyMap()
    {
        return m_fileCopyMap;
    }

    protected Hashtable getDirCopyMap()
    {
        return m_dirCopyMap;
    }

    protected Hashtable getCompleteDirMap()
    {
        return m_completeDirMap;
    }

    protected File getDestDir()
    {
        return m_destDir;
    }

    protected void setForceOverwrite( final boolean forceOverwrite )
    {
        m_forceOverwrite = forceOverwrite;
    }
}
