/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;

/**
 * P4Submit - submit a numbered changelist to Perforce. <B>Note:</B> P4Submit
 * cannot (yet) submit the default changelist. This shouldn't be a problem with
 * the ANT API as the usual flow is P4Change to create a new numbered change
 * followed by P4Edit then P4Submit. Example Usage:-<br>
 * &lt;p4submit change="${p4.change}" /&gt;
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */
public class P4Submit extends P4Base
{

    //ToDo: If dealing with default cl need to parse out <enter description here>
    public String change;

    public void setChange( String change )
    {
        this.change = change;
    }

    public void execute()
        throws TaskException
    {
        if( change != null )
        {
            execP4Command( "submit -c " + change,
                           new P4HandlerAdapter()
                           {
                               public void process( String line )
                               {
                                   log( line, Project.MSG_VERBOSE );
                               }
                           }
            );

        }
        else
        {
            //here we'd parse the output from change -o into submit -i
            //in order to support default change.
            throw new TaskException( "No change specified (no support for default change yet...." );
        }
    }

}
