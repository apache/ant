/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.frontends;

import org.apache.avalon.framework.ExceptionUtil;
import org.apache.avalon.framework.logger.Logger;

/**
 * A basic logger that just prints out messages to <code>System.out</code>.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class BasicLogger
    implements Logger
{
    public final static int LEVEL_DEBUG = 0;
    public final static int LEVEL_INFO = 1;
    public final static int LEVEL_WARN = 2;
    public final static int LEVEL_ERROR = 3;
    public final static int LEVEL_FATAL = 4;

    /**
     * The string prefixed to all log messages.
     */
    private final String m_prefix;

    /**
     * The level at which messages start becoming logged.
     */
    private final int m_logLevel;

    /**
     * Create a logger that has specified prefix and is logging
     * at specified level.
     */
    public BasicLogger( final String prefix, final int logLevel )
    {
        m_prefix = prefix;
        m_logLevel = logLevel;
    }

    /**
     * Log a debug message.
     *
     * @param message the message
     */
    public void debug( final String message )
    {
        if( isDebugEnabled() )
        {
            output( message, null );
        }
    }

    /**
     * Log a debug message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void debug( final String message, final Throwable throwable )
    {
        if( isDebugEnabled() )
        {
            output( message, throwable );
        }
    }

    /**
     * Determine if messages of priority "debug" will be logged.
     *
     * @return true if "debug" messages will be logged
     */
    public boolean isDebugEnabled()
    {
        return m_logLevel <= LEVEL_DEBUG;
    }

    /**
     * Log a info message.
     *
     * @param message the message
     */
    public void info( final String message )
    {
        if( isInfoEnabled() )
        {
            output( message, null );
        }
    }

    /**
     * Log a info message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void info( final String message, final Throwable throwable )
    {
        if( isInfoEnabled() )
        {
            output( message, throwable );
        }
    }

    /**
     * Determine if messages of priority "info" will be logged.
     *
     * @return true if "info" messages will be logged
     */
    public boolean isInfoEnabled()
    {
        return m_logLevel <= LEVEL_INFO;
    }

    /**
     * Log a warn message.
     *
     * @param message the message
     */
    public void warn( final String message )
    {
        if( isWarnEnabled() )
        {
            output( message, null );
        }
    }

    /**
     * Log a warn message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void warn( final String message, final Throwable throwable )
    {
        if( isWarnEnabled() )
        {
            output( message, throwable );
        }
    }

    /**
     * Determine if messages of priority "warn" will be logged.
     *
     * @return true if "warn" messages will be logged
     */
    public boolean isWarnEnabled()
    {
        return m_logLevel <= LEVEL_WARN;
    }

    /**
     * Log a error message.
     *
     * @param message the message
     */
    public void error( final String message )
    {
        if( isErrorEnabled() )
        {
            output( message, null );
        }
    }

    /**
     * Log a error message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void error( final String message, final Throwable throwable )
    {
        if( isErrorEnabled() )
        {
            output( message, throwable );
        }
    }

    /**
     * Determine if messages of priority "error" will be logged.
     *
     * @return true if "error" messages will be logged
     */
    public boolean isErrorEnabled()
    {
        return m_logLevel <= LEVEL_ERROR;
    }

    /**
     * Log a fatalError message.
     *
     * @param message the message
     */
    public void fatalError( final String message )
    {
        if( isFatalErrorEnabled() )
        {
            output( message, null );
        }
    }

    /**
     * Log a fatalError message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void fatalError( final String message, final Throwable throwable )
    {
        if( isFatalErrorEnabled() )
        {
            output( message, throwable );
        }
    }

    /**
     * Determine if messages of priority "fatalError" will be logged.
     *
     * @return true if "fatalError" messages will be logged
     */
    public boolean isFatalErrorEnabled()
    {
        return m_logLevel <= LEVEL_FATAL;
    }

    /**
     * Create a new child logger.
     * The name of the child logger is [current-loggers-name].[passed-in-name]
     *
     * @param name the subname of this logger
     * @return the new logger
     * @exception IllegalArgumentException if name has an empty element name
     */
    public Logger getChildLogger( final String name )
    {
        return new BasicLogger( m_prefix + "." + name, m_logLevel );
    }

    /**
     * Utility method to output messages.
     */
    protected void output( final String message, final Throwable throwable )
    {
        final StringBuffer sb = new StringBuffer( m_prefix );
        if( null != message )
        {
            sb.append( message );
        }

        if( null != throwable )
        {
            final String stackTrace = ExceptionUtil.printStackTrace( throwable, 8, true, true );
            sb.append( stackTrace );
        }

        System.out.println( sb.toString() );
    }
}
