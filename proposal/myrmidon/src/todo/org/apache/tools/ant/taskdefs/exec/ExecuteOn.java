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
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.mappers.Mapper;
import org.apache.tools.ant.types.Marker;
import org.apache.tools.ant.util.mappers.FileNameMapper;
import org.apache.tools.ant.types.SourceFileScanner;
import org.apache.avalon.excalibur.util.StringUtil;

/**
 * Executes a given command, supplying a set of files as arguments.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:mariusz@rakiura.org">Mariusz Nowostawski</a>
 */
public class ExecuteOn extends ExecTask
{
    protected ArrayList filesets = new ArrayList();
    private boolean relative = false;
    private boolean parallel = false;
    protected String type = "file";
    protected Marker srcFilePos = null;
    private boolean skipEmpty = false;
    protected Marker targetFilePos = null;
    protected Mapper mapperElement = null;
    protected FileNameMapper mapper = null;
    protected File destDir = null;

    /**
     * Has &lt;srcfile&gt; been specified before &lt;targetfile&gt;
     */
    protected boolean srcIsFirst = true;

    /**
     * Set the destination directory.
     *
     * @param destDir The new Dest value
     */
    public void setDest( File destDir )
    {
        this.destDir = destDir;
    }

    /**
     * Shall the command work on all specified files in parallel?
     *
     * @param parallel The new Parallel value
     */
    public void setParallel( boolean parallel )
    {
        this.parallel = parallel;
    }

    /**
     * Should filenames be returned as relative path names?
     *
     * @param relative The new Relative value
     */
    public void setRelative( boolean relative )
    {
        this.relative = relative;
    }

    /**
     * Should empty filesets be ignored?
     *
     * @param skip The new SkipEmptyFilesets value
     */
    public void setSkipEmptyFilesets( boolean skip )
    {
        skipEmpty = skip;
    }

    /**
     * Shall the command work only on files, directories or both?
     *
     * @param type The new Type value
     */
    public void setType( FileDirBoth type )
    {
        this.type = type.getValue();
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
        mapperElement = new Mapper();
        return mapperElement;
    }

    /**
     * Marker that indicates where the name of the source file should be put on
     * the command line.
     *
     * @return Description of the Returned Value
     */
    public Marker createSrcfile()
        throws TaskException
    {
        if( srcFilePos != null )
        {
            throw new TaskException( getName() + " doesn\'t support multiple srcfile elements." );
        }
        srcFilePos = getCommand().createMarker();
        return srcFilePos;
    }

    /**
     * Marker that indicates where the name of the target file should be put on
     * the command line.
     *
     * @return Description of the Returned Value
     */
    public Marker createTargetfile()
        throws TaskException
    {
        if( targetFilePos != null )
        {
            throw new TaskException( getName() + " doesn\'t support multiple targetfile elements." );
        }
        targetFilePos = getCommand().createMarker();
        srcIsFirst = ( srcFilePos != null );
        return targetFilePos;
    }

    /**
     * Construct the command line for parallel execution.
     *
     * @param srcFiles The filenames to add to the commandline
     * @param baseDirs Description of Parameter
     * @return The Commandline value
     */
    protected String[] getCommandline( String[] srcFiles, File[] baseDirs )
        throws TaskException
    {
        ArrayList targets = new ArrayList();
        if( targetFilePos != null )
        {
            Hashtable addedFiles = new Hashtable();
            for( int i = 0; i < srcFiles.length; i++ )
            {
                String[] subTargets = mapper.mapFileName( srcFiles[ i ] );
                if( subTargets != null )
                {
                    for( int j = 0; j < subTargets.length; j++ )
                    {
                        String name = null;
                        if( !relative )
                        {
                            name =
                                ( new File( destDir, subTargets[ j ] ) ).getAbsolutePath();
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
        if( srcFilePos != null )
        {
            srcIndex = srcFilePos.getPosition();
        }

        if( targetFilePos != null )
        {
            int targetIndex = targetFilePos.getPosition();

            if( srcIndex < targetIndex
                || ( srcIndex == targetIndex && srcIsFirst ) )
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
            if( !relative )
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
    protected String[] getCommandline( String srcFile, File baseDir )
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
    protected String[] getDirs( File baseDir, DirectoryScanner ds )
        throws TaskException
    {
        if( mapper != null )
        {
            final SourceFileScanner scanner = new SourceFileScanner();
            setupLogger( scanner );
            return scanner.restrict( ds.getIncludedDirectories(), baseDir, destDir,
                                 mapper );
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
    protected String[] getFiles( File baseDir, DirectoryScanner ds )
        throws TaskException
    {
        if( mapper != null )
        {
            final SourceFileScanner scanner = new SourceFileScanner();
            setupLogger( scanner );
            return scanner.restrict( ds.getIncludedFiles(), baseDir, destDir,
                                 mapper );
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
        if( filesets.size() == 0 )
        {
            throw new TaskException( "no filesets specified" );
        }

        if( targetFilePos != null || mapperElement != null
            || destDir != null )
        {

            if( mapperElement == null )
            {
                throw new TaskException( "no mapper specified" );
            }
            if( mapperElement == null )
            {
                throw new TaskException( "no dest attribute specified" );
            }
            mapper = mapperElement.getImplementation();
        }
    }

    protected void runExec( Execute exe )
        throws TaskException
    {
        try
        {

            ArrayList fileNames = new ArrayList();
            ArrayList baseDirs = new ArrayList();
            for( int i = 0; i < filesets.size(); i++ )
            {
                FileSet fs = (FileSet)filesets.get( i );
                File base = fs.getDir();
                DirectoryScanner ds = fs.getDirectoryScanner();

                if( !"dir".equals( type ) )
                {
                    String[] s = getFiles( base, ds );
                    for( int j = 0; j < s.length; j++ )
                    {
                        fileNames.add( s[ j ] );
                        baseDirs.add( base );
                    }
                }

                if( !"file".equals( type ) )
                {
                    String[] s = getDirs( base, ds );
                    ;
                    for( int j = 0; j < s.length; j++ )
                    {
                        fileNames.add( s[ j ] );
                        baseDirs.add( base );
                    }
                }

                if( fileNames.size() == 0 && skipEmpty )
                {
                    getLogger().info( "Skipping fileset for directory " + base + ". It is empty." );
                    continue;
                }

                if( !parallel )
                {
                    String[] s = new String[ fileNames.size() ];
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

            if( parallel && ( fileNames.size() > 0 || !skipEmpty ) )
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

    /**
     * Enumerated attribute with the values "file", "dir" and "both" for the
     * type attribute.
     *
     * @author RT
     */
    public static class FileDirBoth extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"file", "dir", "both"};
        }
    }
}
