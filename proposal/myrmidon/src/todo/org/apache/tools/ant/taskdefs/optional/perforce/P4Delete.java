/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.myrmidon.api.TaskException;

/**
 * P4Delete - checkout file(s) for delete. Example Usage:<br>
 * &lt;p4delete change="${p4.change}" view="//depot/project/foo.txt" /&gt;<br>
 * Simple re-write of P4Edit changing 'edit' to 'delete'.<br>
 * ToDo: What to do if file is already open in one of our changelists perhaps
 * (See also {@link P4Edit P4Edit})?<br>
 *
 *
 * @author <A HREF="mailto:mike@tmorph.com">Mike Roberts</A> , <A
 *      HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */
public class P4Delete extends P4Base
{

    public String change = null;

    public void setChange( String change )
    {
        this.change = change;
    }

    public void execute()
        throws TaskException
    {
        if( change != null )
            P4CmdOpts = "-c " + change;
        if( P4View == null )
            throw new TaskException( "No view specified to delete" );
        execP4Command( "-s delete " + P4CmdOpts + " " + P4View, new SimpleP4OutputHandler( this ) );
    }
}
