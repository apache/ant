/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.frontends;

import org.apache.avalon.framework.logger.Logger;

/**
 * A partial logger implementation.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractLogger
    implements Logger
{
    public static final int LEVEL_DEBUG = 0;
    public static final int LEVEL_INFO = 1;
    public static final int LEVEL_WARN = 2;
    public static final int LEVEL_ERROR = 3;
    public static final int LEVEL_FATAL = 4;

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
     * Utility method to output messages.
     */
    protected abstract void output( final String message,
                                    final Throwable throwable );
}
