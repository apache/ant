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
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;
import org.apache.avalon.excalibur.extension.Extension;
import org.apache.avalon.excalibur.extension.OptionalPackage;
import org.apache.avalon.excalibur.extension.PackageManager;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;
import org.apache.myrmidon.interfaces.extensions.ExtensionManager;

/**
 * This class deploys a .tsk file into a registry.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class DefaultDeployer
    extends AbstractLogEnabled
    implements Deployer, Composable
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultDeployer.class );

    private Deployer m_parent;
    private ComponentManager m_componentManager;
    private PackageManager m_packageManager;

    /** Map from ClassLoader to the deployer for that class loader. */
    private Map m_classLoaderDeployers = new HashMap();

    /**
     * Map from File to the ClassLoader for that library.  This map is shared
     * by all descendents of the root deployer.
     */
    private Map m_fileDeployers;

    /**
     * Creates a root deployer.
     */
    public DefaultDeployer()
    {
        m_fileDeployers = new HashMap();
    }

    private DefaultDeployer( final DefaultDeployer parent )
    {
        m_parent = parent;
        m_fileDeployers = parent.m_fileDeployers;
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
        m_componentManager = componentManager;
        final ExtensionManager extensionManager =
            (ExtensionManager)componentManager.lookup( ExtensionManager.ROLE );
        m_packageManager = new PackageManager( extensionManager );
    }

    /**
     * Creates a child deployer.
     */
    public Deployer createChildDeployer( ComponentManager componentManager )
        throws ComponentException
    {
        final DefaultDeployer child = new DefaultDeployer( this );
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
            URLClassLoader classLoader = getClassLoaderForFile( file );
            return createDeployment( classLoader, file.toURL() );
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "deploy-from-file.error", file );
            throw new DeploymentException( message, e );
        }
    }

    /**
     * Locates the classloader for a typelib file.
     */
    private URLClassLoader getClassLoaderForFile( final File file )
        throws Exception
    {
        File canonFile = file.getCanonicalFile();

        // Locate cached classloader, creating it if necessary
        URLClassLoader classLoader = (URLClassLoader)m_fileDeployers.get( canonFile );
        if( classLoader == null )
        {
            checkFile( canonFile );
            final File[] extensions = getOptionalPackagesFor( canonFile );
            final URL[] urls = buildClasspath( canonFile, extensions );
            classLoader  = new URLClassLoader( urls, Thread.currentThread().getContextClassLoader() );
            m_fileDeployers.put( canonFile, classLoader );
        }
        return classLoader;
    }

    /**
     * Creates a deployer for a ClassLoader.
     */
    private Deployment createDeployment( final ClassLoader loader,
                                         final URL jarUrl ) throws Exception
    {
        // Locate cached deployer, creating it if necessary
        Deployment deployment = (Deployment)m_classLoaderDeployers.get( loader );
        if( deployment == null )
        {
            deployment = new Deployment( loader, m_componentManager );
            setupLogger( deployment );
            deployment.loadDescriptors( jarUrl );
            m_classLoaderDeployers.put( loader, deployment );
        }

        return deployment;
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
                REZ.getString( "available-extensions.notice", Arrays.asList( available ) );
            getLogger().debug( message1 );
            final String message2 =
                REZ.getString( "required-extensions.notice", Arrays.asList( required ) );
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
                REZ.getString( "unsatisfied.extensions.error", new Integer( size ) );
            throw new Exception( message );
        }

        final OptionalPackage[] packages =
            (OptionalPackage[])dependencies.toArray( new OptionalPackage[ 0 ] );
        return OptionalPackage.toFiles( packages );
    }

    /**
     * Ensures a file exists and is not a directory.
     */
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

}
