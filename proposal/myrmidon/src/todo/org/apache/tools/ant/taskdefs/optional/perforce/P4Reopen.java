/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.myrmidon.api.TaskException;

/*
 * P4Reopen - move files to a new changelist
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */

public class P4Reopen extends P4Base
{

    private String toChange = "";

    public void setToChange( String toChange )
        throws TaskException
    {
        if( toChange == null && !toChange.equals( "" ) )
            throw new TaskException( "P4Reopen: tochange cannot be null or empty" );

        this.toChange = toChange;
    }

    public void execute()
        throws TaskException
    {
        if( P4View == null )
            if( P4View == null )
                throw new TaskException( "No view specified to reopen" );
        execP4Command( "-s reopen -c " + toChange + " " + P4View, new SimpleP4OutputHandler( this ) );
    }
}
