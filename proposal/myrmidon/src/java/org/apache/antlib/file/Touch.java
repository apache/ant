/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.DirectoryScanner;
import org.apache.tools.todo.types.FileSet;
import org.apache.tools.todo.types.ScannerUtil;

/**
 * Touch a file and/or fileset(s) -- corresponds to the Unix touch command.
 *
 * If the file to touch doesn't exist, an empty one is created. </p>
 *
 * @ant:task name="touch"
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:mj@servidium.com">Michael J. Sikorsky</a>
 * @author <a href="mailto:shaw@servidium.com">Robert Shaw</a>
 * @version $Revision$ $Date$
 */
public class Touch
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( Touch.class );

    private long m_millis = -1;
    private String m_datetime;
    private ArrayList m_filesets = new ArrayList();
    private File m_file;

    /**
     * Date in the format MM/DD/YYYY HH:MM AM_PM.
     */
    public void setDatetime( final String datetime )
    {
        m_datetime = datetime;
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

        if( m_datetime != null )
        {
            final DateFormat format =
                DateFormat.getDateTimeInstance( DateFormat.SHORT,
                                                DateFormat.SHORT,
                                                Locale.US );
            try
            {
                final long millis = format.parse( m_datetime ).getTime();
                if( 0 > millis )
                {
                    final String message = REZ.getString( "touch.neg-time.error", m_datetime );
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
            final String message = REZ.getString( "touch.no-files.error" );
            throw new TaskException( message );
        }

        if( null != m_file && m_file.exists() && m_file.isDirectory() )
        {
            final String message = REZ.getString( "touch.use-fileset.error" );
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

        if( null != m_file )
        {
            if( !m_file.exists() )
            {
                if( getContext().isInfoEnabled() )
                {
                    final String message = REZ.getString( "touch.create.notice", m_file );
                    getContext().info( message );
                }

                try
                {
                    FileOutputStream fos = new FileOutputStream( m_file );
                    fos.write( new byte[ 0 ] );
                    fos.close();
                }
                catch( final IOException ioe )
                {
                    final String message = REZ.getString( "touch.no-touch.error", m_file, ioe );
                    throw new TaskException( message, ioe );
                }
            }

            touch( m_file );
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

            for( int j = 0; j < srcFiles.length; j++ )
            {
                final File file = new File( fromDir, srcFiles[ j ] );
                touch( file );
            }

            for( int j = 0; j < srcDirs.length; j++ )
            {
                final File file = new File( fromDir, srcDirs[ j ] );
                touch( file );
            }
        }
    }

    private void touch( final File file )
        throws TaskException
    {
        if( !file.canWrite() )
        {
            final String message = REZ.getString( "touch.readonly-file.error", file );
            throw new TaskException( message );
        }

        final long time = ( m_millis < 0 ) ? System.currentTimeMillis() : m_millis;
        file.setLastModified( time );
    }
}
