/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.deployer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.avalon.framework.camelot.AbstractDeployer;
import org.apache.avalon.framework.camelot.DefaultLocator;
import org.apache.avalon.framework.camelot.DeployerUtil;
import org.apache.avalon.framework.camelot.DeploymentException;
import org.apache.avalon.framework.camelot.Loader;
import org.apache.avalon.framework.camelot.Registry;
import org.apache.avalon.framework.camelot.RegistryException;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Loggable;
import org.apache.log.Logger;
import org.apache.myrmidon.api.Task;
import org.apache.myrmidon.components.converter.ConverterInfo;
import org.apache.myrmidon.components.converter.ConverterRegistry;
import org.apache.myrmidon.components.executor.Executor;
import org.apache.myrmidon.components.type.ComponentFactory;
import org.apache.myrmidon.components.type.DefaultComponentFactory;
import org.apache.myrmidon.components.type.TypeManager;
import org.apache.myrmidon.converter.Converter;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultTskDeployer
    extends AbstractDeployer
    implements Composable, TskDeployer, Loggable
{
    private final static String   TSKDEF_FILE     = "TASK-LIB/taskdefs.xml";

    private ConverterRegistry     m_converterInfoRegistry;
    private TypeManager           m_typeManager;

    /**
     * Default constructor.
     */
    public DefaultTskDeployer()
    {
        m_autoUndeploy = true;
        m_type = "Task";
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
        m_converterInfoRegistry = (ConverterRegistry)componentManager.lookup( ConverterRegistry.ROLE );
        m_typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
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

    private void loadResources( final ZipFile zipFile, final String location, final URL url )
        throws DeploymentException
    {
        final Configuration taskdefs = DeployerUtil.loadConfiguration( zipFile, TSKDEF_FILE );

        final DefaultComponentFactory factory =
            new DefaultComponentFactory( new URL[] { url } );

        try
        {
            final Configuration[] tasks = taskdefs.getChildren( "task" );
            for( int i = 0; i < tasks.length; i++ )
            {
                handleTask( tasks[ i ], url, factory );
            }

            final Configuration[] converters = taskdefs.getChildren( "converter" );
            for( int i = 0; i < converters.length; i++ )
            {
                handleConverter( converters[ i ], url, factory );
            }

            final Configuration[] datatypes = taskdefs.getChildren( "datatype" );
            for( int i = 0; i < datatypes.length; i++ )
            {
                handleDataType( datatypes[ i ], url, factory );
            }
        }
        catch( final ConfigurationException ce )
        {
            throw new DeploymentException( "Malformed taskdefs.xml", ce );
        }
        catch( final Exception e )
        {
            throw new DeploymentException( "Failed to deploy " + location, e );
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
                    final DefaultComponentFactory factory =
                        new DefaultComponentFactory( new URL[] { url } );
                    handleConverter( converters[ i ], url, factory );
                    break;
                }
            }
        }
        catch( final ConfigurationException ce )
        {
            throw new DeploymentException( "Malformed taskdefs.xml", ce );
        }
        catch( final Exception e )
        {
            throw new DeploymentException( "Failed to deploy " + name, e );
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
                    final DefaultComponentFactory factory =
                        new DefaultComponentFactory( new URL[] { url } );
                    handleDataType( datatypes[ i ], url, factory );
                    break;
                }
            }
        }
        catch( final ConfigurationException ce )
        {
            throw new DeploymentException( "Malformed taskdefs.xml", ce );
        }
        catch( final Exception e )
        {
            throw new DeploymentException( "Failed to deploy " + name, e );
        }
    }

    public void deployTask( final String name, final String location, final URL url )
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
                    final DefaultComponentFactory factory =
                        new DefaultComponentFactory( new URL[] { url } );
                    handleTask( tasks[ i ], url, factory );
                    break;
                }
            }
        }
        catch( final ConfigurationException ce )
        {
            throw new DeploymentException( "Malformed taskdefs.xml", ce );
        }
        catch( final Exception e )
        {
            throw new DeploymentException( "Failed to deploy " + name, e );
        }
    }

    private void handleConverter( final Configuration converter,
                                  final URL url,
                                  final DefaultComponentFactory factory )
        throws Exception
    {
        final String name = converter.getAttribute( "classname" );
        final String source = converter.getAttribute( "source" );
        final String destination = converter.getAttribute( "destination" );

        final ConverterInfo info = new ConverterInfo( source, destination );
        m_converterInfoRegistry.registerConverterInfo( name, info );

        factory.addNameClassMapping( name, name );
        m_typeManager.registerType( Converter.ROLE, name, factory );

        getLogger().debug( "Registered converter " + name + " that converts from " +
                           source + " to " + destination );
    }

    private void handleTask( final Configuration task,
                             final URL url,
                             final DefaultComponentFactory factory )
        throws Exception
    {
        final String name = task.getAttribute( "name" );
        final String className = task.getAttribute( "classname" );

        factory.addNameClassMapping( name, className );

        m_typeManager.registerType( Task.ROLE, name, factory );

        getLogger().debug( "Registered task " + name + " as " + className );
    }

    private void handleDataType( final Configuration datatype,
                                 final URL url,
                                 final DefaultComponentFactory factory )
        throws Exception
    {
        final String name = datatype.getAttribute( "name" );
        final String className = datatype.getAttribute( "classname" );

        factory.addNameClassMapping( name, className );
        m_typeManager.registerType( "org.apache.ant.tasklet.DataType", name, factory );

        getLogger().debug( "Registered datatype " + name + " as " + className );
    }
}
