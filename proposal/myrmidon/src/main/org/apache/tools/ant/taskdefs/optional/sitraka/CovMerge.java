/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import org.apache.aut.nativelib.ExecManager;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.framework.Execute;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ScannerUtil;

/**
 * Convenient task to run the snapshot merge utility for JProbe Coverage.
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class CovMerge
    extends AbstractTask
{
    /**
     * coverage home, it is mandatory
     */
    private File home = null;

    /**
     * the name of the output snapshot
     */
    private File tofile = null;

    /**
     * the filesets that will get all snapshots to merge
     */
    private ArrayList filesets = new ArrayList();

    private boolean verbose;

    //---------------- the tedious job begins here

    public CovMerge()
    {
    }

    /**
     * set the coverage home. it must point to JProbe coverage directories where
     * are stored native librairies and jars
     *
     * @param value The new Home value
     */
    public void setHome( File value )
    {
        this.home = value;
    }

    /**
     * Set the output snapshot file
     *
     * @param value The new Tofile value
     */
    public void setTofile( File value )
    {
        this.tofile = value;
    }

    /**
     * run the merging in verbose mode
     *
     * @param flag The new Verbose value
     */
    public void setVerbose( boolean flag )
    {
        this.verbose = flag;
    }

    /**
     * add a fileset containing the snapshots to include/exclude
     *
     * @param fs The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet fs )
    {
        filesets.add( fs );
    }

    /**
     * execute the jpcovmerge by providing a parameter file
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        checkOptions();

        File paramfile = createParamFile();
        try
        {
            Commandline cmdl = new Commandline();
            cmdl.setExecutable( new File( home, "jpcovmerge" ).getAbsolutePath() );
            if( verbose )
            {
                cmdl.addArgument( "-v" );
            }
            cmdl.addArgument( "-jp_paramfile=" + paramfile.getAbsolutePath() );

            final ExecManager execManager = (ExecManager)getService( ExecManager.class );
            final Execute exe = new Execute( execManager );
            getContext().debug( cmdl.toString() );
            exe.setCommandline( cmdl );

            // JProbe process always return 0 so  we will not be
            // able to check for failure ! :-(
            int exitValue = exe.execute();
            if( exitValue != 0 )
            {
                throw new TaskException( "JProbe Coverage Merging failed (" + exitValue + ")" );
            }
        }
        catch( IOException e )
        {
            throw new TaskException( "Failed to run JProbe Coverage Merge: " + e );
        }
        finally
        {
            //@todo should be removed once switched to JDK1.2
            paramfile.delete();
        }
    }

    /**
     * get the snapshots from the filesets
     *
     * @return The Snapshots value
     */
    protected File[] getSnapshots()
        throws TaskException
    {
        ArrayList v = new ArrayList();
        final int size = filesets.size();
        for( int i = 0; i < size; i++ )
        {
            FileSet fs = (FileSet)filesets.get( i );
            DirectoryScanner ds = ScannerUtil.getDirectoryScanner( fs );
            ds.scan();
            String[] f = ds.getIncludedFiles();
            for( int j = 0; j < f.length; j++ )
            {
                String pathname = f[ j ];
                final File file = new File( ds.getBasedir(), pathname );
                file = getContext().resolveFile( file.getPath() );
                v.add( file );
            }
        }

        return (File[])v.toArray( new File[ v.size() ] );
    }

    /**
     * check for mandatory options
     *
     * @exception TaskException Description of Exception
     */
    protected void checkOptions()
        throws TaskException
    {
        if( tofile == null )
        {
            throw new TaskException( "'tofile' attribute must be set." );
        }

        // check coverage home
        if( home == null || !home.isDirectory() )
        {
            throw new TaskException( "Invalid home directory. Must point to JProbe home directory" );
        }
        home = new File( home, "coverage" );
        File jar = new File( home, "coverage.jar" );
        if( !jar.exists() )
        {
            throw new TaskException( "Cannot find Coverage directory: " + home );
        }
    }

    /**
     * create the parameters file that contains all file to merge and the output
     * filename.
     *
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    protected File createParamFile()
        throws TaskException
    {
        File[] snapshots = getSnapshots();
        File file = createTmpFile();
        FileWriter fw = null;
        try
        {
            fw = new FileWriter( file );
            PrintWriter pw = new PrintWriter( fw );
            for( int i = 0; i < snapshots.length; i++ )
            {
                pw.println( snapshots[ i ].getAbsolutePath() );
            }
            // last file is the output snapshot
            pw.println( getContext().resolveFile( tofile.getPath() ) );
            pw.flush();
        }
        catch( IOException e )
        {
            throw new TaskException( "I/O error while writing to " + file, e );
        }
        finally
        {
            if( fw != null )
            {
                try
                {
                    fw.close();
                }
                catch( IOException ignored )
                {
                }
            }
        }
        return file;
    }

    /**
     * create a temporary file in the current dir (For JDK1.1 support)
     *
     * @return Description of the Returned Value
     */
    protected File createTmpFile()
    {
        final long rand = ( new Random( System.currentTimeMillis() ) ).nextLong();
        File file = new File( "jpcovmerge" + rand + ".tmp" );
        return file;
    }
}
