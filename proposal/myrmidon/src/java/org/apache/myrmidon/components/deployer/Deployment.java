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
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.xml.sax.XMLReader;

/**
 * This class deploys type libraries from a ClassLoader into a registry.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
class Deployment
    extends AbstractLogEnabled
    implements TypeDeployer
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( Deployment.class );

    private final static String DESCRIPTOR_NAME = "META-INF/ant-descriptor.xml";
    private final static String ROLE_DESCRIPTOR = "META-INF/ant-roles.xml";

    private ClassLoader m_classLoader;
    private ConverterRegistry m_converterRegistry;
    private TypeManager m_typeManager;
    private RoleManager m_roleManager;
    private String[] m_descriptorUrls;
    private Configuration[] m_descriptors;
    private DefaultTypeFactory m_converterFactory;

    /** Map from role name -> DefaultTypeFactory for that role. */
    private Map m_factories = new HashMap();

    public Deployment( final ClassLoader classLoader, ComponentManager manager )
        throws ComponentException
    {
        // Locate the various components needed
        m_classLoader = classLoader;
        m_converterRegistry = (ConverterRegistry)manager.lookup( ConverterRegistry.ROLE );
        m_typeManager = (TypeManager)manager.lookup( TypeManager.ROLE );
        m_roleManager = (RoleManager)manager.lookup( RoleManager.ROLE );
    }

    /**
     * Load the descriptors.  Deploys all roles, then loads the descriptors
     * for, but does not deploy, all the types.
     */
    public void loadDescriptors( URL jarUrl )
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
        final List roleUrls = locateResources( ROLE_DESCRIPTOR, jarUrl );
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
            deployFromDescriptor( descriptor, m_classLoader, m_descriptorUrls[i] );
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
                        final String className = datatype.getAttribute( "classname" );
                        handleType( roleShorthand, typeName, className );
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
    public void deployType( String roleShorthand, String typeName, String className )
        throws DeploymentException
    {
        try
        {
            handleType( roleShorthand, typeName, className );
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "deploy-type.error", roleShorthand, typeName );
            throw new DeploymentException( message, e );
        }
    }

    /**
     * Deploys a converter from the type library.  The converter definition
     * is read from the type library descriptor.
     */
    public void deployConverter( String className )
        throws DeploymentException
    {
        // TODO - implement this
        throw new DeploymentException( "Not implemented." );
    }

    /**
     * Deploys a converter from the type library.
     */
    public void deployConverter( String className, String srcClass, String destClass )
        throws DeploymentException
    {
        try
        {
            handleConverter( className, srcClass, destClass );
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "deploy-converter.error", srcClass, destClass );
            throw new DeploymentException( message, e );
        }
    }

    /**
     * Locates all resources of a particular name.
     */
    private List locateResources( final String resource, final URL jarUrl )
        throws Exception
    {
        ArrayList urls = new ArrayList();
        if( jarUrl != null )
        {
            final String systemID = "jar:" + jarUrl + "!/" + resource;
            urls.add( systemID );
        }
        else
        {
            Enumeration enum = m_classLoader.getResources( resource );
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
            m_roleManager.addNameRoleMapping( name, role );

            if( getLogger().isDebugEnabled() )
            {
                final String debugMessage = REZ.getString( "register-role.notice", role, name );
                getLogger().debug( debugMessage );
            }
        }
    }

    /**
     * Deploys all types from a typelib descriptor.
     */
    private void deployFromDescriptor( final Configuration descriptor,
                                       final ClassLoader classLoader,
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
                final String roleShorthand = typeEntry.getName();
                final String typeName = typeEntry.getAttribute( "name" );
                final String className = typeEntry.getAttribute( "classname" );
                handleType( roleShorthand, typeName, className );
            }

            // Deploy all the converters
            final Configuration[] converterEntries = descriptor.getChild( "converters" ).getChildren();
            for( int i = 0; i < converterEntries.length; i++ )
            {
                final Configuration converter = converterEntries[ i ];
                final String className = converter.getAttribute( "classname" );
                final String source = converter.getAttribute( "source" );
                final String destination = converter.getAttribute( "destination" );
                handleConverter( className, source, destination );
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
    private DefaultTypeFactory getFactory( final Class roleType)
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
     * Handles a converter definition.
     */
    private void handleConverter( final String className,
                                  final String source,
                                  final String destination ) throws Exception
    {
        m_converterRegistry.registerConverter( className, source, destination );

        if( m_converterFactory == null )
        {
            m_converterFactory = new DefaultTypeFactory( m_classLoader );
        }
        m_converterFactory.addNameClassMapping( className, className );
        m_typeManager.registerType( Converter.class, className, m_converterFactory );

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
    private void handleType( final String roleShorthand,
                             final String typeName,
                             final String className )
        throws Exception
    {
        // TODO - detect duplicates
        final String role = getRoleForName( roleShorthand );
        final Class roleType = m_classLoader.loadClass( role );
        final DefaultTypeFactory factory = getFactory( roleType );
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
