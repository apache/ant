/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import org.apache.log.LogEvent;
import org.apache.log.LogTarget;

/**
 * Adapter between Avalon LogKit and Project listener interfaces.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class LogTargetToListenerAdapter
    implements LogTarget
{
    private final ProjectListenerSupport m_listenerSupport;

    /**
     * Constructor taking listener to convert to.
     *
     * @param listenerSupport the ProjectListener
     */
    public LogTargetToListenerAdapter( final ProjectListenerSupport listenerSupport )
    {
        m_listenerSupport = listenerSupport;
    }

    /**
     * Process a log event.
     *
     * @param event the event
     */
    public void processEvent( final LogEvent event )
    {
        m_listenerSupport.log( event.getMessage(), event.getThrowable() );
    }
}
