/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.condition;

import org.apache.myrmidon.api.TaskException;

/**
 * Simple String comparison condition.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public class Equals implements Condition
{

    private String arg1, arg2;

    public void setArg1( String a1 )
    {
        arg1 = a1;
    }

    public void setArg2( String a2 )
    {
        arg2 = a2;
    }

    public boolean eval()
        throws TaskException
    {
        if( arg1 == null || arg2 == null )
        {
            throw new TaskException( "both arg1 and arg2 are required in equals" );
        }
        return arg1.equals( arg2 );
    }
}
