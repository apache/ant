/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasks.core;

import org.apache.ant.AntException;
import org.apache.ant.tasklet.AbstractTasklet;
import org.apache.ant.tasklet.TaskletContext;

/**
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Property 
    extends AbstractTasklet
{
    protected String              m_name;
    protected String              m_value;
    protected boolean             m_localScope     = true;

    public void setName( final String name )
    {
        m_name = name;
    }
    
    public void setValue( final String value )
    {
        m_value = value;
    }
    
    public void setLocalScope( final boolean localScope )
    {
        m_localScope = localScope;
    }

    public void run()
        throws AntException
    {
        if( null == m_name )
        {
            throw new AntException( "Name must be specified" );
        }

        if( null == m_value )
        {
            throw new AntException( "Value must be specified" );
        }

        final TaskletContext context = getContext();
        final Object value = context.resolveValue( m_value );

        if( m_localScope )
        {
            context.setProperty( m_name, value );
        }
        else
        {
            context.setProperty( m_name, value, TaskletContext.PARENT );
        }
    }
}
