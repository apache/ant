/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import java.util.ArrayList;
import junit.framework.AssertionFailedError;

/**
 * Simple class to test typed adder.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ConfigTest4
{
    public void add( final Integer integer )
    {
        System.out.println( "This should not have been called as " +
                            "Integer is not an interface" );
    }
}
