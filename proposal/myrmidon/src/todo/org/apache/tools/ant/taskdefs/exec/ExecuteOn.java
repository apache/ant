/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.exec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Marker;
import org.apache.tools.ant.types.SourceFileScanner;
import org.apache.tools.ant.util.mappers.FileNameMapper;
import org.apache.tools.ant.util.mappers.Mapper;

/**
 * Executes a given command, supplying a set of files as arguments.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:mariusz@rakiura.org">Mariusz Nowostawski</a>
 */
public class ExecuteOn
    extends ExecTask
{
    private ArrayList m_filesets = new ArrayList();
    private boolean m_relative;
    private boolean m_parallel;
    private String m_type = "file";
    private Marker m_srcFilePos;
    private boolean m_skipEmpty;
    private Marker m_targetFilePos;
    private Mapper m_mapperElement;
    private FileNameMapper m_mapper;
    private File m_destDir

    /**
     * Has &lt;srcfile&gt; been specified before &lt;targetfile&gt;
     */
    private boolean m_srcIsFirst = true;

    /**
     * Set the destination directory.
     */
    public void setDest( final File destDir )
    {
        m_destDir = destDir;
    }

    /**
     * Shall the command work on all specified files in parallel?
     */
    public void setParallel( final boolean parallel )
    {
        m_parallel = parallel;
    }

    /**
     * Should filenames be returned as relative path names?
     */
    public void setRelative( final boolean relative )
    {
        m_relative = relative;
    }

    /**
     * Should empty filesets be ignored?
     */
    public void setSkipEmptyFilesets( final boolean skip )
    {
        m_skipEmpty = skip;
    }

    /**
     * Shall the command work only on files, directories or both?
     */
    public void setType( final FileDirBoth type )
    {
        m_type = type.getValue();
    }

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset( final FileSet set )
    {
        m_filesets.add( set );
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     */
    public Mapper createMapper()
        throws TaskException
    {
        if( m_mapperElement != null )
        {
            throw new TaskException( "Cannot define more than one mapper" );
        }
        m_mapperElement = new Mapper();
        return m_mapperElement;
    }

    /**
     * Marker that indicates where the name of the source file should be put on
     * the command line.
     */
    public Marker createSrcfile()
        throws TaskException
    {
        if( m_srcFilePos != null )
        {
            throw new TaskException( getName() + " doesn\'t support multiple srcfile elements." );
        }
        m_srcFilePos = getCommand().createMarker();
        return m_srcFilePos;
    }

    /**
     * Marker that indicates where the name of the target file should be put on
     * the command line.
     */
    public Marker createTargetfile()
        throws TaskException
    {
        if( m_targetFilePos != null )
        {
            throw new TaskException( getName() + " doesn\'t support multiple targetfile elements." );
        }
        m_targetFilePos = getCommand().createMarker();
        m_srcIsFirst = ( m_srcFilePos != null );
        return m_targetFilePos;
    }

    /**
     * Construct the command line for parallel execution.
     *
     * @param srcFiles The filenames to add to the commandline
     */
    protected String[] getCommandline( final String[] srcFiles,
                                       final File[] baseDirs )
        throws TaskException
    {
        final ArrayList targets = new ArrayList();
        if( m_targetFilePos != null )
        {
            Hashtable addedFiles = new Hashtable();
            for( int i = 0; i < srcFiles.length; i++ )
            {
                String[] subTargets = m_mapper.mapFileName( srcFiles[ i ] );
                if( subTargets != null )
                {
                    for( int j = 0; j < subTargets.length; j++ )
                    {
                        String name = null;
                        if( !m_relative )
                        {
                            name =
                                ( new File( m_destDir, subTargets[ j ] ) ).getAbsolutePath();
                        }
                        else
                        {
                            name = subTargets[ j ];
                        }
                        if( !addedFiles.contains( name ) )
                        {
                            targets.add( name );
                            addedFiles.put( name, name );
                        }
                    }
                }
            }
        }
        String[] targetFiles = new String[ targets.size() ];
        targetFiles = (String[])targets.toArray( targetFiles );

        String[] orig = getCommand().getCommandline();
        String[] result = new String[ orig.length + srcFiles.length + targetFiles.length ];

        int srcIndex = orig.length;
        if( m_srcFilePos != null )
        {
            srcIndex = m_srcFilePos.getPosition();
        }

        if( m_targetFilePos != null )
        {
            int targetIndex = m_targetFilePos.getPosition();

            if( srcIndex < targetIndex
                || ( srcIndex == targetIndex && m_srcIsFirst ) )
            {

                // 0 --> srcIndex
                System.arraycopy( orig, 0, result, 0, srcIndex );

                // srcIndex --> targetIndex
                System.arraycopy( orig, srcIndex, result,
                                  srcIndex + srcFiles.length,
                                  targetIndex - srcIndex );

                // targets are already absolute file names
                System.arraycopy( targetFiles, 0, result,
                                  targetIndex + srcFiles.length,
                                  targetFiles.length );

                // targetIndex --> end
                System.arraycopy( orig, targetIndex, result,
                                  targetIndex + srcFiles.length + targetFiles.length,
                                  orig.length - targetIndex );
            }
            else
            {
                // 0 --> targetIndex
                System.arraycopy( orig, 0, result, 0, targetIndex );

                // targets are already absolute file names
                System.arraycopy( targetFiles, 0, result,
                                  targetIndex,
                                  targetFiles.length );

                // targetIndex --> srcIndex
                System.arraycopy( orig, targetIndex, result,
                                  targetIndex + targetFiles.length,
                                  srcIndex - targetIndex );

                // srcIndex --> end
                System.arraycopy( orig, srcIndex, result,
                                  srcIndex + srcFiles.length + targetFiles.length,
                                  orig.length - srcIndex );
                srcIndex += targetFiles.length;
            }

        }
        else
        {// no targetFilePos

            // 0 --> srcIndex
            System.arraycopy( orig, 0, result, 0, srcIndex );
            // srcIndex --> end
            System.arraycopy( orig, srcIndex, result,
                              srcIndex + srcFiles.length,
                              orig.length - srcIndex );

        }

        // fill in source file names
        for( int i = 0; i < srcFiles.length; i++ )
        {
            if( !m_relative )
            {
                result[ srcIndex + i ] =
                    ( new File( baseDirs[ i ], srcFiles[ i ] ) ).getAbsolutePath();
            }
            else
            {
                result[ srcIndex + i ] = srcFiles[ i ];
            }
        }
        return result;
    }

    /**
     * Construct the command line for serial execution.
     *
     * @param srcFile The filename to add to the commandline
     * @param baseDir filename is relative to this dir
     * @return The Commandline value
     */
    protected String[] getCommandline( final String srcFile,
                                       final File baseDir )
        throws TaskException
    {
        return getCommandline( new String[]{srcFile}, new File[]{baseDir} );
    }

    /**
     * Return the list of Directories from this DirectoryScanner that should be
     * included on the command line.
     *
     * @param baseDir Description of Parameter
     * @param ds Description of Parameter
     * @return The Dirs value
     */
    protected String[] getDirs( final File baseDir,
                                final DirectoryScanner ds )
        throws TaskException
    {
        if( m_mapper != null )
        {
            final SourceFileScanner scanner = new SourceFileScanner();
            setupLogger( scanner );
            return scanner.restrict( ds.getIncludedDirectories(), baseDir, m_destDir,
                                     m_mapper );
        }
        else
        {
            return ds.getIncludedDirectories();
        }
    }

    /**
     * Return the list of files from this DirectoryScanner that should be
     * included on the command line.
     *
     * @param baseDir Description of Parameter
     * @param ds Description of Parameter
     * @return The Files value
     */
    protected String[] getFiles( final File baseDir,
                                 final DirectoryScanner ds )
        throws TaskException
    {
        if( m_mapper != null )
        {
            final SourceFileScanner scanner = new SourceFileScanner();
            setupLogger( scanner );
            return scanner.restrict( ds.getIncludedFiles(), baseDir, m_destDir,
                                     m_mapper );
        }
        else
        {
            return ds.getIncludedFiles();
        }
    }

    protected void validate()
        throws TaskException
    {
        super.validate();
        if( m_filesets.size() == 0 )
        {
            final String message = "no filesets specified";
            throw new TaskException( message );
        }

        if( m_targetFilePos != null ||
            m_mapperElement != null ||
            m_destDir != null )
        {
            if( m_mapperElement == null )
            {
                final String message = "no mapper specified";
                throw new TaskException( message );
            }
            if( m_mapperElement == null )
            {
                final String message = "no dest attribute specified";
                throw new TaskException( message );
            }
            m_mapper = m_mapperElement.getImplementation();
        }
    }

    protected void runExec( final Execute exe )
        throws TaskException
    {
        try
        {
            final ArrayList fileNames = new ArrayList();
            final ArrayList baseDirs = new ArrayList();
            for( int i = 0; i < m_filesets.size(); i++ )
            {
                final FileSet fs = (FileSet)m_filesets.get( i );
                final File base = fs.getDir();
                final DirectoryScanner ds = fs.getDirectoryScanner();

                if( !"dir".equals( m_type ) )
                {
                    final String[] s = getFiles( base, ds );
                    for( int j = 0; j < s.length; j++ )
                    {
                        fileNames.add( s[ j ] );
                        baseDirs.add( base );
                    }
                }

                if( !"file".equals( m_type ) )
                {
                    final String[] s = getDirs( base, ds );
                    for( int j = 0; j < s.length; j++ )
                    {
                        fileNames.add( s[ j ] );
                        baseDirs.add( base );
                    }
                }

                if( fileNames.size() == 0 && m_skipEmpty )
                {
                    getLogger().info( "Skipping fileset for directory " + base + ". It is empty." );
                    continue;
                }

                if( !m_parallel )
                {
                    final String[] s = new String[ fileNames.size() ];
                    s = (String[])fileNames.toArray( s );
                    for( int j = 0; j < s.length; j++ )
                    {
                        String[] command = getCommandline( s[ j ], base );
                        getLogger().debug( "Executing " + StringUtil.join( command, " " ) );
                        exe.setCommandline( command );
                        runExecute( exe );
                    }
                    fileNames.clear();
                    baseDirs.clear();
                }
            }

            if( m_parallel && ( fileNames.size() > 0 || !m_skipEmpty ) )
            {
                String[] s = new String[ fileNames.size() ];
                s = (String[])fileNames.toArray( s );
                File[] b = new File[ baseDirs.size() ];
                b = (File[])baseDirs.toArray( b );
                String[] command = getCommandline( s, b );
                getLogger().debug( "Executing " + StringUtil.join( command, " " ) );
                exe.setCommandline( command );
                runExecute( exe );
            }

        }
        catch( IOException e )
        {
            throw new TaskException( "Execute failed: " + e, e );
        }
        finally
        {
            // close the output file if required
            logFlush();
        }
    }
}
