/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.ant.AntException;
import org.apache.ant.tasklet.DefaultTaskletContext;
import org.apache.ant.tasklet.TaskletContext;

public class DefaultProject
    implements Project
{
    protected final TaskletContext       m_baseContext     = new DefaultTaskletContext();
    protected final HashMap              m_targets         = new HashMap();
    protected Target                     m_implicitTarget;
    protected String                     m_defaultTarget;

    public Target getImplicitTarget()
    {
        return m_implicitTarget;
    }

    public void setImplicitTarget( final Target target )
    {
        m_implicitTarget = target;
    }

    public Target getTarget( final String targetName )
    {
        return (Target)m_targets.get( targetName );
    }

    public String getDefaultTargetName()
    {
        return m_defaultTarget;
    }

    public Iterator getTargetNames()
    {
        return m_targets.keySet().iterator();
    }
    
    public TaskletContext getContext()
    {
        return m_baseContext;
    }

    public void setDefaultTargetName( final String defaultTarget )
    {
        m_defaultTarget = defaultTarget;
    }

    public void addTarget( final String name, final Target target )
        throws AntException
    {
        if( null != m_targets.get( name ) )
        {
            throw new AntException( "Can not have two targets in a file with the name " +
                                    name );
        }
        else
        {
            m_targets.put( name, target );
        }
    }
}


