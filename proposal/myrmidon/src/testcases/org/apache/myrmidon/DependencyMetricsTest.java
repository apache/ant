/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;
import junit.framework.TestCase;

/**
 * An abstract Unit test that can be used to test Dependency metrics
 * fall in acceptable limits.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DependencyMetricsTest
    extends TestCase
{
    private JDepend m_jDepend;

    public DependencyMetricsTest( final String name )
    {
        super( name );
    }

    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    protected void setUp()
    {
        m_jDepend = new JDepend();

        try
        {

            m_jDepend.addDirectory( "src/java" );
            //m_jDepend.addDirectory( "src/main" );
        }
        catch( final IOException ioe )
        {
            fail( ioe.getMessage() );
        }

        m_jDepend.analyze();
    }

    /**
     * Tears down the test fixture.
     *
     * Called after every test case method.
     */
    protected void tearDown()
    {
        m_jDepend = null;
    }

    /**
     * Utility method to retrieve JDpenden instance that contains statistics.
     */
    protected final JDepend getJDepend()
    {
        return m_jDepend;
    }

    /**
     * Make sure that the launcher classes in org.apache.myrmidon.launcher.*
     * are completely decoupled from the rest of the system.
     */
    public void testLauncherDecoupled()
    {
        final JDepend jDepend = getJDepend();
        final String name = "org.apache.myrmidon.launcher";
        final JavaPackage javaPackage = jDepend.getPackage( name );

        final Collection efferentSet = javaPackage.getEfferents();
        final Iterator afferents = efferentSet.iterator();
        while( afferents.hasNext() )
        {
            final JavaPackage efferent = (JavaPackage)afferents.next();
            final String efferentName = efferent.getName();
            if( ! isSubPackage( name, efferentName ) )
            {
                fail( "The launcher package " + name + " depends on external classes " +
                      "contained in " + efferentName + ". No classes besides " +
                      "those in the launcher hierarchy should be referenced" );
            }
        }
    }

    /**
     * Make sure that the implementations of the myrmidon kernel components
     * (ie org.apache.myrmidon.component.X.*) are not referenced by anyone
     * except by other objects in the same package or child packages.
     */
    public void testNoComponentImplSharing()
    {
        final JDepend jDepend = getJDepend();
        final Collection packageSet = jDepend.getPackages();

        final Iterator packages = packageSet.iterator();
        while( packages.hasNext() )
        {
            final JavaPackage javaPackage = (JavaPackage)packages.next();
            final String name = javaPackage.getName();
            final String componentPackage = "org.apache.myrmidon.components.";
            if( !name.startsWith( componentPackage ) )
            {
                continue;
            }

            // Extract the component package
            final int start = componentPackage.length() + 1;
            final int end = name.indexOf( '.', start );
            final String component;
            if( end > -1 )
            {
                component = name.substring( end );
            }
            else
            {
                component = name;
            }

            // Make sure that all the afferent packages of this package (i.e.
            // those that refer to this package) are sub-packages of the
            // component package
            final Collection afferentSet = javaPackage.getAfferents();
            final Iterator afferents = afferentSet.iterator();
            while( afferents.hasNext() )
            {
                final JavaPackage efferent = (JavaPackage)afferents.next();
                final String efferentName = efferent.getName();
                if( !isSubPackage( component, efferentName ) )
                {
                    fail( "The package " + name + " is referred to by classes " +
                          "contained in " + efferentName + ". No classes besides " +
                          "those part of the particular implementation of kernel " +
                          "component should reference the implementations" );
                }
            }
        }
    }

    /**
     * Make sure that aut does not depend on any other ant classes
     * and thus can be cleanly decoupled.
     */
    public void testAutDecoupled()
    {
        final String packageName = "org.apache.aut";
        final String[] badEfferents = new String[]
        {
            "org.apache.myrmidon", "org.apache.antlib", "org.apache.tools.ant"
        };
        doTestDecoupled( packageName, badEfferents );
    }

    /**
     * Make sure that myrmidon package does not have any
     * unwanted dependencies.
     */
    /*
    public void testMyrmidonDecoupled()
    {
        final String packageName = "org.apache.myrmidon";
        final String[] badEfferents = new String[]
        {
            "org.apache.antlib", "org.apache.tools.ant"
        };
        doTestDecoupled( packageName, badEfferents );
    }
    */

    /**
     * Make sure that antlib package does not have any
     * unwanted dependencies.
     */
    /*
        public void testAntlibDecoupled()
        {
            final String packageName = "org.apache.antlib";
            final String[] badEfferents = new String[]
            {
                "org.apache.tools.ant"
            };
            doTestDecoupled( packageName, badEfferents );
        }
    */
    /**
     * Make sure there are no circular dependencies between packages because
     * circular dependencies are evil!!!
     */
    public void testNoCircularity()
    {
        final JDepend jDepend = getJDepend();
        final Collection packageSet = jDepend.getPackages();
        final Iterator packages = packageSet.iterator();
        while( packages.hasNext() )
        {
            final JavaPackage javaPackage = (JavaPackage)packages.next();
            if( javaPackage.containsCycle() )
            {
                final ArrayList cycle = new ArrayList();
                javaPackage.collectCycle( cycle );

                final ArrayList names = getPackageNames( cycle );
                fail( "The package " + javaPackage.getName() + " contains a cycle " +
                      "with a path " + names );
            }
        }
    }

    private ArrayList getPackageNames( final ArrayList cycle )
    {
        final ArrayList names = new ArrayList();

        final int size = cycle.size();
        for( int i = 0; i < size; i++ )
        {
            final JavaPackage javaPackage = (JavaPackage)cycle.get( i );
            names.add( javaPackage.getName() );
        }

        return names;
    }

    /**
     * Make sure that the specified package does not depend on any
     * of the specified package hierarchies.
     */
    private void doTestDecoupled( final String packageName,
                                  final String[] invalidEfferents )
    {
        final JDepend jDepend = getJDepend();
        final Collection packageSet = jDepend.getPackages();

        final Iterator packages = packageSet.iterator();
        while( packages.hasNext() )
        {
            final JavaPackage javaPackage = (JavaPackage)packages.next();
            final String name = javaPackage.getName();
            if( !isSubPackage( packageName, name ) )
            {
                continue;
            }

            final Collection efferentSet = javaPackage.getEfferents();
            final Iterator efferents = efferentSet.iterator();
            while( efferents.hasNext() )
            {
                final JavaPackage efferent = (JavaPackage)efferents.next();
                final String efferentName = efferent.getName();
                for( int i = 0; i < invalidEfferents.length; i++ )
                {
                    final String other = invalidEfferents[ i ];
                    if( isSubPackage( other, efferentName ) )
                    {
                        fail( "The package " + name + " has an unwanted dependency " +
                              "on classes contained in " + efferentName );
                    }
                }
            }
        }
    }

    /**
     * Determines if a package is a sub-package of another package.
     *
     * @return true if <code>subpackage</code> is either the same package as
     *         <code>basePackage</code>, or a sub-package of it.
     */
    private boolean isSubPackage( final String basePackage,
                                  final String subpackage )
    {
        if( ! subpackage.startsWith( basePackage ) )
        {
            return false;
        }
        return ( subpackage.length() == basePackage.length()
                 || subpackage.charAt( basePackage.length() ) == '.' );
    }
}
