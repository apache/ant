/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.classloader.test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import org.apache.myrmidon.components.AbstractComponentTest;
import org.apache.myrmidon.components.classloader.DefaultClassLoaderManager;
import org.apache.myrmidon.interfaces.classloader.ClassLoaderManager;
import org.apache.myrmidon.interfaces.classloader.ClassLoaderException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * Test cases for the DefaultClassLoaderManager.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class DefaultClassLoaderManagerTestCase
    extends AbstractComponentTest
{
    private static final String UNSHARED_PKG_NAME =
        getPackageName( DefaultClassLoaderManagerTestCase.class ) + ".libs.unshared";
    private static final String UNSHARED_RES_NAME = getResourceName( UNSHARED_PKG_NAME, "unshared.txt" );
    private static final String UNSHARED_CLASS_NAME = UNSHARED_PKG_NAME + ".UnsharedClass";

    private static final String SHARED_PKG_NAME =
        getPackageName( DefaultClassLoaderManagerTestCase.class ) + ".libs.shared";
    private static final String SHARED_RES_NAME = getResourceName( SHARED_PKG_NAME, "shared.txt" );
    private static final String SHARED_CLASS_NAME = SHARED_PKG_NAME + ".SharedClass";

    private static final String EXTN_PKG_NAME =
        getPackageName( DefaultClassLoaderManagerTestCase.class ) + ".libs.extn";
    private static final String EXTN_RES_NAME = getResourceName( EXTN_PKG_NAME, "extn.txt" );
    private static final String EXTN_CLASS_NAME = EXTN_PKG_NAME + ".ExtnClass";

    private File m_commonJar;
    private ClassLoader m_commonClassLoader;
    private ClassLoaderManager m_loaderManager;

    public DefaultClassLoaderManagerTestCase( final String name )
    {
        super( name );
    }

    /**
     * Sets up the test.
     */
    protected void setUp() throws Exception
    {
        m_commonJar = getTestResource( "common.jar" );
        final URL commonJarUrl = m_commonJar.toURL();
        m_commonClassLoader = new URLClassLoader( new URL[]{commonJarUrl} );

        assertClassFound( m_commonClassLoader, SHARED_CLASS_NAME );
        assertResourcesFound( m_commonClassLoader, SHARED_RES_NAME, m_commonJar );

        // Create the classloader mgr
        m_loaderManager = (ClassLoaderManager)getServiceManager().lookup( ClassLoaderManager.ROLE );
    }

    /**
     * Creates an instance of a test component.
     */
    protected Object createComponent( final String role, final Class defaultImpl )
        throws Exception
    {
        if( role.equals( ClassLoaderManager.ROLE ) )
        {
            return new DefaultClassLoaderManager( m_commonClassLoader );
        }
        else
        {
            return super.createComponent( role, defaultImpl );
        }
    }

    /**
     * Creates the parameters for the test.  Sub-classes can override this
     * method to set-up the parameters.
     */
    protected Parameters getParameters()
    {
        final Parameters parameters = super.getParameters();
        parameters.setParameter( "myrmidon.ext.path", getTestDirectory( "ext" ).getAbsolutePath() );
        return parameters;
    }

    /**
     * Returns the name of a resource in a package.
     */
    private static String getResourceName( final String pkgName,
                                           final String resname )
    {
        return pkgName.replace( '.', '/' ) + '/' + resname;
    }

    /**
     * Asserts that a class is not available in a classloader.
     */
    private void assertClassNotFound( final ClassLoader classLoader,
                                      final String className )
    {
        try
        {
            classLoader.loadClass( className );
            fail( "Class " + className + " should not be available." );
        }
        catch( ClassNotFoundException e )
        {
        }
    }

    /**
     * Asserts that a class is available in a classloader.
     */
    private void assertClassFound( final ClassLoader classLoader,
                                   final String className )
        throws Exception
    {
        assertClassFound( classLoader, className, classLoader );
    }

    /**
     * Asserts that a class is available in a classloader.
     */
    private void assertClassFound( final ClassLoader classLoader,
                                   final String className,
                                   final ClassLoader expectedClassLoader )
        throws Exception
    {
        try
        {
            final Class cls = classLoader.loadClass( className );
            assertSame( expectedClassLoader, cls.getClassLoader() );
            if( classLoader != expectedClassLoader )
            {
                final Class expectedCls = expectedClassLoader.loadClass( className );
                assertSame( expectedCls, cls );
            }
        }
        catch( ClassNotFoundException e )
        {
            fail( "Class " + className + " not found." );
        }

    }

    /**
     * Asserts that a resouce is not available in a classloader.
     */
    private void assertResourceNotFound( final ClassLoader classLoader,
                                         final String resName )
        throws Exception
    {
        assertNull( classLoader.getResource( resName ) );
        assertNull( classLoader.getResourceAsStream( resName ) );
        final Enumeration enum = classLoader.getResources( resName );
        assertTrue( !enum.hasMoreElements() );
    }

    /**
     * Asserts that a resource is available in a classloader.
     */
    private void assertResourcesFound( final ClassLoader classLoader,
                                       final String resName,
                                       final File expectedJar )
        throws Exception
    {
        assertResourcesFound( classLoader, resName, new File[]{expectedJar} );
    }

    /**
     * Asserts that a resource is available in a classloader.
     */
    private void assertResourcesFound( final ClassLoader classLoader,
                                       final String resName,
                                       final File[] expectedJars )
        throws Exception
    {
        final String[] expectedLocations = new String[ expectedJars.length ];
        for( int i = 0; i < expectedJars.length; i++ )
        {
            final File jar = expectedJars[ i ];
            expectedLocations[ i ] = "jar:" + jar.toURL() + "!/" + resName;
        }

        assertResourcesFound( classLoader, resName, expectedLocations );
    }

    /**
     * Asserts that a resource is available in a classloader.
     */
    private void assertResourcesFound( final ClassLoader classLoader,
                                       final String resName,
                                       final String[] expectedLocations )
        throws Exception
    {
        // Use the first in the list of expected locations as the location
        // of the resource returned by getResource()
        final URL resUrl = classLoader.getResource( resName );
        assertNotNull( resUrl );
        assertEquals( expectedLocations[ 0 ], resUrl.toString() );

        // Now check all of the resources returned by getResources()
        final Enumeration resources = classLoader.getResources( resName );
        for( int i = 0; i < expectedLocations.length; i++ )
        {
            final String expectedLocation = expectedLocations[ i ];
            assertTrue( resources.hasMoreElements() );
            final URL location = (URL)resources.nextElement();
            assertEquals( expectedLocation, location.toString() );
        }
        assertTrue( !resources.hasMoreElements() );
    }

    /**
     * Tests for a Jar with no required extensions.
     */
    public void testNoDependencies() throws Exception
    {
        // Make some assumptions about the common classloader
        assertClassNotFound( m_commonClassLoader, UNSHARED_CLASS_NAME );
        assertResourceNotFound( m_commonClassLoader, UNSHARED_RES_NAME );

        // Build the classloader
        final File jarFile = getTestResource( "no-dependencies.jar" );
        final ClassLoader classLoader = m_loaderManager.getClassLoader( jarFile );

        // Check shared classes/resources
        assertClassFound( classLoader, SHARED_CLASS_NAME, m_commonClassLoader );
        assertResourcesFound( classLoader, SHARED_RES_NAME, new File[]{m_commonJar, jarFile} );

        // Check unshared classes/resources
        assertClassFound( classLoader, UNSHARED_CLASS_NAME );
        assertResourcesFound( classLoader, UNSHARED_RES_NAME, jarFile );
    }

    /**
     * Tests ClassLoader caching.
     */
    public void testClassLoaderReuse() throws Exception
    {
        final File jarFile = getTestResource( "no-dependencies.jar" );
        final ClassLoader classLoader1 = m_loaderManager.getClassLoader( jarFile );
        final ClassLoader classLoader2 = m_loaderManager.getClassLoader( jarFile );
        assertSame( classLoader1, classLoader2 );
    }

    /**
     * Tests for a Jar with a single required extension.
     */
    public void testOneDependency() throws Exception
    {
        // Make some assumptions about the common classloader
        assertClassNotFound( m_commonClassLoader, UNSHARED_CLASS_NAME );
        assertResourceNotFound( m_commonClassLoader, UNSHARED_RES_NAME );
        assertClassNotFound( m_commonClassLoader, EXTN_CLASS_NAME );
        assertResourceNotFound( m_commonClassLoader, EXTN_RES_NAME );

        // Build the extension classloader
        final File extnJarFile = getTestResource( "ext/simple-extension.jar" );
        final ClassLoader extnClassLoader = m_loaderManager.getClassLoader( extnJarFile );

        // Build the Jar classloader
        final File jarFile = getTestResource( "one-dependency.jar" );
        final ClassLoader classLoader = m_loaderManager.getClassLoader( jarFile );

        // Check shared classes/resources
        assertClassFound( classLoader, SHARED_CLASS_NAME, m_commonClassLoader );
        assertResourcesFound( classLoader, SHARED_RES_NAME, new File[]{m_commonJar, extnJarFile, jarFile} );

        // Check extension classes/resources
        assertClassFound( classLoader, EXTN_CLASS_NAME, extnClassLoader );
        assertResourcesFound( classLoader, EXTN_RES_NAME, extnJarFile );

        // Check unshared classes/resources
        assertClassFound( classLoader, UNSHARED_CLASS_NAME );
        assertResourcesFound( classLoader, UNSHARED_RES_NAME, jarFile );
    }

    /**
     * Tests that classes from extensions can be shared across classloaders.
     */
    public void testShareClasses() throws Exception
    {
        // Build the extension classloader
        final File extnJarFile = getTestResource( "ext/simple-extension.jar" );
        final ClassLoader extnClassLoader = m_loaderManager.getClassLoader( extnJarFile );

        // Build the Jar classloaders
        final File jarFile1 = getTestResource( "one-dependency.jar" );
        final ClassLoader classLoader1 = m_loaderManager.getClassLoader( jarFile1 );
        final File jarFile2 = getTestResource( "one-dependency-2.jar" );
        final ClassLoader classLoader2 = m_loaderManager.getClassLoader( jarFile2 );

        // Check extension classes/resources
        assertClassFound( classLoader1, EXTN_CLASS_NAME, extnClassLoader );
        assertResourcesFound( classLoader1, EXTN_RES_NAME, extnJarFile );
        assertClassFound( classLoader2, EXTN_CLASS_NAME, extnClassLoader );
        assertResourcesFound( classLoader2, EXTN_RES_NAME, extnJarFile );
    }

    /**
     * Tests detection of dependency cycles in extensions.
     */
    public void testCycle() throws Exception
    {
        final File jarFile = getTestResource( "ext/cycle-extension-1.jar" );
        try
        {
            m_loaderManager.getClassLoader( jarFile );
            fail();
        }
        catch( final ClassLoaderException e )
        {
            final Resources rez = getResourcesForTested( DefaultClassLoaderManager.class );
            final String[] messages = {
                rez.getString( "create-classloader-for-file.error", jarFile ),
                rez.getString( "dependency-cycle.error", jarFile )
            };
            assertSameMessage( messages, e );
        }
    }

    /**
     * add some classes to common loader only.
     *
     * unknown extension
     * multiple versions of the same extension
     * extn with requirement on itself
     *
     * jar with 1 and 2 extns:
     *   class/resources in parent
     *   class/resources in jar
     *   class/resources in extn
     *   class/resources in all
     *
     * jar with transitive extn
     *   class/resources in 2nd extn
     *
     * jar with transitive extn + explicit extn on same jar
     *   class/resources in 2nd extn
     *
     * Same classes:
     *     get extn explicitly and implicitly, and check classes are the same
     *     extn shared by 2 jars, using same extn and different extns
     *     classes in common classloader, shared by 2 jars
     *
     * multiple files:
     *     fetch classloader twice
     *     different path ordering
     *
     * tools.jar
     */
}
