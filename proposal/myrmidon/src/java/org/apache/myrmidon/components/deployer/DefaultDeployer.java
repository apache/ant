/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

import java.io.File;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.Manifest;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.avalon.excalibur.extension.Extension;
import org.apache.avalon.excalibur.extension.OptionalPackage;
import org.apache.avalon.excalibur.extension.PackageManager;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.SAXConfigurationHandler;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.myrmidon.converter.Converter;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.extensions.ExtensionManager;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.xml.sax.XMLReader;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class DefaultDeployer
    extends AbstractLogEnabled
    implements Deployer, Initializable, Composable
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultDeployer.class );

    private final static String TYPE_DESCRIPTOR = "META-INF/ant-types.xml";

    private ConverterRegistry m_converterRegistry;
    private TypeManager m_typeManager;
    private RoleManager m_roleManager;
    private PackageManager m_packageManager;

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

        final ExtensionManager extensionManager =
            (ExtensionManager)componentManager.lookup( ExtensionManager.ROLE );
        m_packageManager = new PackageManager( extensionManager );
    }

    public void initialize()
        throws Exception
    {
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        final SAXParser saxParser = saxParserFactory.newSAXParser();
        final XMLReader parser = saxParser.getXMLReader();
        //parser.setFeature( "http://xml.org/sax/features/namespace-prefixes", false );

        final SAXConfigurationHandler handler = new SAXConfigurationHandler();
        parser.setContentHandler( handler );
        parser.setErrorHandler( handler );

        final ClassLoader classLoader = getClass().getClassLoader();
        final DefaultTypeFactory factory = new DefaultTypeFactory( classLoader );

        final Enumeration enum = classLoader.getResources( Deployment.DESCRIPTOR_NAME );
        while( enum.hasMoreElements() )
        {
            final URL url = (URL)enum.nextElement();
            parser.parse( url.toString() );

            final String message = REZ.getString( "url-deploy.notice", url );
            getLogger().debug( message );

            deployFromDescriptor( handler.getConfiguration(), classLoader, url );
        }
    }

    public void deploy( final File file )
        throws DeploymentException
    {
        if( getLogger().isInfoEnabled() )
        {
            final String message = REZ.getString( "file-deploy.notice", file );
            getLogger().info( message );
        }

        checkFile( file );

        try
        {
            final File[] extensions = getOptionalPackagesFor( file );
            final URL[] urls = buildClasspath( file, extensions );
            final Deployment deployment = new Deployment( file );
            final Configuration descriptor = deployment.getDescriptor();

            final URLClassLoader classLoader =
                new URLClassLoader( urls, Thread.currentThread().getContextClassLoader() );

            deployFromDescriptor( descriptor, classLoader, deployment.getURL() );
        }
        catch( final DeploymentException de )
        {
            throw de;
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "deploy-lib.error" );
            throw new DeploymentException( message, e );
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
            final Configuration[] converters =
                descriptor.getChild( "converters" ).getChildren( "converter" );

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
            final String message = REZ.getString( "bad-descriptor.error" );
            throw new DeploymentException( message, ce );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "deploy-converter.error", name );
            throw new DeploymentException( message, e );
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
            final Configuration[] datatypes =
                descriptor.getChild( "types" ).getChildren( shorthand );

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
            final String message = REZ.getString( "bad-descriptor.error" );
            throw new DeploymentException( message, ce );
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "deploy-type.error", name );
            throw new DeploymentException( message, e );
        }
    }

    private URL[] buildClasspath( final File file, final File[] dependencies )
        throws MalformedURLException
    {
        final URL[] urls = new URL[ dependencies.length + 1 ];

        for( int i = 0; i < dependencies.length; i++ )
        {
            urls[ i ] = dependencies[ i ].toURL();
        }

        urls[ dependencies.length ] = file.toURL();

        return urls;
    }

    /**
     * Retrieve the files for the optional packages required by
     * the specified typeLibrary jar.
     *
     * @param typeLibrary the typeLibrary
     * @return the files that need to be added to ClassLoader
     */
    private File[] getOptionalPackagesFor( final File typeLibrary )
        throws Exception
    {
        final URL url = new URL( "jar:" + typeLibrary.getCanonicalFile().toURL() + "!/" );
        final JarURLConnection connection = (JarURLConnection)url.openConnection();
        final Manifest manifest = connection.getManifest();
        final Extension[] available = Extension.getAvailable( manifest );
        final Extension[] required = Extension.getRequired( manifest );

        if( getLogger().isDebugEnabled() )
        {
            final String message1 =
                REZ.getString( "available-extensions", Arrays.asList( available ) );
            getLogger().debug( message1 );
            final String message2 =
                REZ.getString( "required-extensions", Arrays.asList( required ) );
            getLogger().debug( message2 );
        }

        final ArrayList dependencies = new ArrayList();
        final ArrayList unsatisfied = new ArrayList();

        m_packageManager.scanDependencies( required,
                                           available,
                                           dependencies,
                                           unsatisfied );

        if( 0 != unsatisfied.size() )
        {
            final int size = unsatisfied.size();
            for( int i = 0; i < size; i++ )
            {
                final Extension extension = (Extension)unsatisfied.get( i );
                final Object[] params = new Object[]
                {
                    extension.getExtensionName(),
                    extension.getSpecificationVendor(),
                    extension.getSpecificationVersion(),
                    extension.getImplementationVendor(),
                    extension.getImplementationVendorId(),
                    extension.getImplementationVersion(),
                    extension.getImplementationURL()
                };
                final String message = REZ.format( "missing.extension", params );
                getLogger().warn( message );
            }

            final String message =
                REZ.getString( "unsatisfied.extensions", new Integer( size ) );
            throw new Exception( message );
        }

        final OptionalPackage[] packages =
            (OptionalPackage[])dependencies.toArray( new OptionalPackage[ 0 ] );
        return OptionalPackage.toFiles( packages );
    }

    private void deployFromDescriptor( final Configuration descriptor,
                                       final ClassLoader classLoader,
                                       final URL url )
        throws DeploymentException, Exception
    {
        try
        {
            //Have to keep a new factory per role
            //To avoid name clashes (ie a datatype and task with same name)
            final HashMap factorys = new HashMap();

            final Configuration[] types = descriptor.getChild( "types" ).getChildren();
            for( int i = 0; i < types.length; i++ )
            {
                final String name = types[ i ].getName();
                final String role = getRoleForName( name );
                final DefaultTypeFactory factory = getFactory( role, classLoader, factorys );
                handleType( role, types[ i ], factory );
            }

            final DefaultTypeFactory factory = new DefaultTypeFactory( classLoader );
            final Configuration[] converters = descriptor.getChild( "converters" ).getChildren();
            for( int i = 0; i < converters.length; i++ )
            {
                final String name = converters[ i ].getName();
                handleConverter( converters[ i ], factory );
            }
        }
        catch( final DeploymentException de )
        {
            throw de;
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "deploy-lib.error", url );
            throw new DeploymentException( message, e );
        }
    }

    private DefaultTypeFactory getFactory( final String role,
                                           final ClassLoader classLoader,
                                           final HashMap factorys )
    {
        DefaultTypeFactory factory = (DefaultTypeFactory)factorys.get( role );

        if( null == factory )
        {
            factory = new DefaultTypeFactory( classLoader );
            factorys.put( role, factory );
        }

        return factory;
    }

    private String getNameForRole( final String role )
        throws DeploymentException
    {
        final String name = m_roleManager.getNameForRole( role );

        if( null == name )
        {
            final String message = REZ.getString( "unknown-name4role.error", role );
            throw new DeploymentException( message );
        }

        return name;
    }

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

    private void checkFile( final File file )
        throws DeploymentException
    {
        if( !file.exists() )
        {
            final String message = REZ.getString( "no-file.error", file );
            throw new DeploymentException( message );
        }

        if( file.isDirectory() )
        {
            final String message = REZ.getString( "file-is-dir.error", file );
            throw new DeploymentException( message );
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

        if( getLogger().isDebugEnabled() )
        {
            final String message =
                REZ.getString( "register-converter.notice", name, source, destination );
            getLogger().debug( message );
        }
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

        if( getLogger().isDebugEnabled() )
        {
            final String message =
                REZ.getString( "register-role.notice", role, name, className );
            getLogger().debug( message );
        }
    }
}
