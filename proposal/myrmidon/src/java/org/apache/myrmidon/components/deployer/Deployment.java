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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.SAXConfigurationHandler;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.myrmidon.converter.Converter;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;
import org.apache.myrmidon.interfaces.deployer.ConverterDefinition;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.deployer.TypeDefinition;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.xml.sax.XMLReader;

/**
 * This class deploys type libraries from a ClassLoader into a registry.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class Deployment
    extends AbstractLogEnabled
    implements TypeDeployer
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( Deployment.class );

    private final static String DESCRIPTOR_NAME = "META-INF/ant-descriptor.xml";
    private final static String ROLE_DESCRIPTOR_NAME = "META-INF/ant-roles.xml";

    private ClassLoader m_classLoader;
    private DefaultDeployer m_deployer;
    private String[] m_descriptorUrls;
    private Configuration[] m_descriptors;

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
     */
    public void loadDescriptors( final URL jarUrl )
        throws Exception
    {
        final ArrayList descriptors = new ArrayList();

        // Create a SAX parser to assemble the descriptors into Configuration
        // objects
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        final SAXParser saxParser = saxParserFactory.newSAXParser();
        final XMLReader parser = saxParser.getXMLReader();
        //parser.setFeature( "http://xml.org/sax/features/namespace-prefixes", false );

        final SAXConfigurationHandler handler = new SAXConfigurationHandler();
        parser.setContentHandler( handler );
        parser.setErrorHandler( handler );

        // Load the role descriptors, and deploy all roles
        final List roleUrls = locateResources( ROLE_DESCRIPTOR_NAME, jarUrl );
        for( Iterator iterator = roleUrls.iterator(); iterator.hasNext(); )
        {
            String url = (String)iterator.next();
            try
            {
                parser.parse( url );
            }
            catch( FileNotFoundException e )
            {
                // Ignore - this happens when jarUrl != null and the Jar does
                // not contain a role descriptor.
                continue;
            }

            handleRoleDescriptor( handler.getConfiguration(), url );
        }

        // Load type descriptors
        final List typeUrls = locateResources( DESCRIPTOR_NAME, jarUrl );
        for( Iterator iterator = typeUrls.iterator(); iterator.hasNext(); )
        {
            String url = (String)iterator.next();
            try
            {
                parser.parse( url.toString() );
            }
            catch( FileNotFoundException e )
            {
                // Ignore - this happens when jarUrl != null and the Jar does
                // not contain a type descriptor
                continue;
            }

            descriptors.add( handler.getConfiguration() );
        }
        m_descriptorUrls = (String[])typeUrls.toArray( new String[ typeUrls.size() ] );
        m_descriptors = (Configuration[])descriptors.toArray( new Configuration[ descriptors.size() ] );
    }

    /**
     * Deploys everything in the type library.
     */
    public void deployAll()
        throws DeploymentException
    {
        for( int i = 0; i < m_descriptors.length; i++ )
        {
            Configuration descriptor = m_descriptors[ i ];
            deployFromDescriptor( descriptor, m_descriptorUrls[ i ] );
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
            // Locate the entry for the type
            for( int i = 0; i < m_descriptors.length; i++ )
            {
                Configuration descriptor = m_descriptors[ i ];
                final Configuration[] datatypes =
                    descriptor.getChild( "types" ).getChildren( roleShorthand );
                for( int j = 0; j < datatypes.length; j++ )
                {
                    Configuration datatype = datatypes[ j ];
                    if( datatype.getAttribute( "name" ).equals( typeName ) )
                    {
                        final TypeDefinition typeDef = m_deployer.createTypeDefinition( datatype );
                        m_deployer.handleType( this, typeDef );
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
        final String typeName = typeDef.getName();
        final String roleShorthand = typeDef.getRole();
        try
        {
            m_deployer.handleType( this, typeDef );
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "deploy-type.error", roleShorthand, typeName );
            throw new DeploymentException( message, e );
        }
    }

    /**
     * Locates all resources of a particular name.
     */
    private List locateResources( final String resource, final URL jarUrl )
        throws Exception
    {
        final ArrayList urls = new ArrayList();
        if( null != jarUrl )
        {
            final String systemID = "jar:" + jarUrl + "!/" + resource;
            urls.add( systemID );
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
     * Configure RoleManager based on contents of single descriptor.
     *
     * @param descriptor the descriptor
     * @exception ConfigurationException if an error occurs
     */
    private void handleRoleDescriptor( final Configuration descriptor,
                                       final String url )
        throws ConfigurationException
    {
        final String message = REZ.getString( "url-deploy-roles.notice", url );
        getLogger().info( message );

        final Configuration[] types = descriptor.getChildren( "role" );
        for( int i = 0; i < types.length; i++ )
        {
            final String name = types[ i ].getAttribute( "shorthand" );
            final String role = types[ i ].getAttribute( "name" );
            final RoleDefinition roleDef = new RoleDefinition( role, name );
            m_deployer.handleRole( this, roleDef );
        }
    }

    /**
     * Deploys all types from a typelib descriptor.
     */
    private void deployFromDescriptor( final Configuration descriptor,
                                       final String url )
        throws DeploymentException
    {
        try
        {
            final String message = REZ.getString( "url-deploy-types.notice", url );
            getLogger().info( message );

            // Deploy all the types
            final Configuration[] typeEntries = descriptor.getChild( "types" ).getChildren();
            for( int i = 0; i < typeEntries.length; i++ )
            {
                final Configuration typeEntry = typeEntries[ i ];
                final TypeDefinition typeDef = m_deployer.createTypeDefinition( typeEntry );
                m_deployer.handleType( this, typeDef );
            }
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "deploy-lib.error", url );
            throw new DeploymentException( message, e );
        }
    }

    /**
     * Returns the type factory for a role.
     */
    public DefaultTypeFactory getFactory( final Class roleType )
    {
        DefaultTypeFactory factory = (DefaultTypeFactory)m_factories.get( roleType );

        if( null == factory )
        {
            factory = new DefaultTypeFactory( m_classLoader );
            m_factories.put( roleType, factory );
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
}
