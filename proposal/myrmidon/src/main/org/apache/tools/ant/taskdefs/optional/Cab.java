/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Os;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;

/**
 * Create a CAB archive.
 *
 * @author Roger Vaughn <a href="mailto:rvaughn@seaconinc.com">
 *      rvaughn@seaconinc.com</a>
 */

public class Cab extends MatchingTask
{
    private Vector filesets = new Vector();
    private boolean doCompress = true;
    private boolean doVerbose = false;

    protected String archiveType = "cab";

    private FileUtils fileUtils = FileUtils.newFileUtils();
    private File baseDir;

    private File cabFile;
    private String cmdOptions;

    /**
     * This is the base directory to look in for things to cab.
     *
     * @param baseDir The new Basedir value
     */
    public void setBasedir( File baseDir )
    {
        this.baseDir = baseDir;
    }

    /**
     * This is the name/location of where to create the .cab file.
     *
     * @param cabFile The new Cabfile value
     */
    public void setCabfile( File cabFile )
    {
        this.cabFile = cabFile;
    }

    /**
     * Sets whether we want to compress the files or only store them.
     *
     * @param compress The new Compress value
     */
    public void setCompress( boolean compress )
    {
        doCompress = compress;
    }

    /**
     * Sets additional cabarc options that aren't supported directly.
     *
     * @param options The new Options value
     */
    public void setOptions( String options )
    {
        cmdOptions = options;
    }

    /**
     * Sets whether we want to see or suppress cabarc output.
     *
     * @param verbose The new Verbose value
     */
    public void setVerbose( boolean verbose )
    {
        doVerbose = verbose;
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

    public void execute()
        throws TaskException
    {

        checkConfiguration();

        Vector files = getFileList();

        // quick exit if the target is up to date
        if( isUpToDate( files ) )
            return;

        log( "Building " + archiveType + ": " + cabFile.getAbsolutePath() );

        if( !Os.isFamily( "windows" ) )
        {
            log( "Using listcab/libcabinet", Project.MSG_VERBOSE );

            StringBuffer sb = new StringBuffer();

            Enumeration fileEnum = files.elements();

            while( fileEnum.hasMoreElements() )
            {
                sb.append( fileEnum.nextElement() ).append( "\n" );
            }
            sb.append( "\n" ).append( cabFile.getAbsolutePath() ).append( "\n" );

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
                String msg = "Problem creating " + cabFile + " " + ex.getMessage();
                throw new TaskException( msg );
            }
        }
        else
        {
            try
            {
                File listFile = createListFile( files );
                ExecTask exec = createExec();
                File outFile = null;

                // die if cabarc fails
                exec.setFailonerror( true );
                exec.setDir( baseDir );

                if( !doVerbose )
                {
                    outFile = fileUtils.createTempFile( "ant", "", null );
                    exec.setOutput( outFile );
                }

                exec.setCommand( createCommand( listFile ) );
                exec.execute();

                if( outFile != null )
                {
                    outFile.delete();
                }

                listFile.delete();
            }
            catch( IOException ioe )
            {
                String msg = "Problem creating " + cabFile + " " + ioe.getMessage();
                throw new TaskException( msg );
            }
        }
    }

    /**
     * Get the complete list of files to be included in the cab. Filenames are
     * gathered from filesets if any have been added, otherwise from the
     * traditional include parameters.
     *
     * @return The FileList value
     * @exception TaskException Description of Exception
     */
    protected Vector getFileList()
        throws TaskException
    {
        Vector files = new Vector();

        if( filesets.size() == 0 )
        {
            // get files from old methods - includes and nested include
            appendFiles( files, super.getDirectoryScanner( baseDir ) );
        }
        else
        {
            // get files from filesets
            for( int i = 0; i < filesets.size(); i++ )
            {
                FileSet fs = (FileSet)filesets.elementAt( i );
                if( fs != null )
                {
                    appendFiles( files, fs.getDirectoryScanner( project ) );
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
    protected boolean isUpToDate( Vector files )
    {
        boolean upToDate = true;
        for( int i = 0; i < files.size() && upToDate; i++ )
        {
            String file = files.elementAt( i ).toString();
            if( new File( baseDir, file ).lastModified() >
                cabFile.lastModified() )
                upToDate = false;
        }
        return upToDate;
    }

    /**
     * Append all files found by a directory scanner to a vector.
     *
     * @param files Description of Parameter
     * @param ds Description of Parameter
     */
    protected void appendFiles( Vector files, DirectoryScanner ds )
    {
        String[] dsfiles = ds.getIncludedFiles();

        for( int i = 0; i < dsfiles.length; i++ )
        {
            files.addElement( dsfiles[ i ] );
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
        if( baseDir == null )
        {
            throw new TaskException( "basedir attribute must be set!" );
        }
        if( !baseDir.exists() )
        {
            throw new TaskException( "basedir does not exist!" );
        }
        if( cabFile == null )
        {
            throw new TaskException( "cabfile attribute must be set!" );
        }
    }

    /**
     * Create the cabarc command line to use.
     *
     * @param listFile Description of Parameter
     * @return Description of the Returned Value
     */
    protected Commandline createCommand( File listFile )
        throws TaskException
    {
        Commandline command = new Commandline();
        command.setExecutable( "cabarc" );
        command.createArgument().setValue( "-r" );
        command.createArgument().setValue( "-p" );

        if( !doCompress )
        {
            command.createArgument().setValue( "-m" );
            command.createArgument().setValue( "none" );
        }

        if( cmdOptions != null )
        {
            command.createArgument().setLine( cmdOptions );
        }

        command.createArgument().setValue( "n" );
        command.createArgument().setFile( cabFile );
        command.createArgument().setValue( "@" + listFile.getAbsolutePath() );

        return command;
    }

    /**
     * Create a new exec delegate. The delegate task is populated so that it
     * appears in the logs to be the same task as this one.
     *
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    protected ExecTask createExec()
        throws TaskException
    {
        ExecTask exec = (ExecTask)project.createTask( "exec" );
        exec.setOwningTarget( this.getOwningTarget() );
        exec.setDescription( this.getDescription() );

        return exec;
    }

    /**
     * Creates a list file. This temporary file contains a list of all files to
     * be included in the cab, one file per line.
     *
     * @param files Description of Parameter
     * @return Description of the Returned Value
     * @exception IOException Description of Exception
     */
    protected File createListFile( Vector files )
        throws IOException
    {
        File listFile = fileUtils.createTempFile( "ant", "", null );

        PrintWriter writer = new PrintWriter( new FileOutputStream( listFile ) );

        for( int i = 0; i < files.size(); i++ )
        {
            writer.println( files.elementAt( i ).toString() );
        }
        writer.close();

        return listFile;
    }
}
