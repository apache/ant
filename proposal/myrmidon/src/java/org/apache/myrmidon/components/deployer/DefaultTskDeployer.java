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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.myrmidon.api.Task;
import org.apache.myrmidon.api.DataType;
import org.apache.myrmidon.components.converter.ConverterRegistry;
import org.apache.myrmidon.components.executor.Executor;
import org.apache.myrmidon.components.type.DefaultTypeFactory;
import org.apache.myrmidon.components.type.TypeManager;
import org.apache.myrmidon.converter.Converter;
import org.xml.sax.SAXException;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultTskDeployer
    extends AbstractLoggable
    implements Composable, TskDeployer
{
    private final static String   TSKDEF_FILE     = "TASK-LIB/taskdefs.xml";

    private DefaultConfigurationBuilder  m_configurationBuilder = new DefaultConfigurationBuilder();
    private ConverterRegistry            m_converterRegistry;
    private TypeManager                  m_typeManager;
    private RoleManager                  m_roleManager;

    /**
     * Retrieve relevent services needed to deploy.
     *
     * @param componentManager the ComponentManager
     * @exception ComponentException if an error occurs
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        m_converterRegistry = (ConverterRegistry)componentManager.lookup( ConverterRegistry.ROLE );
        m_typeManager = (TypeManager)componentManager.lookup( TypeManager.ROLE );
        m_roleManager = (RoleManager)componentManager.lookup( RoleManager.ROLE );
    }

    public void deploy( final File file )
        throws DeploymentException
    {
        getLogger().info( "Deploying AntLib file (" + file + ")" );

        final URL url = getURL( file );

        final ZipFile zipFile = getZipFileFor( file );
        try
        {
            loadResources( zipFile, url );
        }
        catch( final DeploymentException de )
        {
            throw de;
        }
        catch( final Exception e )
        {
            throw new DeploymentException( "Error deploying library", e );
        }       
        finally
        {
            try { zipFile.close(); }
            catch( final IOException ioe ) {}
        }
    }

    private void loadResources( final ZipFile zipFile, final URL url )
        throws Exception
    {
        final Configuration taskdefs = getDescriptor( zipFile );
        final DefaultTypeFactory factory = new DefaultTypeFactory( new URL[] { url } );

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
    }

    public void deployConverter( final String name, final File file )
        throws DeploymentException
    {
        final ZipFile zipFile = getZipFileFor( file );
        final Configuration taskdefs = getDescriptor( zipFile );

        try
        {
            final Configuration[] converters = taskdefs.getChildren( "converter" );
            for( int i = 0; i < converters.length; i++ )
            {
                if( converters[ i ].getAttribute( "classname" ).equals( name ) )
                {
                    final URL url = getURL( file );
                    final DefaultTypeFactory factory = new DefaultTypeFactory( new URL[] { url } );
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

    public void deployDataType( final String name, final File file )
        throws DeploymentException
    {
        final ZipFile zipFile = getZipFileFor( file );
        final Configuration datatypedefs = getDescriptor( zipFile );

        try
        {
            final Configuration[] datatypes = datatypedefs.getChildren( "datatype" );
            for( int i = 0; i < datatypes.length; i++ )
            {
                if( datatypes[ i ].getAttribute( "name" ).equals( name ) )
                {
                    final URL url = getURL( file );
                    final DefaultTypeFactory factory = new DefaultTypeFactory( new URL[] { url } );
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

    public void deployTask( final String name, final File file )
        throws DeploymentException
    {
        final ZipFile zipFile = getZipFileFor( file );
        final Configuration taskdefs = getDescriptor( zipFile );

        try
        {
            final Configuration[] tasks = taskdefs.getChildren( "task" );
            for( int i = 0; i < tasks.length; i++ )
            {
                if( tasks[ i ].getAttribute( "name" ).equals( name ) )
                {
                    final URL url = getURL( file );
                    final DefaultTypeFactory factory = new DefaultTypeFactory( new URL[] { url } );
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

    /**
     * Retrieve zip file for file.
     *
     * @param file the file
     * @return the zipFile
     * @exception DeploymentException if an error occurs
     */
    private ZipFile getZipFileFor( final File file )
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
     * Utility method to load configuration from zip.
     *
     * @param zipFile the zip file
     * @param filename the property filename
     * @return the Configuration
     * @exception DeploymentException if an error occurs
     */
    private Configuration getDescriptor( final ZipFile zipFile )
        throws DeploymentException
    {
        return buildConfiguration( loadResourceStream( zipFile, TSKDEF_FILE ) );
    }

    /**
     * Build a configuration tree based on input stream.
     *
     * @param input the InputStream
     * @return the Configuration tree
     * @exception DeploymentException if an error occurs
     */
    private Configuration buildConfiguration( final InputStream input )
        throws DeploymentException
    {
        try { return m_configurationBuilder.build( input ); }
        catch( final SAXException se )
        {
            throw new DeploymentException( "Malformed configuration data", se );
        }
        catch( final ConfigurationException ce )
        {
            throw new DeploymentException( "Error building configuration", ce );
        }
        catch( final IOException ioe )
        {
            throw new DeploymentException( "Error reading configuration", ioe );
        }
    }

    private File getFileFor( final URL url )
        throws DeploymentException
    {
        File file = null;

        if( url.getProtocol().equals( "file" ) )
        {
            file = new File( url.getFile() );
        }
        else
        {
            throw new DeploymentException( "Currently unable to deploy non-local " +
                                           "archives (" + url + ")" );
        }

        file = file.getAbsoluteFile();

        if( !file.exists() )
        {
            throw new DeploymentException( "Could not find application archive at " +
                                           file );
        }

        if( file.isDirectory() )
        {
            throw new DeploymentException( "Could not find application archive at " +
                                           file + " as it is a directory." );
        }

        return file;
    }

    private void handleConverter( final Configuration converter,
                                  final URL url,
                                  final DefaultTypeFactory factory )
        throws Exception
    {
        final String name = converter.getAttribute( "classname" );
        final String source = converter.getAttribute( "source" );
        final String destination = converter.getAttribute( "destination" );

        m_converterRegistry.registerConverter( name, source, destination );

        factory.addNameClassMapping( name, name );
        m_typeManager.registerType( Converter.ROLE, name, factory );

        getLogger().debug( "Registered converter " + name + " that converts from " +
                           source + " to " + destination );
    }

    private void handleTask( final Configuration task,
                             final URL url,
                             final DefaultTypeFactory factory )
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
                                 final DefaultTypeFactory factory )
        throws Exception
    {
        final String name = datatype.getAttribute( "name" );
        final String className = datatype.getAttribute( "classname" );

        factory.addNameClassMapping( name, className );
        m_typeManager.registerType( DataType.ROLE, name, factory );

        getLogger().debug( "Registered datatype " + name + " as " + className );
    }


    /**
     * Load a resource from a zip file.
     *
     * @param zipFile the ZipFile
     * @param filename the filename
     * @return the InputStream
     * @exception DeploymentException if an error occurs
     */
    private InputStream loadResourceStream( final ZipFile zipFile, final String filename )
        throws DeploymentException
    {
        final ZipEntry entry = zipFile.getEntry( filename );

        if( null == entry )
        {
            throw new DeploymentException( "Unable to locate " + filename +
                                           " in " + zipFile.getName() );
        }

        try { return zipFile.getInputStream( entry ); }
        catch( final IOException ioe )
        {
            throw new DeploymentException( "Error reading " + filename +
                                           " from " + zipFile.getName(),
                                           ioe );
        }
    }

    private URL getURL( final File file )
        throws DeploymentException
    {
        try { return file.toURL(); }
        catch( final MalformedURLException mue )
        {
            throw new DeploymentException( "Unable to form url", mue );
        }
    }
}
