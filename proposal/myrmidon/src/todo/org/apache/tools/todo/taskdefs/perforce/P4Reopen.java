/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.perforce;

import org.apache.myrmidon.api.TaskException;

/*
 * P4Reopen - move files to a new changelist
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */
public class P4Reopen
    extends P4Base
{
    private String m_toChange = "";

    public void setToChange( final String toChange )
        throws TaskException
    {
        if( toChange == null && !toChange.equals( "" ) )
        {
            throw new TaskException( "P4Reopen: tochange cannot be null or empty" );
        }

        m_toChange = toChange;
    }

    public void execute()
        throws TaskException
    {
        if( m_p4View == null )
        {
            throw new TaskException( "No view specified to reopen" );
        }
        final String message = "-s reopen -c " + m_toChange + " " + m_p4View;
        execP4Command( message, null );
    }
}
