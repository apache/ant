/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.selftest;

import org.apache.antlib.selftest.extension1.ExtensionsLoadedClass;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * This is to test whether extension is loaded.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant.task name="extensions-test"
 */
public class ExtensionsTest
    extends AbstractTask
{
    public void execute()
        throws TaskException
    {
        ExtensionsLoadedClass.doSomething();

        Class clazz = null;
        try
        {
            clazz = Class.forName( "sun.tools.javac.Main" );
        }
        catch( ClassNotFoundException e )
        {
            try
            {
                clazz = Class.forName( "com.sun.tools.javac.Main" );
            }
            catch( ClassNotFoundException e1 )
            {
                throw new TaskException( "Unable to locate compilers from tools.jar" );
            }
        }

        System.out.println( "Compiler loaded from tools.jar = " + clazz );
    }
}
