/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.aut.nativelib.ExecManager;
import org.apache.aut.nativelib.Os;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.todo.taskdefs.MatchingTask;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.FileSet;
import org.apache.tools.todo.types.ScannerUtil;

/**
 * Create a CAB archive.
 *
 * @author <a href="mailto:rvaughn@seaconinc.com">Roger Vaughn</a>
 */
public class Cab
    extends MatchingTask
{
    private ArrayList m_filesets = new ArrayList();
    private boolean m_compress = true;
    private File m_baseDir;
    private File m_cabFile;
    private String m_options;

    /**
     * This is the base directory to look in for things to cab.
     *
     * @param baseDir The new Basedir value
     */
    public void setBasedir( final File baseDir )
    {
        m_baseDir = baseDir;
    }

    /**
     * This is the name/location of where to create the .cab file.
     *
     * @param cabFile The new Cabfile value
     */
    public void setCabfile( final File cabFile )
    {
        m_cabFile = cabFile;
    }

    /**
     * Sets whether we want to compress the files or only store them.
     *
     * @param compress The new Compress value
     */
    public void setCompress( final boolean compress )
    {
        m_compress = compress;
    }

    /**
     * Sets additional cabarc options that aren't supported directly.
     *
     * @param options The new Options value
     */
    public void setOptions( final String options )
    {
        m_options = options;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( final FileSet set )
    {
        m_filesets.add( set );
    }

    public void execute()
        throws TaskException
    {
        checkConfiguration();

        final ArrayList files = getFileList();

        // quick exit if the target is up to date
        if( isUpToDate( files ) )
        {
            return;
        }

        getContext().info( "Building cab: " + m_cabFile.getAbsolutePath() );

        if( !Os.isFamily( Os.OS_FAMILY_WINDOWS ) )
        {
            getContext().debug( "Using listcab/libcabinet" );

            final StringBuffer sb = new StringBuffer();

            final Iterator e = files.iterator();
            while( e.hasNext() )
            {
                sb.append( e.next() ).append( "\n" );
            }
            sb.append( "\n" ).append( m_cabFile.getAbsolutePath() ).append( "\n" );

            try
            {
                Process p = Runtime.getRuntime().exec( "listcab" );
                OutputStream out = p.getOutputStream();
                out.write( sb.toString().getBytes() );
                out.flush();
                out.close();
            }
            catch( IOException ex )
            {
                String msg = "Problem creating " + m_cabFile + " " + ex.getMessage();
                throw new TaskException( msg );
            }
        }
        else
        {
            try
            {
                File listFile = createListFile( files );
                Execute exe = new Execute();
                exe.setWorkingDirectory( m_baseDir );
                final Commandline cmd = createCommand( listFile );
                exe.setCommandline( cmd );
                exe.execute( getContext() );

                listFile.delete();
            }
            catch( final IOException ioe )
            {
                final String message =
                    "Problem creating " + m_cabFile + " " + ioe.getMessage();
                throw new TaskException( message );
            }
        }
    }

    /**
     * Get the complete list of files to be included in the cab. Filenames are
     * gathered from filesets if any have been added, otherwise from the
     * traditional include parameters.
     */
    protected ArrayList getFileList()
        throws TaskException
    {
        ArrayList files = new ArrayList();

        if( m_filesets.size() == 0 )
        {
            // get files from old methods - includes and nested include
            appendFiles( files, super.getDirectoryScanner( m_baseDir ) );
        }
        else
        {
            // get files from filesets
            for( int i = 0; i < m_filesets.size(); i++ )
            {
                FileSet fs = (FileSet)m_filesets.get( i );
                if( fs != null )
                {
                    appendFiles( files, ScannerUtil.getDirectoryScanner( fs ) );
                }
            }
        }

        return files;
    }

    /**
     * Check to see if the target is up to date with respect to input files.
     *
     * @param files Description of Parameter
     * @return true if the cab file is newer than its dependents.
     */
    protected boolean isUpToDate( ArrayList files )
    {
        boolean upToDate = true;
        for( int i = 0; i < files.size() && upToDate; i++ )
        {
            String file = files.get( i ).toString();
            if( new File( m_baseDir, file ).lastModified() >
                m_cabFile.lastModified() )
            {
                upToDate = false;
            }
        }
        return upToDate;
    }

    /**
     * Append all files found by a directory scanner to a vector.
     *
     * @param files Description of Parameter
     * @param ds Description of Parameter
     */
    protected void appendFiles( ArrayList files, DirectoryScanner ds )
    {
        String[] dsfiles = ds.getIncludedFiles();

        for( int i = 0; i < dsfiles.length; i++ )
        {
            files.add( dsfiles[ i ] );
        }
    }

    /*
     * I'm not fond of this pattern: "sub-method expected to throw
     * task-cancelling exceptions".  It feels too much like programming
     * for side-effects to me...
     */
    protected void checkConfiguration()
        throws TaskException
    {
        if( m_baseDir == null )
        {
            throw new TaskException( "basedir attribute must be set!" );
        }
        if( !m_baseDir.exists() )
        {
            throw new TaskException( "basedir does not exist!" );
        }
        if( m_cabFile == null )
        {
            throw new TaskException( "cabfile attribute must be set!" );
        }
    }

    /**
     * Create the cabarc command line to use.
     */
    protected Commandline createCommand( final File listFile )
        throws TaskException
    {
        final Commandline cmd = new Commandline();
        cmd.setExecutable( "cabarc" );
        cmd.addArgument( "-r" );
        cmd.addArgument( "-p" );

        if( !m_compress )
        {
            cmd.addArgument( "-m" );
            cmd.addArgument( "none" );
        }

        if( m_options != null )
        {
            cmd.addLine( m_options );
        }

        cmd.addArgument( "n" );
        cmd.addArgument( m_cabFile );
        cmd.addArgument( "@" + listFile.getAbsolutePath() );

        return cmd;
    }

    /**
     * Creates a list file. This temporary file contains a list of all files to
     * be included in the cab, one file per line.
     *
     * @param files Description of Parameter
     * @return Description of the Returned Value
     * @exception java.io.IOException Description of Exception
     */
    protected File createListFile( ArrayList files )
        throws IOException
    {
        File listFile = File.createTempFile( "ant", "", getBaseDirectory() );

        PrintWriter writer = new PrintWriter( new FileOutputStream( listFile ) );

        for( int i = 0; i < files.size(); i++ )
        {
            writer.println( files.get( i ).toString() );
        }
        writer.close();

        return listFile;
    }
}
