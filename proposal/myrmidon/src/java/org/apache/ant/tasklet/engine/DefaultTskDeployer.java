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
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.ant.convert.ConverterEngine;
import org.apache.ant.convert.ConverterRegistry;
import org.apache.ant.convert.DefaultConverterInfo;
import org.apache.avalon.Component;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.ComponentNotAccessibleException;
import org.apache.avalon.ComponentNotFoundException;
import org.apache.avalon.Composer;
import org.apache.avalon.Configuration;
import org.apache.avalon.ConfigurationException;
import org.apache.avalon.camelot.AbstractZipDeployer;
import org.apache.avalon.camelot.DefaultLocator;
import org.apache.avalon.camelot.DefaultLocatorRegistry;
import org.apache.avalon.camelot.DeploymentException;
import org.apache.avalon.camelot.Loader;
import org.apache.avalon.camelot.LocatorRegistry;
import org.apache.avalon.camelot.RegistryException;
import org.apache.log.Logger;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultTskDeployer
    extends AbstractZipDeployer
    implements Composer, TskDeployer
{
    protected final static String   TSKDEF_FILE     = "TASK-LIB/taskdefs.xml";

    protected LocatorRegistry       m_dataTypeRegistry;
    protected LocatorRegistry       m_taskletRegistry;
    protected LocatorRegistry       m_converterRegistry;
    protected ConverterRegistry     m_converterInfoRegistry;

    /**
     * Default constructor.
     */
    public DefaultTskDeployer()
    {
        super( false );
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
        final TaskletEngine taskletEngine = (TaskletEngine)componentManager.
            lookup( "org.apache.ant.tasklet.engine.TaskletEngine" );

        final ConverterEngine converterEngine = taskletEngine.getConverterEngine();

        m_converterInfoRegistry = converterEngine.getInfoRegistry();
        m_converterRegistry = converterEngine.getRegistry();

        m_taskletRegistry = taskletEngine.getRegistry();
        
        m_dataTypeRegistry = taskletEngine.getDataTypeEngine().getRegistry();
    }

    public void setLogger( final Logger logger )
    {
        m_logger = logger;
    }

    protected void loadResources( final ZipFile zipFile, final String location, final URL url )
        throws DeploymentException
    {
        final Configuration taskdefs = loadConfiguration( zipFile, TSKDEF_FILE );

        try
        {
            final Iterator tasks = taskdefs.getChildren( "task" );
            while( tasks.hasNext() )
            {
                final Configuration task = (Configuration)tasks.next();
                handleTasklet( task, url );
            }
            
            final Iterator converters = taskdefs.getChildren( "converter" );
            while( converters.hasNext() )
            {
                final Configuration converter = (Configuration)converters.next();
                handleConverter( converter, url );
            }

            final Iterator datatypes = taskdefs.getChildren( "datatype" );
            while( datatypes.hasNext() )
            {
                final Configuration datatype = (Configuration)datatypes.next();
                handleDataType( datatype, url );
            }
        }
        catch( final ConfigurationException ce )
        {
            throw new DeploymentException( "Malformed taskdefs.xml", ce );
        }
    }
    
    public void deployConverter( String name, String location, URL url )
        throws DeploymentException
    {
        checkDeployment( location, url );
        final ZipFile zipFile = getZipFileFor( url );
        final Configuration taskdefs = loadConfiguration( zipFile, TSKDEF_FILE );
        
        try
        {
            final Iterator converters = taskdefs.getChildren( "converter" );
            while( converters.hasNext() )
            {
                final Configuration converter = (Configuration)converters.next();
                if( converter.getAttribute( "classname" ).equals( name ) )
                {
                    handleConverter( converter, url );
                    break;
                }
            }
        }
        catch( final ConfigurationException ce )
        {
            throw new DeploymentException( "Malformed taskdefs.xml", ce );
        }
    }

    public void deployDataType( final String name, final String location, final URL url )
        throws DeploymentException
    {
        checkDeployment( location, url );
        final ZipFile zipFile = getZipFileFor( url );
        final Configuration datatypedefs = loadConfiguration( zipFile, TSKDEF_FILE );
        
        try
        {
            final Iterator datatypes = datatypedefs.getChildren( "datatype" );
            while( datatypes.hasNext() )
            {
                final Configuration datatype = (Configuration)datatypes.next();
                if( datatype.getAttribute( "name" ).equals( name ) )
                {
                    handleDataType( datatype, url );
                    break;
                }
            }
        }
        catch( final ConfigurationException ce )
        {
            throw new DeploymentException( "Malformed taskdefs.xml", ce );
        }
    }
    
    public void deployTasklet( final String name, final String location, final URL url )
        throws DeploymentException
    {
        checkDeployment( location, url );
        final ZipFile zipFile = getZipFileFor( url );
        final Configuration taskdefs = loadConfiguration( zipFile, TSKDEF_FILE );
        
        try
        {
            final Iterator tasks = taskdefs.getChildren( "task" );
            while( tasks.hasNext() )
            {
                final Configuration task = (Configuration)tasks.next();
                if( task.getAttribute( "name" ).equals( name ) )
                {
                    handleTasklet( task, url );
                    break;
                }
            }
        }
        catch( final ConfigurationException ce )
        {
            throw new DeploymentException( "Malformed taskdefs.xml", ce );
        }
    }
    
    protected void handleConverter( final Configuration converter, final URL url )
        throws DeploymentException, ConfigurationException
    {
        final String name = converter.getAttribute( "classname" );
        final String source = converter.getAttribute( "source" );
        final String destination = converter.getAttribute( "destination" );
        
        final DefaultConverterInfo info = new DefaultConverterInfo( source, destination );
       
        try { m_converterInfoRegistry.register( name, info ); }
        catch( final RegistryException re )
        {
            throw new DeploymentException( "Error registering converter info " + 
                                           name + " due to " + re,
                                           re );
        }

        final DefaultLocator locator = new DefaultLocator( name, url );

        try { m_converterRegistry.register( name, locator ); }
        catch( final RegistryException re )
        {
            throw new DeploymentException( "Error registering converter locator " + 
                                           name + " due to " + re,
                                           re );
        }

        m_logger.debug( "Registered converter " + name + " that converts from " + 
                        source + " to " + destination );
    }
    
    protected void handleTasklet( final Configuration task, final URL url )
        throws DeploymentException, ConfigurationException
    {
        final String name = task.getAttribute( "name" );
        final String classname = task.getAttribute( "classname" );

        final DefaultLocator info = new DefaultLocator( classname, url );
        
        try { m_taskletRegistry.register( name, info ); }
        catch( final RegistryException re )
        {
            throw new DeploymentException( "Error registering " + name + " due to " + re,
                                           re );
        }
        
        m_logger.debug( "Registered tasklet " + name + " as " + classname );
    }
  
    protected void handleDataType( final Configuration datatype, final URL url )
        throws DeploymentException, ConfigurationException
    {
        final String name = datatype.getAttribute( "name" );
        final String classname = datatype.getAttribute( "classname" );
        
        final DefaultLocator info = new DefaultLocator( classname, url );
        
        try { m_dataTypeRegistry.register( name, info ); }
        catch( final RegistryException re )
        {
            throw new DeploymentException( "Error registering " + name + " due to " + re,
                                           re );
        }
        
        m_logger.debug( "Registered datatype " + name + " as " + classname );
    }
}
