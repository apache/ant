/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import org.apache.avalon.framework.ValuedEnum;

/**
 * Type safe wrapper class for Java Version enums.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public final class JavaVersion
    extends ValuedEnum
{
    //standard enums for version of JVM
    public final static JavaVersion JAVA1_0 = new JavaVersion( "Java 1.0", 100 );
    public final static JavaVersion JAVA1_1 = new JavaVersion( "Java 1.1", 110 );
    public final static JavaVersion JAVA1_2 = new JavaVersion( "Java 1.2", 120 );
    public final static JavaVersion JAVA1_3 = new JavaVersion( "Java 1.3", 130 );
    public final static JavaVersion JAVA1_4 = new JavaVersion( "Java 1.4", 140 );

    private final static JavaVersion CURRENT = determineCurrentJavaVersion();

    /**
     * Method to retrieve the current JVM version.
     *
     * @return the current JVM version
     */
    public static final JavaVersion getCurrentJavaVersion()
    {
        return CURRENT;
    }

    /**
     * Private constructor so no instance except here can be defined.
     *
     * @param name the java version name
     * @param value the version * 100
     */
    private JavaVersion( final String name, final int value )
    {
        super( name, value );
    }

    /**
     * Helper method to retrieve current JVM version.
     *
     * @return the current JVM version
     */
    private static final JavaVersion determineCurrentJavaVersion()
    {
        JavaVersion version = JavaVersion.JAVA1_0;

        try
        {
            Class.forName( "java.lang.Void" );
            version = JAVA1_1;
            Class.forName( "java.lang.ThreadLocal" );
            version = JAVA1_2;
            Class.forName( "java.lang.StrictMath" );
            version = JAVA1_3;
            Class.forName("java.lang.CharSequence");
            version = JAVA1_4;
        }
        catch( final ClassNotFoundException cnfe )
        {
        }

        return version;
    }
}
