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
import org.apache.myrmidon.api.DefaultTaskContext;
import org.apache.myrmidon.api.TaskContext;

/**
 * Default project implementation.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultProject
    implements Project
{
    protected final TaskContext          m_baseContext     = new DefaultTaskContext();
    protected final HashMap              m_targets         = new HashMap();
    protected Target                     m_implicitTarget;
    protected String                     m_defaultTarget;
    
    /**
     * Retrieve implicit target. 
     * The implicit target is top level tasks. 
     * Currently restricted to property tasks.
     *
     * @return the Target
     */
    public Target getImplicitTarget()
    {
        return m_implicitTarget;
    }

    /**
     * Set ImplicitTarget.
     *
     * @param target the implicit target
     */
    public void setImplicitTarget( final Target target )
    {
        m_implicitTarget = target;
    }
    
    /**
     * Retrieve a target by name.
     *
     * @param name the name of target
     * @return the Target or null if no target exists with name
     */
    public Target getTarget( final String targetName )
    {
        return (Target)m_targets.get( targetName );
    }
    
    /**
     * Get name of default target.
     *
     * @return the default target name
     */
    public String getDefaultTargetName()
    {
        return m_defaultTarget;
    }
    
    /**
     * Retrieve names of all targets in project.
     *
     * @return the iterator of project names
     */
    public Iterator getTargetNames()
    {
        return m_targets.keySet().iterator();
    }
    
    /**
     * Get project (top-level) context.
     *
     * @return the context
     */    
    public TaskContext getContext()
    {
        return m_baseContext;
    }

    /**
     * Set DefaultTargetName.
     *
     * @param defaultTarget the default target name
     */
    public void setDefaultTargetName( final String defaultTarget )
    {
        m_defaultTarget = defaultTarget;
    }

    /**
     * Add a target to project.
     *
     * @param name the name of target
     * @param target the Target
     * @exception AntException if an error occurs
     */
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


