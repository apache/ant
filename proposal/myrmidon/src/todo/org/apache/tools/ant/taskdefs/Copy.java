/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
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
public class Copy extends Task
{
    protected File file = null;// the source file
    protected File destFile = null;// the destination file
    protected File destDir = null;// the destination directory
    protected Vector filesets = new Vector();

    protected boolean filtering = false;
    protected boolean preserveLastModified = false;
    protected boolean forceOverwrite = false;
    protected boolean flatten = false;
    protected int verbosity = Project.MSG_VERBOSE;
    protected boolean includeEmpty = true;

    protected Hashtable fileCopyMap = new Hashtable();
    protected Hashtable dirCopyMap = new Hashtable();
    protected Hashtable completeDirMap = new Hashtable();

    protected Mapper mapperElement = null;
    private Vector filterSets = new Vector();
    private FileUtils fileUtils;

    public Copy()
    {
        fileUtils = FileUtils.newFileUtils();
    }

    /**
     * Sets a single source file to copy.
     *
     * @param file The new File value
     */
    public void setFile( File file )
    {
        this.file = file;
    }

    /**
     * Sets filtering.
     *
     * @param filtering The new Filtering value
     */
    public void setFiltering( boolean filtering )
    {
        this.filtering = filtering;
    }

    /**
     * When copying directory trees, the files can be "flattened" into a single
     * directory. If there are multiple files with the same name in the source
     * directory tree, only the first file will be copied into the "flattened"
     * directory, unless the forceoverwrite attribute is true.
     *
     * @param flatten The new Flatten value
     */
    public void setFlatten( boolean flatten )
    {
        this.flatten = flatten;
    }

    /**
     * Used to copy empty directories.
     *
     * @param includeEmpty The new IncludeEmptyDirs value
     */
    public void setIncludeEmptyDirs( boolean includeEmpty )
    {
        this.includeEmpty = includeEmpty;
    }

    /**
     * Overwrite any existing destination file(s).
     *
     * @param overwrite The new Overwrite value
     */
    public void setOverwrite( boolean overwrite )
    {
        this.forceOverwrite = overwrite;
    }

    /**
     * Give the copied files the same last modified time as the original files.
     *
     * @param preserve The new PreserveLastModified value
     */
    public void setPreserveLastModified( boolean preserve )
    {
        preserveLastModified = preserve;
    }

    /**
     * Sets the destination directory.
     *
     * @param destDir The new Todir value
     */
    public void setTodir( File destDir )
    {
        this.destDir = destDir;
    }

    /**
     * Sets the destination file.
     *
     * @param destFile The new Tofile value
     */
    public void setTofile( File destFile )
    {
        this.destFile = destFile;
    }

    /**
     * Used to force listing of all names of copied files.
     *
     * @param verbose The new Verbose value
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
     * Create a nested filterset
     *
     * @return Description of the Returned Value
     */
    public FilterSet createFilterSet()
    {
        FilterSet filterSet = new FilterSet();
        filterSets.addElement( filterSet );
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
        if( mapperElement != null )
        {
            throw new TaskException( "Cannot define more than one mapper" );
        }
        mapperElement = new Mapper( project );
        return mapperElement;
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
        if( file != null )
        {
            if( file.exists() )
            {
                if( destFile == null )
                {
                    destFile = new File( destDir, file.getName() );
                }

                if( forceOverwrite ||
                    ( file.lastModified() > destFile.lastModified() ) )
                {
                    fileCopyMap.put( file.getAbsolutePath(), destFile.getAbsolutePath() );
                }
                else
                {
                    log( file + " omitted as " + destFile + " is up to date.",
                         Project.MSG_VERBOSE );
                }
            }
            else
            {
                String message = "Could not find file "
                    + file.getAbsolutePath() + " to copy.";
                getLogger().info( message );
                throw new TaskException( message );
            }
        }

        // deal with the filesets
        for( int i = 0; i < filesets.size(); i++ )
        {
            FileSet fs = (FileSet)filesets.elementAt( i );
            DirectoryScanner ds = fs.getDirectoryScanner( project );
            File fromDir = fs.getDir( project );

            String[] srcFiles = ds.getIncludedFiles();
            String[] srcDirs = ds.getIncludedDirectories();
            boolean isEverythingIncluded = ds.isEverythingIncluded();
            if( isEverythingIncluded
                && !flatten && mapperElement == null )
            {
                completeDirMap.put( fromDir, destDir );
            }
            scan( fromDir, destDir, srcFiles, srcDirs );
        }

        // do all the copy operations now...
        doFileOperations();

        // clean up destDir again - so this instance can be used a second
        // time without throwing an exception
        if( destFile != null )
        {
            destDir = null;
        }
    }

    protected FileUtils getFileUtils()
    {
        return fileUtils;
    }

    /**
     * Get the filtersets being applied to this operation.
     *
     * @return a vector of FilterSet objects
     */
    protected Vector getFilterSets()
    {
        return filterSets;
    }

    protected void buildMap( File fromDir, File toDir, String[] names,
                             FileNameMapper mapper, Hashtable map )
        throws TaskException
    {

        String[] toCopy = null;
        if( forceOverwrite )
        {
            Vector v = new Vector();
            for( int i = 0; i < names.length; i++ )
            {
                if( mapper.mapFileName( names[ i ] ) != null )
                {
                    v.addElement( names[ i ] );
                }
            }
            toCopy = new String[ v.size() ];
            v.copyInto( toCopy );
        }
        else
        {
            SourceFileScanner ds = new SourceFileScanner( this );
            toCopy = ds.restrict( names, fromDir, toDir, mapper );
        }

        for( int i = 0; i < toCopy.length; i++ )
        {
            File src = new File( fromDir, toCopy[ i ] );
            File dest = new File( toDir, mapper.mapFileName( toCopy[ i ] )[ 0 ] );
            map.put( src.getAbsolutePath(), dest.getAbsolutePath() );
        }
    }

    /**
     * Actually does the file (and possibly empty directory) copies. This is a
     * good method for subclasses to override.
     */
    protected void doFileOperations()
        throws TaskException
    {
        if( fileCopyMap.size() > 0 )
        {
            getLogger().info( "Copying " + fileCopyMap.size() +
                 " file" + ( fileCopyMap.size() == 1 ? "" : "s" ) +
                 " to " + destDir.getAbsolutePath() );

            Enumeration e = fileCopyMap.keys();
            while( e.hasMoreElements() )
            {
                String fromFile = (String)e.nextElement();
                String toFile = (String)fileCopyMap.get( fromFile );

                if( fromFile.equals( toFile ) )
                {
                    log( "Skipping self-copy of " + fromFile, verbosity );
                    continue;
                }

                try
                {
                    log( "Copying " + fromFile + " to " + toFile, verbosity );

                    FilterSetCollection executionFilters = new FilterSetCollection();
                    if( filtering )
                    {
                        executionFilters.addFilterSet( project.getGlobalFilterSet() );
                    }
                    for( Enumeration filterEnum = filterSets.elements(); filterEnum.hasMoreElements(); )
                    {
                        executionFilters.addFilterSet( (FilterSet)filterEnum.nextElement() );
                    }
                    fileUtils.copyFile( fromFile, toFile, executionFilters,
                                        forceOverwrite, preserveLastModified );
                }
                catch( IOException ioe )
                {
                    String msg = "Failed to copy " + fromFile + " to " + toFile
                        + " due to " + ioe.getMessage();
                    throw new TaskException( msg, ioe );
                }
            }
        }

        if( includeEmpty )
        {
            Enumeration e = dirCopyMap.elements();
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
                     " to " + destDir.getAbsolutePath() );
            }
        }
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
        if( mapperElement != null )
        {
            mapper = mapperElement.getImplementation();
        }
        else if( flatten )
        {
            mapper = new FlatFileNameMapper();
        }
        else
        {
            mapper = new IdentityMapper();
        }

        buildMap( fromDir, toDir, files, mapper, fileCopyMap );

        if( includeEmpty )
        {
            buildMap( fromDir, toDir, dirs, mapper, dirCopyMap );
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
        if( file == null && filesets.size() == 0 )
        {
            throw new TaskException( "Specify at least one source - a file or a fileset." );
        }

        if( destFile != null && destDir != null )
        {
            throw new TaskException( "Only one of tofile and todir may be set." );
        }

        if( destFile == null && destDir == null )
        {
            throw new TaskException( "One of tofile or todir must be set." );
        }

        if( file != null && file.exists() && file.isDirectory() )
        {
            throw new TaskException( "Use a fileset to copy directories." );
        }

        if( destFile != null && filesets.size() > 0 )
        {
            if( filesets.size() > 1 )
            {
                throw new TaskException(
                    "Cannot concatenate multiple files into a single file." );
            }
            else
            {
                FileSet fs = (FileSet)filesets.elementAt( 0 );
                DirectoryScanner ds = fs.getDirectoryScanner( project );
                String[] srcFiles = ds.getIncludedFiles();

                if( srcFiles.length > 0 )
                {
                    if( file == null )
                    {
                        file = new File( srcFiles[ 0 ] );
                        filesets.removeElementAt( 0 );
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

        if( destFile != null )
        {
            destDir = new File( destFile.getParent() );// be 1.1 friendly
        }
    }
}
