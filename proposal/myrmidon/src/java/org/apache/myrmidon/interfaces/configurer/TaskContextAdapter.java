/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.configurer;

import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.myrmidon.api.TaskContext;

/**
 * This class adpats the TaskContext API to the Avalon Context API.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class TaskContextAdapter
    implements Context
{
    private final TaskContext m_context;

    public TaskContextAdapter( final TaskContext context )
    {
        m_context = context;
    }

    public Object get( Object key )
        throws ContextException
    {
        final Object value = m_context.getProperty( key.toString() );
        if( null != value )
        {
            return value;
        }
        else
        {
            throw new ContextException( "Missing key " + key );
        }
    }
}
