/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;

public class SimpleP4OutputHandler
    extends P4HandlerAdapter
{
    private P4Base parent;

    public SimpleP4OutputHandler( P4Base parent )
    {
        this.parent = parent;
    }

    public void process( String line )
        throws TaskException
    {
        if( parent.util.match( "/^exit/", line ) )
            return;

        //Throw exception on errors (except up-to-date)
        //p4 -s is unpredicatable. For example a server down
        //does not return error: markup
        //
        //Some forms producing commands (p4 -s change -o) do tag the output
        //others don't.....
        //Others mark errors as info, for example edit a file
        //which is already open for edit.....
        //Just look for error: - catches most things....

        if( parent.util.match( "/error:/", line ) && !parent.util.match( "/up-to-date/", line ) )
        {
            throw new TaskException( line );
        }

        parent.log( parent.util.substitute( "s/^.*: //", line ), Project.MSG_INFO );

    }
}
