/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import org.apache.avalon.excalibur.extension.Extension;
import org.apache.avalon.excalibur.extension.OptionalPackage;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.myrmidon.interfaces.classloader.ClassLoaderException;
import org.apache.myrmidon.interfaces.classloader.ClassLoaderManager;
import org.apache.myrmidon.interfaces.deployer.DeploymentException;
import org.apache.myrmidon.interfaces.extensions.ExtensionManager;
import org.apache.tools.todo.types.PathUtil;

/**
 * A default implementation of a ClassLoader manager.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class DefaultClassLoaderManager
    extends AbstractLogEnabled
    implements ClassLoaderManager, Serviceable, Initializable
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultClassLoaderManager.class );

    /**
     * Map from File/ArrayList to the ClassLoader for that file/files.
     */
    private final Map m_classLoaders = new HashMap();

    private ExtensionManager m_extensionManager;
    private ClassLoader m_commonClassLoader;

    public DefaultClassLoaderManager()
    {
    }

    public DefaultClassLoaderManager( final ClassLoader commonClassLoader )
    {
        m_commonClassLoader = commonClassLoader;
    }

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
        m_extensionManager = (ExtensionManager)serviceManager.lookup( ExtensionManager.ROLE );
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
    public ClassLoader getClassLoader( final File file ) throws ClassLoaderException
    {
        try
        {
            final File canonFile = file.getCanonicalFile();

            // Check for cached classloader, creating it if required
            ClassLoader loader = (ClassLoader)m_classLoaders.get( canonFile );
            if( loader == null )
            {
                checkFile( canonFile );
                final OptionalPackage optionalPackage = toOptionalPackage( canonFile );
                loader = buildClassLoader( optionalPackage, new HashSet() );
            }
            return loader;
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "create-classloader-for-file.error", file );
            throw new ClassLoaderException( message, e );
        }
    }

    /**
     * Creates a class loader for a set of Jar files.
     */
    public ClassLoader createClassLoader( final File[] files ) throws ClassLoaderException
    {
        try
        {
            if( files == null || files.length == 0 )
            {
                return m_commonClassLoader;
            }

            // Build a list of optional packages for the files
            final OptionalPackage[] packages = new OptionalPackage[ files.length ];
            for( int i = 0; i < files.length; i++ )
            {
                final File canonFile = files[ i ].getCanonicalFile();
                checkFile( canonFile );
                packages[ i ] = toOptionalPackage( canonFile );
            }

            // Build the classloaders for the required extensions
            final ClassLoader[] parentClassLoaders = buildParentClassLoaders( packages, new HashSet() );

            // Build the classloader
            final URL[] urls = buildClasspath( files );
            return new MultiParentURLClassLoader( urls, parentClassLoaders );
        }
        catch( final Exception e )
        {
            final String fileNames = PathUtil.formatPath( files );
            final String message = REZ.getString( "create-classloader-for-files.error", fileNames );
            throw new ClassLoaderException( message, e );
        }
    }

    /**
     * Builds the classloader for an optional package.
     */
    private ClassLoader buildClassLoader( final OptionalPackage pkg,
                                          final Set pending )
        throws Exception
    {
        final File jarFile = pkg.getFile();

        // Check for cached classloader
        ClassLoader classLoader = (ClassLoader)m_classLoaders.get( jarFile );
        if( classLoader != null )
        {
            return classLoader;
        }

        // Check for cyclic dependency
        if( pending.contains( jarFile ) )
        {
            final String message = REZ.getString( "dependency-cycle.error", jarFile );
            throw new Exception( message );
        }
        pending.add( jarFile );

        // Build the classloaders for the extensions required by this optional
        // package
        final ClassLoader[] parentClassLoaders =
            buildParentClassLoaders( new OptionalPackage[] { pkg }, pending );

        // Create and cache the classloader
        final URL[] urls = { jarFile.toURL() };
        classLoader = new MultiParentURLClassLoader( urls, parentClassLoaders );
        m_classLoaders.put( jarFile, classLoader );
        pending.remove( jarFile );
        return classLoader;
    }

    /**
     * Builds the parent classloaders for a set of optional packages.  That is,
     * the classloaders for all of the extensions required by the given set
     * of optional packages.
     */
    private ClassLoader[] buildParentClassLoaders( final OptionalPackage[] packages,
                                                   final Set pending )
        throws Exception
    {
        final ArrayList classLoaders = new ArrayList();

        // Include the common class loader
        classLoaders.add( m_commonClassLoader );

        // Build the classloader for each optional package, filtering out duplicates
        for( int i = 0; i < packages.length; i++ )
        {
            final OptionalPackage optionalPackage = packages[ i ];

            // Locate the dependencies for this jar file
            final OptionalPackage[] requiredPackages = getOptionalPackagesFor( optionalPackage );

            // Build the classloader for the package
            for( int j = 0; j < requiredPackages.length; j++ )
            {
                final OptionalPackage requiredPackage = requiredPackages[j ];
                final ClassLoader classLoader = buildClassLoader( requiredPackage, pending );
                if( ! classLoaders.contains( classLoader ) )
                {
                    classLoaders.add( classLoader );
                }
            }
        }

        return (ClassLoader[])classLoaders.toArray( new ClassLoader[classLoaders.size() ] );
    }

    /**
     * Assembles a set of files into a URL classpath.
     */
    private URL[] buildClasspath( final File[] files )
        throws MalformedURLException
    {
        final URL[] urls = new URL[ files.length ];
        for( int i = 0; i < files.length; i++ )
        {
            urls[ i ]  = files[i ].toURL();
        }

        return urls;
    }

    /**
     * Builds an OptionalPackage for a Jar file.
     *
     * @param file the jar.
     */
    private OptionalPackage toOptionalPackage( final File file )
        throws Exception
    {
        // Determine the extensions required by this file
        final JarFile jarFile = new JarFile( file );
        final Manifest manifest = jarFile.getManifest();
        final Extension[] required = Extension.getRequired( manifest );
        return new OptionalPackage( file, new Extension[0], required );
    }

    /**
     * Locates the optional packages required by an optional package.
     */
    private OptionalPackage[] getOptionalPackagesFor( final OptionalPackage pkg )
        throws Exception
    {
        // Locate the optional packages that provide the required extesions
        final Extension[] required = pkg.getRequiredExtensions();
        final ArrayList packages = new ArrayList();
        for( int i = 0; i < required.length; i++ )
        {
            final Extension extension = required[i ];
            final OptionalPackage optionalPackage = m_extensionManager.getOptionalPackage( extension );
            if( optionalPackage == null )
            {
                final String message =
                    REZ.getString( "unsatisfied.extension.error",
                                   pkg.getFile(),
                                   extension.getExtensionName(),
                                   extension.getSpecificationVersion() );
                throw new Exception( message );
            }
            packages.add( optionalPackage );
        }

        return (OptionalPackage[])packages.toArray( new OptionalPackage[packages.size() ] );
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
