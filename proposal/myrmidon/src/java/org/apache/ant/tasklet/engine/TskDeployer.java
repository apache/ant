/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet.engine;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.ant.tasklet.engine.DefaultTaskletInfo;
import org.apache.ant.convert.ConverterRegistry;
import org.apache.ant.convert.DefaultConverterInfo;
import org.apache.avalon.Component;
import org.apache.avalon.camelot.AbstractDeployer;
import org.apache.avalon.camelot.DeploymentException;
import org.apache.avalon.camelot.RegistryException;
import org.apache.log.Logger;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class TskDeployer
    extends AbstractDeployer
{
    protected final static String   TASKDEF_FILE     = "TASK-LIB/taskdefs.properties";
    protected final static String   CONVERTER_FILE   = "TASK-LIB/converters.properties";

    protected TaskletRegistry       m_taskletRegistry;
    protected ConverterRegistry     m_converterRegistry;

    public TskDeployer( final TaskletRegistry taskletRegistry, 
                        final ConverterRegistry converterRegistry )
    {
        m_taskletRegistry = taskletRegistry;
        m_converterRegistry = converterRegistry;
        m_autoUndeploy = true;
        m_type = "Tasklet";
    }

    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    protected boolean isValidLocation( final String location )
    {
        //TODO: Make sure it is valid JavaIdentifier 
        //that optionally has '-' embedded in it
        return true;
    }
    
    /**
     * Deploy Tasklets from a .tsk file.
     * Eventually this should be cached for performance reasons.
     *
     * @param location the location 
     * @param file the file
     * @exception DeploymentException if an error occurs
     */
    protected void deployFromFile( final String location, final File file )
        throws DeploymentException
    {
        m_logger.info( "Deploying .tsk file (" + file + ") as " + location );

        ZipFile zipFile = null;

        try { zipFile = new ZipFile( file ); }
        catch( final IOException ioe )
        {
            throw new DeploymentException( "Error opening " + file + 
                                           " due to " + ioe.getMessage(),
                                           ioe );
        }

        try
        {
            final Properties taskdefs = loadProperties( zipFile, TASKDEF_FILE );
            final Properties converters = loadProperties( zipFile, CONVERTER_FILE );

            try { zipFile.close(); }
            catch( final IOException ioe ) {}

            URL url = null;

            try { url = file.toURL(); }
            catch( final MalformedURLException mue ) {}

            handleTaskdefs( taskdefs, url );
            handleConverters( converters, url );
        }
        catch( final DeploymentException de )
        {
            try { zipFile.close(); }
            catch( final IOException ioe ) {}

            throw de;
        }
    }

    protected void handleConverters( final Properties properties, final URL url )
        throws DeploymentException
    {
        final Enumeration enum = properties.propertyNames();
        
        while( enum.hasMoreElements() )
        {
            final String key = (String)enum.nextElement();
            final String value = (String)properties.get( key );
            final int index = value.indexOf( ',' );

            if( -1 == index )
            {
                throw new DeploymentException( "Malformed converter definition (" + 
                                               key + ")" );
            }
            
            final String source = value.substring( 0, index ).trim();
            final String destination = value.substring( index + 1 ).trim();

            final DefaultConverterInfo info = 
                new DefaultConverterInfo( source, destination, key, url );
            
            try { m_converterRegistry.register( key, info ); }
            catch( final RegistryException re )
            {
                throw new DeploymentException( "Error registering converter " + 
                                               key + " due to " + re,
                                               re );
            }

            m_logger.debug( "Registered converter " + key + " that converts from " + 
                            source + " to " + destination );
        }
    }   
     
    protected void handleTaskdefs( final Properties properties, final URL url )
        throws DeploymentException
    {
        final Enumeration enum = properties.propertyNames();
        
        while( enum.hasMoreElements() )
        {
            final String key = (String)enum.nextElement();
            final String value = (String)properties.get( key );
            final DefaultTaskletInfo info = new DefaultTaskletInfo( value, url );
                    
            try { m_taskletRegistry.register( key, info ); }
            catch( final RegistryException re )
            {
                throw new DeploymentException( "Error registering " + key + " due to " + re,
                                               re );
            }
            
            m_logger.debug( "Registered tasklet " + key + " as " + value );
        }
    }

    protected Properties loadProperties( final ZipFile zipFile, final String filename )
        throws DeploymentException
    {
        final ZipEntry entry = zipFile.getEntry( filename );
        if( null == entry )
        {
            throw new DeploymentException( "Unable to locate " + filename + 
                                           " in " + zipFile.getName() );
        }
        
        Properties properties = new Properties();
        
        try
        {
            properties.load( zipFile.getInputStream( entry ) );
        }
        catch( final IOException ioe )
        {
            throw new DeploymentException( "Error reading " + filename + 
                                           " from " + zipFile.getName(),
                                           ioe );
        }

        return properties;
    }
    
    protected boolean canUndeploy( final Component component )
        throws DeploymentException
    {
        return true;
    }
    
    protected void shutdownDeployment( final Component component )
        throws DeploymentException
    {
    }
}
