/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import org.apache.myrmidon.frontends.BasicLogger;

/**
 * A basic logger that just routes the messages to the ProjectListenerSupport.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
final class RoutingLogger
    extends BasicLogger
{
    /**
     * The endpoint of all the logging messages.
     */
    private final ProjectListenerSupport m_listenerSupport;

    /**
     * Create a Logger that routes messages at specified level
     * to specified support.
     */
    public RoutingLogger( final int logLevel,
                          final ProjectListenerSupport listenerSupport )
    {
        super( null, logLevel );
        m_listenerSupport = listenerSupport;
    }

    /**
     * Utility method to output messages.
     */
    protected void output( final String message, final Throwable throwable )
    {
        m_listenerSupport.log( message, throwable );
    }
}
