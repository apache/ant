/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.deployer;

import java.io.File;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.myrmidon.components.converter.ConverterRegistry;
import org.apache.myrmidon.components.executor.Executor;
import org.apache.myrmidon.components.type.DefaultTypeFactory;
import org.apache.myrmidon.components.type.TypeManager;
import org.apache.myrmidon.converter.Converter;
import org.apache.myrmidon.api.DataType;
import org.apache.myrmidon.api.Task;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultDeployer
    extends AbstractLoggable
    implements Composable, Deployer
{
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

        try
        {
            checkFile( file );

            final Deployment deployment = new Deployment( file );
            final Configuration descriptor = deployment.getDescriptor();
            final DefaultTypeFactory factory = new DefaultTypeFactory( deployment.getURL() );

            final Configuration[] children = descriptor.getChildren();
            for( int i = 0; i < children.length; i++ )
            {
                final String name = children[ i ].getName();
                
                if( name.equals( "converter" ) )
                {
                    handleConverter( children[ i ], factory );
                }
                else
                {
                    final String role = getRoleForName( name );
                    handleType( role, children[ i ], factory );
                }
            }
        }
        catch( final DeploymentException de )
        {
            throw de;
        }
        catch( final Exception e )
        {
            throw new DeploymentException( "Error deploying library", e );
        }       
    }

    public void deployConverter( final String name, final File file )
        throws DeploymentException
    {
        checkFile( file );

        final Deployment deployment = new Deployment( file );
        final Configuration descriptor = deployment.getDescriptor();
        final DefaultTypeFactory factory = new DefaultTypeFactory( deployment.getURL() );

        try
        {
            final Configuration[] converters = descriptor.getChildren( "converter" );
            for( int i = 0; i < converters.length; i++ )
            {
                if( converters[ i ].getAttribute( "classname" ).equals( name ) )
                {
                    handleConverter( converters[ i ], factory );
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

    public void deployType( final String role, final String name, final File file )
        throws DeploymentException
    {
        checkFile( file );

        final String shorthand = getNameForRole( role );
        final Deployment deployment = new Deployment( file );
        final Configuration descriptor = deployment.getDescriptor();
        final DefaultTypeFactory factory = new DefaultTypeFactory( deployment.getURL() );
        
        try
        {
            final Configuration[] datatypes = descriptor.getChildren( shorthand );
            for( int i = 0; i < datatypes.length; i++ )
            {
                if( datatypes[ i ].getAttribute( "name" ).equals( name ) )
                {
                    handleType( role, datatypes[ i ], factory );
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

    private String getNameForRole( final String role )
        throws DeploymentException
    {
        final String name = m_roleManager.getNameForRole( role );

        if( null == name )
        {
            throw new DeploymentException( "RoleManager does not know name for role " + role );
        }
        
        return name;
    }

    private String getRoleForName( final String name )
        throws DeploymentException
    {
        final String role = m_roleManager.getRoleForName( name );

        if( null == role )
        {
            throw new DeploymentException( "RoleManager does not know role for name " + name );
        }
        
        return role;
    }

    private void checkFile( final File file )
        throws DeploymentException
    {
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
    }

    private void handleConverter( final Configuration converter,
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

    private void handleType( final String role,
                             final Configuration type,
                             final DefaultTypeFactory factory )
        throws Exception
    {
        final String name = type.getAttribute( "name" );
        final String className = type.getAttribute( "classname" );

        factory.addNameClassMapping( name, className );
        m_typeManager.registerType( role, name, factory );

        getLogger().debug( "Registered " + role + "/" + name + " as " + className );
    }
}
