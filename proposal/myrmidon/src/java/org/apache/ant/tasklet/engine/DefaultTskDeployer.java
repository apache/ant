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
import org.apache.ant.convert.engine.ConverterEngine;
import org.apache.ant.convert.engine.ConverterRegistry;
import org.apache.ant.convert.engine.DefaultConverterInfo;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.camelot.AbstractDeployer;
import org.apache.avalon.framework.camelot.DefaultLocator;
import org.apache.avalon.framework.camelot.DefaultRegistry;
import org.apache.avalon.framework.camelot.DeploymentException;
import org.apache.avalon.framework.camelot.DeployerUtil;
import org.apache.avalon.framework.camelot.Loader;
import org.apache.avalon.framework.camelot.Registry;
import org.apache.avalon.framework.camelot.RegistryException;
import org.apache.log.Logger;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultTskDeployer
    extends AbstractDeployer
    implements Composable, TskDeployer
{
    protected final static String   TSKDEF_FILE     = "TASK-LIB/taskdefs.xml";

    protected Registry              m_dataTypeRegistry;
    protected Registry              m_taskletRegistry;
    protected Registry              m_converterRegistry;
    protected ConverterRegistry     m_converterInfoRegistry;

    /**
     * Default constructor.
     */
    public DefaultTskDeployer()
    {
        super();
        m_autoUndeploy = true;
        m_type = "Tasklet";
    }
    
    /**
     * Retrieve relevent services needed to deploy.
     *
     * @param componentManager the ComponentManager
     * @exception ComponentException if an error occurs
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        final TaskletEngine taskletEngine = (TaskletEngine)componentManager.
            lookup( "org.apache.ant.tasklet.engine.TaskletEngine" );

        m_taskletRegistry = taskletEngine.getRegistry();

        final ConverterEngine converterEngine = (ConverterEngine)componentManager.
            lookup( "org.apache.ant.convert.engine.ConverterEngine" );

        m_converterInfoRegistry = converterEngine.getInfoRegistry();
        m_converterRegistry = converterEngine.getRegistry();

        final DataTypeEngine dataTypeEngine = (DataTypeEngine)componentManager.
            lookup( "org.apache.ant.tasklet.engine.DataTypeEngine" );
        
        m_dataTypeRegistry = dataTypeEngine.getRegistry();
    }

    /**
     * Deploy a file.
     * Eventually this should be cached for performance reasons.
     *
     * @param location the location 
     * @param file the file
     * @exception DeploymentException if an error occurs
     */
    protected void deployFromFile( final String location, final File file )
        throws DeploymentException
    {
        final ZipFile zipFile = DeployerUtil.getZipFileFor( file );

        URL url = null;
        
        try
        {
            try { url = file.toURL(); }
            catch( final MalformedURLException mue ) 
            {
                throw new DeploymentException( "Unable to form url", mue );
            }
            loadResources( zipFile, location, url );
        }
        finally
        {
            try { zipFile.close(); }
            catch( final IOException ioe ) {}
        }
    }

    protected void loadResources( final ZipFile zipFile, final String location, final URL url )
        throws DeploymentException
    {
        final Configuration taskdefs = DeployerUtil.loadConfiguration( zipFile, TSKDEF_FILE );

        try
        {
            final Configuration[] tasks = taskdefs.getChildren( "task" );
            for( int i = 0; i < tasks.length; i++ )
            {
                handleTasklet( tasks[ i ], url );
            }
            
            final Configuration[] converters = taskdefs.getChildren( "converter" );
            for( int i = 0; i < converters.length; i++ )
            {
                handleConverter( converters[ i ], url );
            }

            final Configuration[] datatypes = taskdefs.getChildren( "datatype" );
            for( int i = 0; i < datatypes.length; i++ )
            {
                handleDataType( datatypes[ i ], url );
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
        final ZipFile zipFile = DeployerUtil.getZipFileFor( getFileFor( url ) );
        final Configuration taskdefs = DeployerUtil.loadConfiguration( zipFile, TSKDEF_FILE );
        
        try
        {
            final Configuration[] converters = taskdefs.getChildren( "converter" );
            for( int i = 0; i < converters.length; i++ )
            {
                if( converters[ i ].getAttribute( "classname" ).equals( name ) )
                {
                    handleConverter( converters[ i ], url );
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
        final ZipFile zipFile = DeployerUtil.getZipFileFor( getFileFor( url ) );
        final Configuration datatypedefs = 
            DeployerUtil.loadConfiguration( zipFile, TSKDEF_FILE );
        
        try
        {
            final Configuration[] datatypes = datatypedefs.getChildren( "datatype" );
            for( int i = 0; i < datatypes.length; i++ )
            {
                if( datatypes[ i ].getAttribute( "name" ).equals( name ) )
                {
                    handleDataType( datatypes[ i ], url );
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
        final ZipFile zipFile = DeployerUtil.getZipFileFor( getFileFor( url ) );
        final Configuration taskdefs = DeployerUtil.loadConfiguration( zipFile, TSKDEF_FILE );
        
        try
        {
            final Configuration[] tasks = taskdefs.getChildren( "task" );
            for( int i = 0; i < tasks.length; i++ )
            {
                if( tasks[ i ].getAttribute( "name" ).equals( name ) )
                {
                    handleTasklet( tasks[ i ], url );
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

        getLogger().debug( "Registered converter " + name + " that converts from " + 
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
        
        getLogger().debug( "Registered tasklet " + name + " as " + classname );
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
        
        getLogger().debug( "Registered datatype " + name + " as " + classname );
    }
}
