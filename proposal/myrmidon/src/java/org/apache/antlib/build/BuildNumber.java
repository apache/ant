/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * This is a basic task that can be used to track build numbers.
 *
 * It will first attempt to read a build number from a file, then
 * set the property "build.number" to the value that was read in
 * (or 0 if no such value). Then it will increment the build number
 * by one and write it back out into the file.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant:task name="build-number"
 */
public class BuildNumber
    extends AbstractTask
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( BuildNumber.class );

    /**
     * The name of the property in which the build number is stored.
     */
    private static final String DEFAULT_PROPRTY_NAME = "build.number";

    /**
     * The default filename to use if no file specified.
     */
    private static final String DEFAULT_FILENAME = DEFAULT_PROPRTY_NAME;

    /**
     * The File in which the build number is stored.
     */
    private File m_file;

    /**
     * Specify the file in which the build numberis stored.
     * Defaults to "build.number" if not specified.
     *
     * @param file the file in which build number is stored.
     */
    public void setFile( final File file )
    {
        m_file = file;
    }

    /**
     * Run task.
     *
     * @exception TaskException if an error occurs
     */
    public void execute()
        throws TaskException
    {
        validate();

        final Properties properties = loadProperties();
        final int buildNumber = getBuildNumber( properties );

        properties.put( DEFAULT_PROPRTY_NAME,
                        String.valueOf( buildNumber + 1 ) );

        // Write the properties file back out
        FileOutputStream output = null;
        try
        {
            final String header = REZ.getString( "buildnumber.header.info" );

            output = new FileOutputStream( m_file );
            properties.store( output, header );
        }
        catch( final IOException ioe )
        {
            final String message =
                REZ.getString( "buildnumber.badwrite.error", m_file );
            throw new TaskException( message, ioe );
        }
        finally
        {
            IOUtil.shutdownStream( output );
        }

        //Finally set the property
        getContext().setProperty( DEFAULT_PROPRTY_NAME,
                                  String.valueOf( buildNumber ) );
    }

    /**
     * Utility method to retrieve build number from properties object.
     *
     * @param properties the properties to retrieve build number from
     * @return the build number or if no number in properties object
     * @throws TaskException if build.number property is not an integer
     */
    private int getBuildNumber( final Properties properties )
        throws TaskException
    {
        final String buildNumber =
            properties.getProperty( DEFAULT_PROPRTY_NAME, "0" ).trim();

        // Try parsing the line into an integer.
        try
        {
            return Integer.parseInt( buildNumber );
        }
        catch( final NumberFormatException nfe )
        {
            final String message =
                REZ.getString( "buildnumber.noparse.error", m_file, buildNumber );
            throw new TaskException( message, nfe );
        }
    }

    /**
     * Utility method to load properties from file.
     *
     * @return the loaded properties
     * @throws TaskException
     */
    private Properties loadProperties()
        throws TaskException
    {
        FileInputStream input = null;
        try
        {
            final Properties properties = new Properties();
            input = new FileInputStream( m_file );
            properties.load( input );
            return properties;
        }
        catch( final IOException ioe )
        {
            throw new TaskException( ioe.getMessage(), ioe );
        }
        finally
        {
            IOUtil.shutdownStream( input );
        }
    }

    /**
     * Validate that the task parameters are valid.
     *
     * @throws TaskException if parameters are invalid
     */
    private void validate()
        throws TaskException
    {
        if( null == m_file )
        {
            m_file = getContext().resolveFile( DEFAULT_FILENAME );
        }

        if( !m_file.exists() )
        {
            try
            {
                m_file.createNewFile();
            }
            catch( final IOException ioe )
            {
                final String message =
                    REZ.getString( "buildnumber.nocreate.error", m_file );
                throw new TaskException( message, ioe );
            }
        }

        if( !m_file.canRead() )
        {
            final String message =
                REZ.getString( "buildnumber.noread.error", m_file );
            throw new TaskException( message );
        }

        if( !m_file.canWrite() )
        {
            final String message =
                REZ.getString( "buildnumber.nowrite.error", m_file );
            throw new TaskException( message );
        }
    }
}