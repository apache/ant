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
 * P4Revert - revert open files or files in a changelist
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */
public class P4Revert
    extends P4Base
{
    private String m_revertChange;
    private boolean m_onlyUnchanged;

    public void setChange( final String revertChange )
        throws TaskException
    {
        if( revertChange == null && !revertChange.equals( "" ) )
        {
            throw new TaskException( "P4Revert: change cannot be null or empty" );
        }

        m_revertChange = revertChange;
    }

    public void setRevertOnlyUnchanged( boolean onlyUnchanged )
    {
        this.m_onlyUnchanged = onlyUnchanged;
    }

    public void execute()
        throws TaskException
    {
        /*
         * Here we can either revert any unchanged files in a changelist
         * or
         * any files regardless of whether they have been changed or not
         *
         *
         * The whole process also accepts a p4 filespec
         */
        String p4cmd = "-s revert";
        if( m_onlyUnchanged )
        {
            p4cmd += " -a";
        }

        if( m_revertChange != null )
        {
            p4cmd += " -c " + m_revertChange;
        }

        final String command = p4cmd + " " + m_p4View;
        execP4Command( command, null );
    }
}
