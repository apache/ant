/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.selftest;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * This is to test whether adders with just a type (and no name) work.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @ant:task name="typed-adder-test"
 */
public class TypedAdderTest
    extends AbstractTask
{
    public void add( final Integer value )
    {
        //Should fail as value is not an interface
        getLogger().warn( "Integer add: " + value );
    }

    public void execute()
        throws TaskException
    {
    }
}
