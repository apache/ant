/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;
import org.apache.myrmidon.interfaces.deployer.TypeDefinition;
import org.apache.myrmidon.interfaces.deployer.ConverterDefinition;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.converter.Converter;

/**
 * This class deploys roles, types and services from a typelib.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultDeployer
    extends AbstractLogEnabled
    implements Deployer, Composable
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultDeployer.class );

    // The components used to deploy
    private ConverterRegistry m_converterRegistry;
    private TypeManager m_typeManager;
    private RoleManager m_roleManager;
    private ClassLoaderManager m_classLoaderManager;

    /** Map from ClassLoader to the deployer for that class loader. */
    private final Map m_classLoaderDeployers = new HashMap();

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
        m_classLoaderManager = (ClassLoaderManager)componentManager.lookup( ClassLoaderManager.ROLE );
    }

    /**
     * Creates a child deployer.
     */
    public Deployer createChildDeployer( ComponentManager componentManager )
        throws ComponentException
    {
        final DefaultDeployer child = new DefaultDeployer( );
        setupLogger( child );
        child.compose( componentManager );
        return child;
    }

    /**
     * Returns the deployer for a ClassLoader, creating the deployer if
     * necessary.
     */
    public TypeDeployer createDeployer( final ClassLoader loader )
        throws DeploymentException
    {
        try
        {
            return createDeployment( loader, null );
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "deploy-from-classloader.error", loader );
            throw new DeploymentException( message, e );
        }
    }

    /**
     * Returns the deployer for a type library, creating the deployer if
     * necessary.
     */
    public TypeDeployer createDeployer( final File file )
        throws DeploymentException
    {
        try
        {
            final ClassLoader classLoader = m_classLoaderManager.createClassLoader( file );
            return createDeployment( classLoader, file.toURL() );
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "deploy-from-file.error", file );
            throw new DeploymentException( message, e );
        }
    }

    /**
     * Creates a deployer for a ClassLoader.
     */
    private Deployment createDeployment( final ClassLoader loader,
                                         final URL jarUrl )
        throws Exception
    {
        // Locate cached deployer, creating it if necessary
        Deployment deployment = (Deployment)m_classLoaderDeployers.get( loader );
        if( deployment == null )
        {
            deployment = new Deployment( this, loader );
            setupLogger( deployment );
            deployment.loadDescriptors( jarUrl );
            m_classLoaderDeployers.put( loader, deployment );
        }

        return deployment;
    }

    /**
     * Creates a type definition.
     */
    public TypeDefinition createTypeDefinition( final Configuration configuration )
        throws ConfigurationException
    {
        final String converterShorthand = m_roleManager.getNameForRole( Converter.ROLE );
        final String roleShorthand = configuration.getName();
        if( roleShorthand.equals( converterShorthand ) )
        {
            // A converter definition
            final String className = configuration.getAttribute( "classname" );
            final String source = configuration.getAttribute( "source" );
            final String destination = configuration.getAttribute( "destination" );
            return new ConverterDefinition( className, source, destination );
        }
        else
        {
            // A type definition
            final String typeName = configuration.getAttribute( "name" );
            final String className = configuration.getAttribute( "classname" );
            return new TypeDefinition( typeName, roleShorthand, className );
        }
    }

    /**
     * Handles a converter definition.
     */
    private void handleConverter( final Deployment deployment,
                                  final String className,
                                  final String source,
                                  final String destination )
        throws Exception
    {
        m_converterRegistry.registerConverter( className, source, destination );
        final DefaultTypeFactory factory = deployment.getFactory( Converter.class );
        factory.addNameClassMapping( className, className );
        m_typeManager.registerType( Converter.class, className, factory );

        if( getLogger().isDebugEnabled() )
        {
            final String message =
                REZ.getString( "register-converter.notice", source, destination );
            getLogger().debug( message );
        }
    }

    /**
     * Handles a type definition.
     */
    public void handleType( final Deployment deployment,
                            final TypeDefinition typeDef )
        throws Exception
    {
        final String typeName = typeDef.getName();
        final String roleShorthand = typeDef.getRole();

        final String className = typeDef.getClassname();
        if( null == className )
        {
            final String message = REZ.getString( "typedef.no-classname.error" );
            throw new DeploymentException( message );
        }

        if( typeDef instanceof ConverterDefinition )
        {
            // Validate the definition
            final ConverterDefinition converterDef = (ConverterDefinition)typeDef;
            final String srcClass = converterDef.getSourceType();
            final String destClass = converterDef.getDestinationType();
            if( null == srcClass )
            {
                final String message = REZ.getString( "converterdef.no-source.error" );
                throw new DeploymentException( message );
            }
            if( null == destClass )
            {
                final String message = REZ.getString( "converterdef.no-destination.error" );
                throw new DeploymentException( message );
            }

            // Deploy the converter
            handleConverter( deployment, className, srcClass, destClass );
        }
        else
        {
            // Validate the definition
            if( null == roleShorthand )
            {
                final String message = REZ.getString( "typedef.no-role.error" );
                throw new DeploymentException( message );
            }
            else if( null == typeName )
            {
                final String message = REZ.getString( "typedef.no-name.error" );
                throw new DeploymentException( message );
            }

            // Deploy general-purpose type
            handleType( deployment, roleShorthand, typeName, className );
        }
    }

    /**
     * Handles a type definition.
     */
    private void handleType( final Deployment deployment,
                             final String roleShorthand,
                             final String typeName,
                             final String className )
        throws Exception
    {
        // TODO - detect duplicates
        final String role = getRoleForName( roleShorthand );
        final Class roleType = deployment.getClassLoader().loadClass( role );
        final DefaultTypeFactory factory = deployment.getFactory( roleType );
        factory.addNameClassMapping( typeName, className );
        m_typeManager.registerType( roleType, typeName, factory );

        if( getLogger().isDebugEnabled() )
        {
            final String message =
                REZ.getString( "register-type.notice", roleShorthand, typeName );
            getLogger().debug( message );
        }
    }

    /**
     * Handles a role definition.
     */
    public void handleRole( final Deployment deployment,
                            final RoleDefinition roleDef )
    {
        final String name = roleDef.getShortHand();
        final String role = roleDef.getRoleName();
        m_roleManager.addNameRoleMapping( name, role );

        if( getLogger().isDebugEnabled() )
        {
            final String debugMessage = REZ.getString( "register-role.notice", role, name );
            getLogger().debug( debugMessage );
        }
    }

    /**
     * Determines the role name from shorthand name.
     */
    private String getRoleForName( final String name )
        throws DeploymentException
    {
        final String role = m_roleManager.getRoleForName( name );

        if( null == role )
        {
            final String message = REZ.getString( "unknown-role4name.error", name );
            throw new DeploymentException( message );
        }

        return role;
    }

}
