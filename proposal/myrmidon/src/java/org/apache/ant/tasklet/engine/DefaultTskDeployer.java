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
import org.apache.ant.convert.ConverterEngine;
import org.apache.ant.convert.ConverterRegistry;
import org.apache.ant.convert.DefaultConverterInfo;
import org.apache.ant.tasklet.engine.DefaultTaskletInfo;
import org.apache.avalon.Component;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentNotAccessibleException;
import org.apache.avalon.ComponentNotFoundException;
import org.apache.avalon.Composer;
import org.apache.avalon.camelot.AbstractDeployer;
import org.apache.avalon.camelot.DeploymentException;
import org.apache.avalon.camelot.RegistryException;
import org.apache.log.Logger;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultTskDeployer
    extends AbstractDeployer
    implements Composer, TskDeployer
{
    protected final static String   TASKDEF_FILE     = "TASK-LIB/taskdefs.properties";
    protected final static String   CONVERTER_FILE   = "TASK-LIB/converters.properties";

    protected TaskletRegistry       m_taskletRegistry;
    protected ConverterRegistry     m_converterRegistry;

    /**
     * Default constructor.
     */
    public DefaultTskDeployer()
    {
        m_autoUndeploy = true;
        m_type = "Tasklet";
    }
    
    /**
     * Retrieve relevent services needed to deploy.
     *
     * @param componentManager the ComponentManager
     * @exception ComponentNotFoundException if an error occurs
     * @exception ComponentNotAccessibleException if an error occurs
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentNotFoundException, ComponentNotAccessibleException
    {
        final ConverterEngine converterEngine = (ConverterEngine)componentManager.
            lookup( "org.apache.ant.convert.ConverterEngine" );

        m_converterRegistry = converterEngine.getConverterRegistry();

        final TaskletEngine taskletEngine = (TaskletEngine)componentManager.
            lookup( "org.apache.ant.tasklet.engine.TaskletEngine" );

        m_taskletRegistry = taskletEngine.getTaskletRegistry();
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

        final ZipFile zipFile = getZipFileFor( file );

        try
        {
            final Properties taskdefs = loadProperties( zipFile, TASKDEF_FILE );
            final Properties converters = loadProperties( zipFile, CONVERTER_FILE );

            try { zipFile.close(); }
            catch( final IOException ioe ) {}

            URL url = null;

            try { url = file.toURL(); }
            catch( final MalformedURLException mue ) {}

            handleTasklets( taskdefs, url );
            handleConverters( converters, url );
        }
        catch( final DeploymentException de )
        {
            try { zipFile.close(); }
            catch( final IOException ioe ) {}

            throw de;
        }
    }
    
    public void deployConverter( String name, String location, URL url )
        throws DeploymentException
    {
        checkDeployment( location, url );
        final ZipFile zipFile = getZipFileFor( url );
        final Properties converters = loadProperties( zipFile, CONVERTER_FILE );
        final String value = converters.getProperty( name );

        if( null == value )
        {
            throw new DeploymentException( "Unable to locate converter named " + name );
        }
            
        handleConverter( name, value, url );
    }
    
    public void deployTasklet( final String name, final String location, final URL url )
        throws DeploymentException
    {
        checkDeployment( location, url );
        final ZipFile zipFile = getZipFileFor( url );
        final Properties tasklets = loadProperties( zipFile, TASKDEF_FILE );
        final String value = tasklets.getProperty( name );
        
        if( null == value )
        {
            throw new DeploymentException( "Unable to locate tasklet named " + name );
        }
        
        handleTasklet( name, value, url );
    }

    protected ZipFile getZipFileFor( final URL url )
        throws DeploymentException
    {
        final File file = getFileFor( url );
        return getZipFileFor( file );
    }

    protected ZipFile getZipFileFor( final File file )
        throws DeploymentException
    {
        try { return new ZipFile( file ); }
        catch( final IOException ioe )
        {
            throw new DeploymentException( "Error opening " + file + 
                                           " due to " + ioe.getMessage(),
                                           ioe );
        }        
    }
    
    /**
     * Create and register Infos for all converters stored in deployment.
     *
     * @param properties the properties
     * @param url the url of deployment
     * @exception DeploymentException if an error occurs
     */
    protected void handleConverters( final Properties properties, final URL url )
        throws DeploymentException
    {
        final Enumeration enum = properties.propertyNames();
        
        while( enum.hasMoreElements() )
        {
            final String key = (String)enum.nextElement();
            final String value = (String)properties.get( key );

            handleConverter( key, value, url );
        }
    }   

    protected void handleConverter( final String name, final String param, final URL url )
        throws DeploymentException
    {
        final int index = param.indexOf( ',' );
        
        if( -1 == index )
        {
            throw new DeploymentException( "Malformed converter definition (" + name + ")" );
        }
        
        final String source = param.substring( 0, index ).trim();
        final String destination = param.substring( index + 1 ).trim();
        
        final DefaultConverterInfo info = 
            new DefaultConverterInfo( source, destination, name, url );
        
        try { m_converterRegistry.register( name, info ); }
        catch( final RegistryException re )
        {
            throw new DeploymentException( "Error registering converter " + 
                                           name + " due to " + re,
                                           re );
        }
        
        m_logger.debug( "Registered converter " + name + " that converts from " + 
                        source + " to " + destination );
    }
    
    /**
     * Create and register Infos for all tasklets stored in deployment.
     *
     * @param properties the properties
     * @param url the url of deployment
     * @exception DeploymentException if an error occurs
     */     
    protected void handleTasklets( final Properties properties, final URL url )
        throws DeploymentException
    {
        final Enumeration enum = properties.propertyNames();
        
        while( enum.hasMoreElements() )
        {
            final String key = (String)enum.nextElement();
            final String value = (String)properties.get( key );
            handleTasklet( key, value, url );
        }
    }

    protected void handleTasklet( final String name, final String classname, final URL url )
        throws DeploymentException
    {
        final DefaultTaskletInfo info = new DefaultTaskletInfo( classname, url );
        
        try { m_taskletRegistry.register( name, info ); }
        catch( final RegistryException re )
        {
            throw new DeploymentException( "Error registering " + name + " due to " + re,
                                           re );
        }
        
        m_logger.debug( "Registered tasklet " + name + " as " + classname );
    }

    /**
     * Utility method to load properties from zip.
     *
     * @param zipFile the zip file
     * @param filename the property filename
     * @return the Properties
     * @exception DeploymentException if an error occurs
     */
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
