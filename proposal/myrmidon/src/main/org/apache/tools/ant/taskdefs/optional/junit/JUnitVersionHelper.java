/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.lang.reflect.Method;
import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Work around for some changes to the public JUnit API between different JUnit
 * releases.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public class JUnitVersionHelper
{

    private static Method testCaseName = null;

    static
    {
        try
        {
            testCaseName = TestCase.class.getMethod( "getName", new Class[ 0 ] );
        }
        catch( NoSuchMethodException e )
        {
            // pre JUnit 3.7
            try
            {
                testCaseName = TestCase.class.getMethod( "name", new Class[ 0 ] );
            }
            catch( NoSuchMethodException e2 )
            {
            }
        }
    }

    /**
     * JUnit 3.7 introduces TestCase.getName() and subsequent versions of JUnit
     * remove the old name() method. This method provides access to the name of
     * a TestCase via reflection that is supposed to work with version before
     * and after JUnit 3.7.
     *
     * @param t Description of Parameter
     * @return The TestCaseName value
     */
    public static String getTestCaseName( Test t )
    {
        if( t instanceof TestCase && testCaseName != null )
        {
            try
            {
                return (String)testCaseName.invoke( t, new Object[ 0 ] );
            }
            catch( Throwable e )
            {
            }
        }
        return "unknown";
    }

}
