/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.net;

import org.apache.myrmidon.api.TaskException;

/**
 * This class sends text to the connected server
 */
public class TelnetWrite
    extends TelnetSubTask
{
    private boolean m_echo = true;

    public void setEcho( final boolean echo )
    {
        m_echo = echo;
    }

    public void execute( final AntTelnetClient telnet )
        throws TaskException
    {
        telnet.sendString( getTaskString(), m_echo );
    }
}
