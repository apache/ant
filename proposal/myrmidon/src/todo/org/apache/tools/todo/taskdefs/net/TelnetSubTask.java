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
 * This class is the parent of the Read and Write tasks. It handles the
 * common attributes for both.
 */
public abstract class TelnetSubTask
{
    private String m_taskString = "";

    public void setString( final String string )
    {
        m_taskString += string;
    }

    public void addContent( final String string )
    {
        setString( string );
    }

    public void execute( AntTelnetClient telnet )
        throws TaskException
    {
        throw new TaskException( "Shouldn't be able instantiate a SubTask directly" );
    }

    protected final String getTaskString()
    {
        return m_taskString;
    }
}
