/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.perforce;

import org.apache.myrmidon.api.TaskException;

/**
 * P4Have - lists files currently on client. P4Have simply dumps the current
 * file version info into the Ant log (or stdout).
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */
public class P4Have
    extends P4Base
{
    public void execute()
        throws TaskException
    {
        final String command = "have " + m_p4CmdOpts + " " + m_p4View;
        execP4Command( command, null );
    }
}
