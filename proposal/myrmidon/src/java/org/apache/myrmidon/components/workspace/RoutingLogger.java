/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import org.apache.avalon.framework.logger.Logger;
import org.apache.myrmidon.frontends.AbstractLogger;

/**
 * A logger that just routes the messages to the ProjectListenerSupport.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
final class RoutingLogger
    extends AbstractLogger
    implements Logger
{
    /**
     * The endpoint of all the logging messages.
     */
    private final ProjectListenerSupport m_listenerSupport;

    /**
     * A wrapped logger that is used to determine which message types are
     * enabled.
     */
    private final Logger m_logger;

    /**
     * Create a Logger that routes messages at specified level
     * to specified support.
     *
     * @todo Use something other than a logger to figure out which messages
     *       are enabled.
     */
    public RoutingLogger( final Logger logger,
                          final ProjectListenerSupport listenerSupport )
    {
        m_listenerSupport = listenerSupport;
        m_logger = logger;
    }

    public boolean isDebugEnabled()
    {
        return m_logger.isDebugEnabled();
    }

    public boolean isInfoEnabled()
    {
        return m_logger.isInfoEnabled();
    }

    public boolean isWarnEnabled()
    {
        return m_logger.isWarnEnabled();
    }

    public boolean isErrorEnabled()
    {
        return m_logger.isErrorEnabled();
    }

    public boolean isFatalErrorEnabled()
    {
        return m_logger.isFatalErrorEnabled();
    }

    public Logger getChildLogger( final String name )
    {
        return new RoutingLogger( m_logger.getChildLogger( name ), m_listenerSupport );
    }

    /**
     * Utility method to output messages.
     */
    protected void output( final String message, final Throwable throwable )
    {
        m_listenerSupport.log( message, throwable );
    }
}
