/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet;

import org.apache.avalon.util.ValuedEnum;

/**
 * Type safe wrapper class for Java Version enums.
 */
public final class JavaVersion
    extends ValuedEnum
{
    //standard enums for version of JVM
    public final static JavaVersion  JAVA1_0  = new JavaVersion( "Java 1.0", 100 );
    public final static JavaVersion  JAVA1_1  = new JavaVersion( "Java 1.1", 110 );
    public final static JavaVersion  JAVA1_2  = new JavaVersion( "Java 1.2", 120 );
    public final static JavaVersion  JAVA1_3  = new JavaVersion( "Java 1.3", 130 );

    private JavaVersion( final String name, final int value )
    {
        super( name, value );
    }
}
