/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.classloader;

import java.io.File;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
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
import org.apache.myrmidon.interfaces.classloader.ClassLoaderManager;
import org.apache.myrmidon.interfaces.classloader.ClassLoaderException;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.extensions.ExtensionManager;
import org.apache.tools.todo.types.PathUtil;

/**
 * A default implementation of a ClassLoader manager.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class DefaultClassLoaderManager
    extends AbstractLogEnabled
    implements ClassLoaderManager, Serviceable, Initializable
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultClassLoaderManager.class );

    /**
     * Map from File to the ClassLoader for that file.
     */
    private final Map m_fileDeployers = new HashMap();

    private PackageManager m_packageManager;
    private ClassLoader m_commonClassLoader;

    public void initialize() throws Exception
    {
        if( null == m_commonClassLoader )
        {
            m_commonClassLoader = Thread.currentThread().getContextClassLoader();
        }
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
     * Sets the ClassLoader to use as the parent for all classloaders
     * created by this ClassLoader manager.
     */
    public void setCommonClassLoader( final ClassLoader classLoader )
    {
        m_commonClassLoader = classLoader;
    }

    /**
     * Returns the common ClassLoader.  This is the parent of all classloaders
     * built by this ClassLoaderManager.
     */
    public ClassLoader getCommonClassLoader()
    {
        return m_commonClassLoader;
    }

    /**
     * Creates a class loader for a Jar file.
     */
    public ClassLoader createClassLoader( final File file ) throws ClassLoaderException
    {
        return createClassLoader( new File[] { file } );
    }

    /**
     * Creates a class loader for a set of Jar files.
     */
    public ClassLoader createClassLoader( final File[] files ) throws ClassLoaderException
    {
        try
        {
            // Build a list of canonical file names
            final ArrayList canonFiles = new ArrayList( files.length );
            for( int i = 0; i < files.length; i++ )
            {
                canonFiles.add( files[ i ].getCanonicalFile() );
            }

            // Locate cached classloader, creating it if necessary
            ClassLoader classLoader = (ClassLoader)m_fileDeployers.get( canonFiles );
            if( classLoader == null )
            {
                classLoader = buildClassLoader( canonFiles );
                m_fileDeployers.put( canonFiles, classLoader );
            }
            return classLoader;
        }
        catch( final Exception e )
        {
            final String fileNames = PathUtil.formatPath( files );
            final String message = REZ.getString( "create-classloader-for-file.error", fileNames );
            throw new ClassLoaderException( message, e );
        }
    }

    /**
     * Builds the classloader for a set of files.
     */
    private ClassLoader buildClassLoader( final ArrayList files )
        throws Exception
    {
        final ArrayList allFiles = new ArrayList( files );
        final int count = files.size();
        for( int i = 0; i < count; i++ )
        {
            final File file = (File)files.get(i );
            checkFile( file );
            getOptionalPackagesFor( file, allFiles );
        }

        final URL[] urls = buildClasspath( allFiles );
        return new URLClassLoader( urls, m_commonClassLoader );
    }

    /**
     * Assembles a set of files into a URL classpath.
     */
    private URL[] buildClasspath( final ArrayList files )
        throws MalformedURLException
    {
        final URL[] urls = new URL[ files.size() ];
        final int count = files.size();
        for( int i = 0; i < count; i++ )
        {
            final File file = (File)files.get( i );
            urls[ i ] = file.toURL();
        }

        return urls;
    }

    /**
     * Retrieve the files for the optional packages required by
     * the specified typeLibrary jar.
     *
     * @param jarFile the typeLibrary
     * @param packages used to return the files that need to be added to ClassLoader.
     */
    private void getOptionalPackagesFor( final File jarFile, final List packages )
        throws Exception
    {
        final URL url = new URL( "jar:" + jarFile.getCanonicalFile().toURL() + "!/" );
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
            final String message =
                REZ.getString( "unsatisfied.extensions.error", new Integer( unsatisfied.size() ) );
            throw new Exception( message );
        }

        final int count = dependencies.size();
        for( int i = 0; i < count; i++ )
        {
            final OptionalPackage optionalPackage = (OptionalPackage)dependencies.get(i );
            packages.add( optionalPackage.getFile() );
        }
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
