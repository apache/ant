/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.manager;

import org.apache.log.LogEvent;
import org.apache.log.LogTarget;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * Adapter between Avalon LogKit and Project listener interfaces.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class LogTargetToListenerAdapter
    implements LogTarget
{
    private final ProjectListener    m_listener;

    /**
     * Constructor taking listener to convert to.
     *
     * @param listener the ProjectListener
     */
    public LogTargetToListenerAdapter( final ProjectListener listener )
    {
        m_listener = listener;
    }

    /**
     * Process a log event.
     *
     * @param event the event
     */
    public void processEvent( final LogEvent event )
    {
        if( null == event.getThrowable() )
        {
            m_listener.log( event.getMessage() );
        }
        else
        {
            m_listener.log( event.getMessage(), event.getThrowable() );
        }
    }
}
