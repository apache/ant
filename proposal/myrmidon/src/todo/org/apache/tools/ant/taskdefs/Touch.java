/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Touch a file and/or fileset(s) -- corresponds to the Unix touch command. <p>
 *
 * If the file to touch doesn't exist, an empty one is created. </p> <p>
 *
 * Note: Setting the modification time of files is not supported in JDK 1.1.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:mj@servidium.com">Michael J. Sikorsky</a>
 * @author <a href="mailto:shaw@servidium.com">Robert Shaw</a>
 */
public class Touch extends Task
{// required
    private long millis = -1;
    private ArrayList filesets = new ArrayList();
    private String dateTime;

    private File file;

    /**
     * Date in the format MM/DD/YYYY HH:MM AM_PM.
     *
     * @param dateTime The new Datetime value
     */
    public void setDatetime( String dateTime )
    {
        this.dateTime = dateTime;
    }

    /**
     * Sets a single source file to touch. If the file does not exist an empty
     * file will be created.
     *
     * @param file The new File value
     */
    public void setFile( File file )
    {
        this.file = file;
    }

    /**
     * Milliseconds since 01/01/1970 00:00 am.
     *
     * @param millis The new Millis value
     */
    public void setMillis( long millis )
    {
        this.millis = millis;
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
     * Execute the touch operation.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        if( file == null && filesets.size() == 0 )
        {
            throw
                new TaskException( "Specify at least one source - a file or a fileset." );
        }

        if( file != null && file.exists() && file.isDirectory() )
        {
            throw new TaskException( "Use a fileset to touch directories." );
        }

        if( dateTime != null )
        {
            DateFormat df = DateFormat.getDateTimeInstance( DateFormat.SHORT,
                                                            DateFormat.SHORT,
                                                            Locale.US );
            try
            {
                setMillis( df.parse( dateTime ).getTime() );
                if( millis < 0 )
                {
                    throw new TaskException( "Date of " + dateTime
                                             + " results in negative milliseconds value relative to epoch (January 1, 1970, 00:00:00 GMT)." );
                }
            }
            catch( ParseException pe )
            {
                throw new TaskException( pe.getMessage(), pe );
            }
        }

        touch();
    }

    /**
     * Does the actual work. Entry point for Untar and Expand as well.
     *
     * @exception TaskException Description of Exception
     */
    protected void touch()
        throws TaskException
    {
        if( file != null )
        {
            if( !file.exists() )
            {
                log( "Creating " + file, Project.MSG_INFO );
                try
                {
                    FileOutputStream fos = new FileOutputStream( file );
                    fos.write( new byte[ 0 ] );
                    fos.close();
                }
                catch( IOException ioe )
                {
                    throw new TaskException( "Could not create " + file, ioe );
                }
            }
        }

        if( millis >= 0 && getProject().getJavaVersion() == Project.JAVA_1_1 )
        {
            log( "modification time of files cannot be set in JDK 1.1",
                 Project.MSG_WARN );
            return;
        }

        boolean resetMillis = false;
        if( millis < 0 )
        {
            resetMillis = true;
            millis = System.currentTimeMillis();
        }

        if( file != null )
        {
            touch( file );
        }

        // deal with the filesets
        for( int i = 0; i < filesets.size(); i++ )
        {
            FileSet fs = (FileSet)filesets.get( i );
            DirectoryScanner ds = fs.getDirectoryScanner( getProject() );
            File fromDir = fs.getDir( getProject() );

            String[] srcFiles = ds.getIncludedFiles();
            String[] srcDirs = ds.getIncludedDirectories();

            for( int j = 0; j < srcFiles.length; j++ )
            {
                touch( new File( fromDir, srcFiles[ j ] ) );
            }

            for( int j = 0; j < srcDirs.length; j++ )
            {
                touch( new File( fromDir, srcDirs[ j ] ) );
            }
        }

        if( resetMillis )
        {
            millis = -1;
        }
    }

    protected void touch( File file )
        throws TaskException
    {
        if( !file.canWrite() )
        {
            throw new TaskException( "Can not change modification date of read-only file " + file );
        }

        if( getProject().getJavaVersion() == Project.JAVA_1_1 )
        {
            return;
        }

        final long time = ( millis < 0 ) ? System.currentTimeMillis() : millis;
        file.setLastModified( time );
    }
}
