/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.type;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.apache.myrmidon.AbstractMyrmidonTest;

/**
 * These are unit tests that test the basic operation of TypeFactories.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class TypeFactoryTest
    extends AbstractMyrmidonTest
{
    private final static String TYPE_NAME1 = "my-type1";
    private final static String TYPE_NAME2 = "my-type2";
    private final static Class TYPE_CLASS1 = MyType1.class;
    private final static Class TYPE_CLASS2 = MyType2.class;
    private final static String TYPE_CLASSNAME1 = TYPE_CLASS1.getName();
    private final static String TYPE_CLASSNAME2 = TYPE_CLASS2.getName();

    public TypeFactoryTest( final String name )
    {
        super( name );
    }

    /**
     * Make sure that you can load a basic type from DefaultTypeManager.
     */
    public void testBasicType()
    {
        final ClassLoader classLoader = getClass().getClassLoader();
        final DefaultTypeFactory factory = new DefaultTypeFactory( classLoader );
        factory.addNameClassMapping( TYPE_NAME2, TYPE_CLASSNAME2 );

        try
        {
            final Object type = factory.create( TYPE_NAME2 );
            final Class typeClass = type.getClass();
            assertEquals( "The type loaded for factory should be same class as in current classloader",
                          typeClass, TYPE_CLASS2 );
        }
        catch( TypeException e )
        {
            fail( "Unable to create Type due to " + e );
        }
    }

    /**
     * Make sure that when you load a type from a RelaodableTypeFactory
     * that it is actually reloaded.
     */
    public void testReloadingTypeFactory()
        throws Exception
    {
        final File file = getTestResource( "types.jar" );
        assertTrue( "Support Jar " + file + " exists", file.exists() );

        final URL[] classpath = new URL[]{file.toURL()};
        final ReloadingTypeFactory factory = new ReloadingTypeFactory( classpath, null );
        factory.addNameClassMapping( TYPE_NAME1, TYPE_CLASSNAME1 );

        try
        {
            final Object type = factory.create( TYPE_NAME1 );
            final Class typeClass = type.getClass();
            final boolean sameClass = typeClass == TYPE_CLASS1;
            assertTrue( "The type loaded for factory should not be same class as in current classloader",
                        !sameClass );
        }
        catch( TypeException e )
        {
            fail( "Unable to create Type due to " + e );
        }
    }
}
