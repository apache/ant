/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ScannerUtil;

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
public class Touch
    extends AbstractTask
{
    private long m_millis = -1;
    private String m_dateTime;
    private ArrayList m_filesets = new ArrayList();
    private File m_file;

    /**
     * Date in the format MM/DD/YYYY HH:MM AM_PM.
     */
    public void setDatetime( String dateTime )
    {
        m_dateTime = dateTime;
    }

    /**
     * Sets a single source file to touch. If the file does not exist an empty
     * file will be created.
     */
    public void setFile( final File file )
    {
        m_file = file;
    }

    /**
     * Milliseconds since 01/01/1970 00:00 am.
     */
    public void setMillis( final long millis )
    {
        m_millis = millis;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset( final FileSet set )
    {
        m_filesets.add( set );
    }

    /**
     * Execute the touch operation.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        validate();

        if( m_dateTime != null )
        {
            final DateFormat format =
                DateFormat.getDateTimeInstance( DateFormat.SHORT,
                                                DateFormat.SHORT,
                                                Locale.US );
            try
            {
                final long millis = format.parse( m_dateTime ).getTime();
                if( 0 > millis )
                {
                    final String message = "Date of " + m_dateTime + " results in negative " +
                        "milliseconds value relative to epoch (January 1, 1970, 00:00:00 GMT).";
                    throw new TaskException( message );
                }
                setMillis( millis );
            }
            catch( final ParseException pe )
            {
                throw new TaskException( pe.getMessage(), pe );
            }
        }

        touch();
    }

    private void validate()
        throws TaskException
    {
        if( null == m_file && 0 == m_filesets.size() )
        {
            final String message = "Specify at least one source - a file or a fileset.";
            throw new TaskException( message );
        }

        if( null != m_file && m_file.exists() && m_file.isDirectory() )
        {
            final String message = "Use a fileset to touch directories.";
            throw new TaskException( message );
        }
    }

    private void touch()
        throws TaskException
    {
        if( m_millis < 0 )
        {
            m_millis = System.currentTimeMillis();
        }

        if( m_file != null )
        {
            if( !m_file.exists() )
            {
                getLogger().info( "Creating " + m_file );
                try
                {
                    FileOutputStream fos = new FileOutputStream( m_file );
                    fos.write( new byte[ 0 ] );
                    fos.close();
                }
                catch( final IOException ioe )
                {
                    final String message = "Could not create " + m_file;
                    throw new TaskException( message, ioe );
                }
            }

            touch( m_file );
        }

        // deal with the filesets
        final int size = m_filesets.size();
        for( int i = 0; i < size; i++ )
        {
            final FileSet fs = (FileSet)m_filesets.get( i );
            final DirectoryScanner ds = ScannerUtil.getDirectoryScanner( fs );
            final File fromDir = fs.getDir();

            final String[] srcFiles = ds.getIncludedFiles();
            final String[] srcDirs = ds.getIncludedDirectories();

            for( int j = 0; j < srcFiles.length; j++ )
            {
                touch( new File( fromDir, srcFiles[ j ] ) );
            }

            for( int j = 0; j < srcDirs.length; j++ )
            {
                touch( new File( fromDir, srcDirs[ j ] ) );
            }
        }
    }

    private void touch( final File file )
        throws TaskException
    {
        if( !file.canWrite() )
        {
            throw new TaskException( "Can not change modification date of read-only file " + file );
        }

        final long time = ( m_millis < 0 ) ? System.currentTimeMillis() : m_millis;
        file.setLastModified( time );
    }
}
