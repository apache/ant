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

        final Collection afferentSet = javaPackage.getAfferents();
        final Iterator afferents = afferentSet.iterator();
        while( afferents.hasNext() )
        {
            final JavaPackage afferent = (JavaPackage)afferents.next();
            final String afferentName = afferent.getName();
            if( !afferentName.startsWith( name ) )
            {
                fail( "The launcher package " + name + " depends on external classes " +
                      "contained in " + afferentName + ". No classes besides " +
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
            final String componentPackage = "org.apache.myrmidon.component.";
            if( !name.startsWith( componentPackage ) )
            {
                continue;
            }
            final int start = componentPackage.length() + 1;
            final int end = name.indexOf( '.', start );
            final String component = name.substring( end );

            final Collection afferentSet = javaPackage.getAfferents();
            final Iterator afferents = afferentSet.iterator();
            while( afferents.hasNext() )
            {
                final JavaPackage efferent = (JavaPackage)afferents.next();
                final String efferentName = efferent.getName();
                if( !efferentName.startsWith( component ) )
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
        final JDepend jDepend = getJDepend();
        final Collection packageSet = jDepend.getPackages();

        final Iterator packages = packageSet.iterator();
        while( packages.hasNext() )
        {
            final JavaPackage javaPackage = (JavaPackage)packages.next();
            final String name = javaPackage.getName();
            if( !name.startsWith( "org.apache.aut" ) )
            {
                continue;
            }

            final Collection efferentSet = javaPackage.getEfferents();
            final Iterator efferents = efferentSet.iterator();
            while( efferents.hasNext() )
            {
                final JavaPackage efferent = (JavaPackage)efferents.next();
                final String efferentName = efferent.getName();
                if( efferentName.startsWith( "org.apache.myrmidon" ) ||
                    efferentName.startsWith( "org.apache.antlib" ) ||
                    efferentName.startsWith( "org.apache.tools.ant" ) )
                {
                    fail( "The package " + name + " depends on classes " +
                          "contained in " + efferentName );
                }
            }
        }
    }

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
}
