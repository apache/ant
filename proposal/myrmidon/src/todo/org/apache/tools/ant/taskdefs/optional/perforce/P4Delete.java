/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
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
        {
            m_p4CmdOpts = "-c " + change;
        }
        if( m_p4View == null )
        {
            throw new TaskException( "No view specified to delete" );
        }

        final String command = "-s delete " + m_p4CmdOpts + " " + m_p4View;
        execP4Command( command, null );
    }
}
