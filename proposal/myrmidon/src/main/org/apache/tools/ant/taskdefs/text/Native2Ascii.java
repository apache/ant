/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.text;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.SourceFileScanner;
import org.apache.tools.ant.util.mappers.FileNameMapper;
import org.apache.tools.ant.util.mappers.IdentityMapper;

/**
 * Convert files from native encodings to ascii.
 *
 * @author <a href="asudell@acm.org">Drew Sudell</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Native2Ascii
    extends MatchingTask
{
    private boolean m_reverse;// convert from ascii back to native
    private String m_encoding;// encoding to convert to/from
    private File m_srcDir;// Where to find input files
    private File m_destDir;// Where to put output files
    private String m_ext;// Extension of output files if different
    private Mapper m_mapper;

    /**
     * Set the destination dirctory to place converted files into.
     *
     * @param destDir directory to place output file into.
     */
    public void setDest( final File destDir )
    {
        m_destDir = destDir;
    }

    /**
     * Set the encoding to translate to/from. If unset, the default encoding for
     * the JVM is used.
     *
     * @param encoding String containing the name of the Native encoding to
     *      convert from or to.
     */
    public void setEncoding( final String encoding )
    {
        m_encoding = encoding;
    }

    /**
     * Set the extension which converted files should have. If unset, files will
     * not be renamed.
     *
     * @param ext File extension to use for converted files.
     */
    public void setExt( final String ext )
    {
        m_ext = ext;
    }

    /**
     * Flag the conversion to run in the reverse sense, that is Ascii to Native
     * encoding.
     *
     * @param reverse True if the conversion is to be reversed, otherwise false;
     */
    public void setReverse( boolean reverse )
    {
        m_reverse = reverse;
    }

    /**
     * Set the source directory in which to find files to convert.
     *
     * @param srcDir Direcrory to find input file in.
     */
    public void setSrc( final File srcDir )
    {
        m_srcDir = srcDir;
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
        if( m_mapper != null )
        {
            throw new TaskException( "Cannot define more than one mapper" );
        }
        m_mapper = new Mapper();
        return m_mapper;
    }

    public void execute()
        throws TaskException
    {
        validate();

        final DirectoryScanner scanner = getDirectoryScanner( m_srcDir );
        String[] files = scanner.getIncludedFiles();

        final SourceFileScanner sfs = new SourceFileScanner( this );
        final FileNameMapper mapper = buildMapper();
        files = sfs.restrict( files, m_srcDir, m_destDir, mapper );
        int count = files.length;
        if( count == 0 )
        {
            return;
        }

        final String message = "Converting " + count + " file" +
            ( count != 1 ? "s" : "" ) + " from " + m_srcDir + " to " +
            m_destDir;
        getLogger().info( message );

        for( int i = 0; i < files.length; i++ )
        {
            final String name = mapper.mapFileName( files[ i ] )[ 0 ];
            convert( files[ i ], name );
        }
    }

    private FileNameMapper buildMapper()
        throws TaskException
    {
        FileNameMapper mapper = null;
        if( m_mapper == null )
        {
            if( m_ext == null )
            {
                mapper = new IdentityMapper();
            }
            else
            {
                mapper = new ExtMapper( m_ext );
            }
        }
        else
        {
            mapper = m_mapper.getImplementation();
        }

        return mapper;
    }

    private void validate()
        throws TaskException
    {
        // Require destDir
        if( m_destDir == null )
        {
            throw new TaskException( "The dest attribute must be set." );
        }

        // if src and dest dirs are the same, require the extension
        // to be set, so we don't stomp every file.  One could still
        // include a file with the same extension, but ....
        if( m_srcDir.equals( m_destDir ) && m_ext == null && m_mapper == null )
        {
            throw new TaskException( "The ext attribute or a mapper must be set if" +
                                     " src and dest dirs are the same." );
        }

        // default srcDir to basedir
        if( m_srcDir == null )
        {
            m_srcDir = getBaseDirectory();
        }
    }

    /**
     * Convert a single file.
     *
     * @param srcName Description of Parameter
     * @param destName Description of Parameter
     * @exception TaskException Description of Exception
     */
    private void convert( String srcName, String destName )
        throws TaskException
    {
        // Build the full file names
        final File srcFile = new File( m_srcDir, srcName );
        final File destFile = new File( m_destDir, destName );

        // Make sure we're not about to clobber something
        if( srcFile.equals( destFile ) )
        {
            throw new TaskException( "file " + srcFile
                                     + " would overwrite its self" );
        }

        final Commandline cmd = buildCommand( srcFile, destFile );

        // Make intermediate directories if needed
        // XXX JDK 1.1 dosen't have File.getParentFile,
        final File parent = destFile.getParentFile();
        if( parent != null )
        {
            if( ( !parent.exists() ) && ( !parent.mkdirs() ) )
            {
                throw new TaskException( "cannot create parent directory " + parent );
            }
        }

        getLogger().debug( "converting " + srcName );
        sun.tools.native2ascii.Main n2a = new sun.tools.native2ascii.Main();
        if( !n2a.convert( cmd.getArguments() ) )
        {
            throw new TaskException( "conversion failed" );
        }
    }

    private Commandline buildCommand( final File srcFile,
                                      final File destFile )
    {
        final Commandline cmd = new Commandline();// Command line to run
        // Set up the basic args (this could be done once, but
        // it's cleaner here)
        if( m_reverse )
        {
            cmd.createArgument().setValue( "-reverse" );
        }

        if( m_encoding != null )
        {
            cmd.createArgument().setValue( "-encoding" );
            cmd.createArgument().setValue( m_encoding );
        }

        cmd.createArgument().setFile( srcFile );
        cmd.createArgument().setFile( destFile );
        return cmd;
    }
}
