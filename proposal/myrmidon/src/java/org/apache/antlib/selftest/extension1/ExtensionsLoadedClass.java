/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.selftest.extension1;

/**
 * This is to test whether extension is loaded.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class ExtensionsLoadedClass
{
    public static void doSomething()
    {
        System.out.println( "This was loaded via an extension - yea!" );
    }
}
