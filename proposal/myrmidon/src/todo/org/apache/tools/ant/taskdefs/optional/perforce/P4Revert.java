/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.myrmidon.api.TaskException;

/*
 * P4Revert - revert open files or files in a changelist
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */

public class P4Revert extends P4Base
{

    private String revertChange = null;
    private boolean onlyUnchanged = false;

    public void setChange( String revertChange )
        throws TaskException
    {
        if( revertChange == null && !revertChange.equals( "" ) )
            throw new TaskException( "P4Revert: change cannot be null or empty" );

        this.revertChange = revertChange;

    }

    public void setRevertOnlyUnchanged( boolean onlyUnchanged )
    {
        this.onlyUnchanged = onlyUnchanged;
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
        if( onlyUnchanged )
            p4cmd += " -a";

        if( revertChange != null )
            p4cmd += " -c " + revertChange;

        execP4Command( p4cmd + " " + P4View, new SimpleP4OutputHandler( this ) );
    }
}
