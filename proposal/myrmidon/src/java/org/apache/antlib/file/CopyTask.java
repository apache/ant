/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.FileNameMapper;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ScannerUtil;
import org.apache.tools.ant.types.SourceFileScanner;
import org.apache.tools.ant.util.mappers.IdentityMapper;

/**
 * This is a task used to copy files.
 *
 * @ant:task name="copy"
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:glennm@ca.ibm.com">Glenn McAllister</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <A href="gholam@xtra.co.nz">Michael McCallum</A>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 * @version $Revision$ $Date$
 */
public class CopyTask
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( CopyTask.class );

    private File m_file;
    private ArrayList m_filesets = new ArrayList();
    private File m_destFile;
    private File m_destDir;
    private boolean m_preserveLastModified;
    private boolean m_overwrite;
    private boolean m_includeEmpty = true;
    private FileNameMapper m_mapper;

    private HashMap m_fileMap = new HashMap();
    private HashMap m_dirMap = new HashMap();

    /**
     * Sets a single source file to copy.
     */
    public void setFile( final File file )
    {
        m_file = file;
    }

    public void addFileset( final FileSet set )
    {
        m_filesets.add( set );
    }

    public void setDestFile( final File destFile )
    {
        m_destFile = destFile;
    }

    public void setDestDir( final File destDir )
    {
        m_destDir = destDir;
    }

    public void setPreserveLastModified( boolean preserveLastModified )
    {
        m_preserveLastModified = preserveLastModified;
    }

    /**
     * Overwrite any existing destination file(s).
     */
    public void setOverwrite( boolean overwrite )
    {
        m_overwrite = overwrite;
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     */
    public void addMapper( final FileNameMapper mapper )
        throws TaskException
    {
        if( null != m_mapper )
        {
            final String message = "Cannot define more than one mapper";
            throw new TaskException( message );
        }
        m_mapper = mapper;
    }

    protected final boolean isPreserveLastModified()
    {
        return m_preserveLastModified;
    }

    public void execute()
        throws TaskException
    {
        validate();

        // deal with the single file
        if( m_file != null )
        {
            if( null == m_destFile )
            {
                m_destFile = new File( m_destDir, m_file.getName() );
            }

            if( m_overwrite ||
                ( m_file.lastModified() > m_destFile.lastModified() ) )
            {
                m_fileMap.put( m_file.getAbsolutePath(), m_destFile.getAbsolutePath() );
            }
            else
            {
                final String message =
                    REZ.getString( "copy.omit-uptodate.notice", m_file, m_destFile );
                getLogger().debug( message );
            }
        }

        // deal with the filesets
        final int size = m_filesets.size();
        for( int i = 0; i < size; i++ )
        {
            final FileSet fileSet = (FileSet)m_filesets.get( i );
            final DirectoryScanner scanner = ScannerUtil.getDirectoryScanner( fileSet );
            final File fromDir = fileSet.getDir();

            final String[] srcFiles = scanner.getIncludedFiles();
            final String[] srcDirs = scanner.getIncludedDirectories();

            scan( fromDir, m_destDir, srcFiles, srcDirs );
        }

        // do all the copy operations now...
        doFileOperations( m_fileMap, m_dirMap );
    }

    protected void validate()
        throws TaskException
    {
        final int fileSetSize = m_filesets.size();

        if( null == m_file && 0 == fileSetSize )
        {
            final String message = REZ.getString( "copy.missing-src.error" );
            throw new TaskException( message );
        }

        if( null != m_destFile && null != m_destDir )
        {
            final String message = REZ.getString( "copy.one-dest-only.error" );
            throw new TaskException( message );
        }

        if( null != m_file && m_file.exists() && m_file.isDirectory() )
        {
            final String message = REZ.getString( "copy.fileset-for-dirs.error" );
            throw new TaskException( message );
        }

        if( null != m_destFile && fileSetSize > 0 )
        {
            if( fileSetSize > 1 )
            {
                final String message = REZ.getString( "copy.need-destdir.error" );
                throw new TaskException( message );
            }
            else
            {
                final FileSet fileSet = (FileSet)m_filesets.get( 0 );
                final DirectoryScanner scanner = ScannerUtil.getDirectoryScanner( fileSet );
                final String[] srcFiles = scanner.getIncludedFiles();

                if( srcFiles.length > 0 )
                {
                    if( m_file == null )
                    {
                        m_file = new File( srcFiles[ 0 ] );
                        m_filesets.remove( 0 );
                    }
                    else
                    {
                        final String message = REZ.getString( "copy.bad-mapping.error" );
                        throw new TaskException( message );
                    }
                }
                else
                {
                    final String message = REZ.getString( "copy.bad-operation.error" );
                    throw new TaskException( message );
                }
            }
        }

        if( null != m_file && !m_file.exists() )
        {
            final String message =
                REZ.getString( "copy.missing-file.error", m_file.getAbsolutePath() );
            throw new TaskException( message );
        }

        if( null != m_destFile )
        {
            m_destDir = m_destFile.getParentFile();
        }
    }

    /**
     * Compares source files to destination files to see if they should be
     * copied.
     */
    private void scan( final File sourceDir,
                       final File destDir,
                       final String[] files,
                       final String[] dirs )
        throws TaskException
    {
        final FileNameMapper mapper = getFilenameMapper();

        buildMap( sourceDir, destDir, files, mapper, m_fileMap );

        if( m_includeEmpty )
        {
            buildMap( sourceDir, destDir, dirs, mapper, m_dirMap );
        }
    }

    private void buildMap( final File sourceDir,
                           final File destDir,
                           final String[] files,
                           final FileNameMapper mapper,
                           final Map map )
        throws TaskException
    {
        final String[] toCopy = buildFilenameList( files, mapper, sourceDir, destDir );
        for( int i = 0; i < toCopy.length; i++ )
        {
            final String destFilename = mapper.mapFileName( toCopy[ i ], getContext() )[ 0 ];
            final File source = new File( sourceDir, toCopy[ i ] );
            final File destination = new File( destDir, destFilename );
            map.put( source.getAbsolutePath(), destination.getAbsolutePath() );
        }
    }

    /**
     * Utility method to build up a list of files needed between both
     * but only getting the files that need updating (unless overwrite is true).
     */
    private String[] buildFilenameList( final String[] names,
                                        final FileNameMapper mapper,
                                        final File fromDir,
                                        final File toDir )
        throws TaskException
    {
        if( m_overwrite )
        {
            final ArrayList list = new ArrayList( names.length );
            for( int i = 0; i < names.length; i++ )
            {
                final String name = names[ i ];
                if( null != mapper.mapFileName( name, getContext() ) )
                {
                    list.add( name );
                }
            }

            return (String[])list.toArray( new String[ list.size() ] );
        }
        else
        {
            final SourceFileScanner scanner = new SourceFileScanner();
            setupLogger( scanner );
            return scanner.restrict( names, fromDir, toDir, mapper, getContext() );
        }
    }

    /**
     * Perform the oepration on all the files (and possibly empty directorys).
     */
    private void doFileOperations( final Map fileCopyMap, final Map dirCopyMap )
        throws TaskException
    {
        final int fileCount = fileCopyMap.size();
        if( fileCount > 0 )
        {
            doOperationOnFiles( fileCopyMap );
        }

        if( m_includeEmpty )
        {
            doOperationOnDirs( dirCopyMap );
        }
    }

    /**
     * perform operation on files.
     */
    private void doOperationOnFiles( final Map fileMap )
        throws TaskException
    {
        final int fileCount = fileMap.size();
        displayFilecountNotice( fileCount );

        final Iterator names = fileMap.keySet().iterator();
        while( names.hasNext() )
        {
            final String source = (String)names.next();
            final String destination = (String)fileMap.get( source );

            if( source.equals( destination ) )
            {
                final String message =
                    REZ.getString( "copy.selfcopy-ignored.notice", source );
                getLogger().info( message );
                continue;
            }

            try
            {
                final String message =
                    REZ.getString( "copy.filecopy.notice", source, destination );
                getLogger().info( message );

                doOperation( source, destination );
            }
            catch( final IOException ioe )
            {
                final String message =
                    REZ.getString( "copy.filecopy.error", source, destination, ioe );
                throw new TaskException( message, ioe );
            }
        }
    }

    /**
     * perform operation on directories.
     */
    private void doOperationOnDirs( final Map dirMap )
    {
        final Iterator dirs = dirMap.values().iterator();
        int count = 0;
        while( dirs.hasNext() )
        {
            final String pathname = (String)dirs.next();
            final File dir = new File( pathname );
            if( !dir.exists() )
            {
                if( !dir.mkdirs() )
                {
                    final String message =
                        REZ.getString( "copy.dircopy.error", dir.getAbsolutePath() );
                    getLogger().error( message );
                }
                else
                {
                    count++;
                }
            }
        }

        if( count > 0 )
        {
            displayDirCopyNotice( count );
        }
    }

    /**
     * Utility method to determine and retrieve FilenameMapper.
     */
    private FileNameMapper getFilenameMapper()
        throws TaskException
    {
        if( null != m_mapper )
        {
            return m_mapper;
        }
        else
        {
            return new IdentityMapper();
        }
    }

    /**
     * Utility method to perform operation to transform a single source file
     * to a destination.
     */
    protected void doOperation( final String sourceFilename,
                                final String destinationFilename )
        throws IOException
    {
        final File source = new File( sourceFilename );
        final File destination = new File( destinationFilename );

        if( m_overwrite )
        {
            FileUtil.forceDelete( destination );
        }

        FileUtil.copyFile( source, destination );

        if( m_preserveLastModified )
        {
            destination.setLastModified( source.lastModified() );
        }
    }

    /**
     * Utility method to display notice about how many dirs copied.
     */
    private void displayDirCopyNotice( final int count )
    {
        final String message =
            REZ.getString( "copy.dir-count.notice",
                           new Integer( count ),
                           m_destDir.getAbsolutePath() );
        getLogger().info( message );
    }

    /**
     * Utility method to display notice about how many files copied.
     */
    private void displayFilecountNotice( final int count )
    {
        if( getLogger().isInfoEnabled() )
        {
            final String message =
                REZ.getString( "copy.file-count.notice",
                               new Integer( count ),
                               m_destDir.getAbsolutePath() );
            getLogger().info( message );
        }
    }
}
