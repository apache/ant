/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.antlib.selftest;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * Test sub-elements addition.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class SubElementTest
    extends AbstractTask
{
    public static final class Beep
    {
        public void setMessage( final String string )
        {
            System.out.println( string );
        }
    }

    public Beep createCreateBeep()
    {
        System.out.println( "createCreateBeep()" );
        return new Beep();
    }

    public void addAddBeep( final Beep beep )
    {
        System.out.println( "addBeeper(" + beep + ");" );
    }

    public void execute()
        throws TaskException
    {
    }
}
