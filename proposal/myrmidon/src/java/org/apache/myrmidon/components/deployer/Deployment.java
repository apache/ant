/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.SAXConfigurationHandler;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.deployer.TypeDefinition;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.xml.sax.XMLReader;

/**
 * This class deploys type libraries from a ClassLoader into a registry.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
class Deployment
    extends AbstractLogEnabled
    implements TypeDeployer
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( Deployment.class );

    private static final String TYPE_DESCRIPTOR_NAME = "META-INF/ant-descriptor.xml";
    private static final String ROLE_DESCRIPTOR_NAME = "META-INF/ant-roles.xml";
    private static final String SERVICE_DESCRIPTOR_NAME = "META-INF/ant-services.xml";

    private ClassLoader m_classLoader;
    private DefaultDeployer m_deployer;
    private TypeDescriptor[] m_descriptors;
    private ServiceDescriptor[] m_services;

    // TODO - create and configure these in DefaultDeployer
    private DescriptorBuilder m_roleBuilder = new RoleDescriptorBuilder();
    private DescriptorBuilder m_typeBuilder = new TypeDescriptorBuilder();
    private DescriptorBuilder m_serviceBuilder = new ServiceDescriptorBuilder();

    /** Map from role Class -> DefaultTypeFactory for that role. */
    private Map m_factories = new HashMap();

    public Deployment( final DefaultDeployer deployer, final ClassLoader classLoader )
    {
        m_deployer = deployer;
        m_classLoader = classLoader;
    }

    /**
     * Load the descriptors.  Deploys all roles, then loads the descriptors
     * for, but does not deploy, all the types.
     *
     * @param jarUrl The URL for the typelib, used to locate the descriptors.
     *               If null, the resources from the classloader are used.
     */
    public void loadDescriptors( final URL jarUrl )
        throws Exception
    {
        // Create a SAX parser to assemble the descriptors into Configuration
        // objects
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        final SAXParser saxParser = saxParserFactory.newSAXParser();
        final XMLReader parser = saxParser.getXMLReader();
        //parser.setFeature( "http://xml.org/sax/features/namespace-prefixes", false );

        final SAXConfigurationHandler handler = new SAXConfigurationHandler();
        parser.setContentHandler( handler );
        parser.setErrorHandler( handler );

        // Build the role descriptors
        final ArrayList roleUrls = locateResources( ROLE_DESCRIPTOR_NAME, jarUrl );
        final ArrayList roleDescriptors =
            buildDescriptors( roleUrls, m_roleBuilder, parser, handler );

        // Deploy the roles
        // TODO - need to defer this
        final int roleCount = roleDescriptors.size();
        for( int i = 0; i < roleCount; i++ )
        {
            final RoleDescriptor descriptor = (RoleDescriptor)roleDescriptors.get( i );
            deployRoles( descriptor );
        }

        // Build the type descriptors
        final ArrayList typeUrls = locateResources( TYPE_DESCRIPTOR_NAME, jarUrl );
        final ArrayList typeDescriptors =
            buildDescriptors( typeUrls, m_typeBuilder, parser, handler );
        m_descriptors = (TypeDescriptor[])typeDescriptors.toArray
            ( new TypeDescriptor[ typeDescriptors.size() ] );

        // Build the service descriptors
        final ArrayList serviceUrls = locateResources( SERVICE_DESCRIPTOR_NAME, jarUrl );
        final ArrayList serviceDescriptors =
            buildDescriptors( serviceUrls, m_serviceBuilder, parser, handler );
        m_services = (ServiceDescriptor[])serviceDescriptors.toArray
            ( new ServiceDescriptor[ serviceDescriptors.size() ] );
    }

    /**
     * Returns the type factory for a role.
     */
    public DefaultTypeFactory getFactory( final String roleName )
    {
        DefaultTypeFactory factory = (DefaultTypeFactory)m_factories.get( roleName );

        if( null == factory )
        {
            factory = new DefaultTypeFactory( m_classLoader );
            m_factories.put( roleName, factory );
        }

        return factory;
    }

    /**
     * Returns the classloader for this deployment.
     */
    public ClassLoader getClassLoader()
    {
        return m_classLoader;
    }

    /**
     * Deploys everything in the type library.
     */
    public void deployAll()
        throws DeploymentException
    {
        // Deploy types
        for( int i = 0; i < m_descriptors.length; i++ )
        {
            TypeDescriptor descriptor = m_descriptors[ i ];
            deployTypes( descriptor );
        }

        // Deploy services
        for( int i = 0; i < m_services.length; i++ )
        {
            final ServiceDescriptor descriptor = m_services[ i ];
            deployServices( descriptor );
        }
    }

    /**
     * Deploys a single type in the type library.
     */
    public void deployType( final String roleShorthand, final String typeName )
        throws DeploymentException
    {
        try
        {
            // Locate the definition for the type
            for( int i = 0; i < m_descriptors.length; i++ )
            {
                final TypeDescriptor descriptor = m_descriptors[ i ];
                final TypeDefinition[] definitions = descriptor.getDefinitions();
                for( int j = 0; j < definitions.length; j++ )
                {
                    TypeDefinition definition = definitions[ j ];
                    if( definition.getRole().equals( roleShorthand )
                        && definition.getName().equals( typeName ) )
                    {
                        // Found the definition - deploy it.  Note that we
                        // keep looking for matching types, and let the deployer
                        // deal with duplicates
                        m_deployer.deployType( this, definition );
                    }
                }
            }
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "deploy-type.error", roleShorthand, typeName );
            throw new DeploymentException( message, e );
        }
    }

    /**
     * Deploys a single type from the type library.
     */
    public void deployType( final TypeDefinition typeDef )
        throws DeploymentException
    {
        try
        {
            m_deployer.deployType( this, typeDef );
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "deploy-type.error",
                                                  typeDef.getRole(), typeDef.getName() );
            throw new DeploymentException( message, e );
        }
    }

    /**
     * Builds descriptors.
     */
    private ArrayList buildDescriptors( final ArrayList urls,
                                        final DescriptorBuilder builder,
                                        final XMLReader parser,
                                        final SAXConfigurationHandler handler )
        throws Exception
    {
        final ArrayList descriptors = new ArrayList();
        final int size = urls.size();
        for( int i = 0; i < size; i++ )
        {
            final String url = (String)urls.get( i );

            // Parse the file
            parser.parse( url );
            final TypelibDescriptor descriptor =
                builder.createDescriptor( handler.getConfiguration(), url );
            descriptors.add( descriptor );
        }

        return descriptors;
    }

    /**
     * Locates all resources of a particular name.
     */
    private ArrayList locateResources( final String resource, final URL jarUrl )
        throws Exception
    {
        final ArrayList urls = new ArrayList();
        if( null != jarUrl )
        {
            final String systemID = "jar:" + jarUrl + "!/" + resource;
            try
            {
                // Probe the resource
                final URL url = new URL( systemID );
                url.openStream().close();

                // Add to the list
                urls.add( systemID );
            }
            catch( FileNotFoundException e )
            {
                // Ignore
            }
        }
        else
        {
            final Enumeration enum = m_classLoader.getResources( resource );
            while( enum.hasMoreElements() )
            {
                urls.add( enum.nextElement().toString() );
            }
        }

        return urls;
    }

    /**
     * Deploys the roles from a role descriptor.
     */
    private void deployRoles( final RoleDescriptor descriptor )
        throws DeploymentException
    {

        try
        {
            if( getLogger().isDebugEnabled() )
            {
                final String message =
                    REZ.getString( "url-deploy-roles.notice", descriptor.getUrl() );
                getLogger().debug( message );
            }

            final RoleDefinition[] definitions = descriptor.getDefinitions();
            for( int i = 0; i < definitions.length; i++ )
            {
                final RoleDefinition definition = definitions[ i ];
                m_deployer.deployRole( this, definition );
            }
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "deploy-roles.error", descriptor.getUrl() );
            throw new DeploymentException( message, e );
        }
    }

    /**
     * Deploys all types from a typelib descriptor.
     */
    private void deployTypes( final TypeDescriptor descriptor )
        throws DeploymentException
    {
        try
        {
            if( getLogger().isDebugEnabled() )
            {
                final String message =
                    REZ.getString( "url-deploy-types.notice", descriptor.getUrl() );
                getLogger().debug( message );
            }

            // Deploy all the types
            final TypeDefinition[] definitions = descriptor.getDefinitions();
            for( int i = 0; i < definitions.length; i++ )
            {
                final TypeDefinition definition = definitions[ i ];
                m_deployer.deployType( this, definition );
            }
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "deploy-types.error", descriptor.getUrl() );
            throw new DeploymentException( message, e );
        }
    }

    /**
     * Deploys all services from a typelib descriptor.
     */
    private void deployServices( final ServiceDescriptor descriptor )
        throws DeploymentException
    {

        try
        {
            if( getLogger().isDebugEnabled() )
            {
                final String message =
                    REZ.getString( "url-deploy-services.notice", descriptor.getUrl() );
                getLogger().debug( message );
            }

            // Deploy the services
            final ServiceDefinition[] definitions = descriptor.getDefinitions();
            for( int i = 0; i < definitions.length; i++ )
            {
                final ServiceDefinition definition = definitions[ i ];
                m_deployer.deployService( this, definition );
            }
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "deploy-services.error", descriptor.getUrl() );
            throw new DeploymentException( message, e );
        }
    }
}
