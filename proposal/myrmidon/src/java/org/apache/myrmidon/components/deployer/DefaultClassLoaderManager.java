/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
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
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.extensions.ExtensionManager;

/**
 * A default implementation of a classloader manager.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class DefaultClassLoaderManager
    extends AbstractLogEnabled
    implements ClassLoaderManager, Serviceable, Initializable
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultClassLoaderManager.class );

    /**
     * Map from File to the ClassLoader for that file.
     */
    private final Map m_fileDeployers = new HashMap();

    private PackageManager m_packageManager;
    private ClassLoader m_baseClassLoader;

    public void initialize() throws Exception
    {
        if( null == m_baseClassLoader )
        {
            m_baseClassLoader = Thread.currentThread().getContextClassLoader();
        }
    }

    /**
     * Sets the ClassLoader to use as the parent for all classloaders
     * created by this ClassLoader manager.
     */
    public void setBaseClassLoader( final ClassLoader classLoader )
    {
        m_baseClassLoader = classLoader;
    }

    /**
     * Retrieve relevent services needed to deploy.
     */
    public void service( final ServiceManager serviceManager )
        throws ServiceException
    {
        final ExtensionManager extensionManager =
            (ExtensionManager)serviceManager.lookup( ExtensionManager.ROLE );
        m_packageManager = new PackageManager( extensionManager );
    }

    /**
     * Creates a class loader for a Jar file.
     */
    public ClassLoader createClassLoader( File file ) throws DeploymentException
    {
        try
        {
            final File canonFile = file.getCanonicalFile();

            // Locate cached classloader, creating it if necessary
            URLClassLoader classLoader = (URLClassLoader)m_fileDeployers.get( canonFile );
            if( classLoader == null )
            {
                checkFile( canonFile );
                final File[] extensions = getOptionalPackagesFor( canonFile );
                final URL[] urls = buildClasspath( canonFile, extensions );
                classLoader = new URLClassLoader( urls, m_baseClassLoader );
                m_fileDeployers.put( canonFile, classLoader );
            }
            return classLoader;
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "create-classloader-for-file.error", file );
            throw new DeploymentException( message );
        }
    }

    /**
     * Assembles a set of files into a URL classpath.
     */
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
