/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import java.util.HashMap;
import java.util.Set;
import org.apache.avalon.framework.Enum;
import org.apache.avalon.framework.logger.Logger;

/**
 * Type safe Enum for Log Levels and utility method
 * for using enum to write to logger.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public final class LogLevel
    extends Enum
{
    //Map for all the levels
    private final static HashMap c_levels = new HashMap();

    //standard enums for version of JVM
    public final static LogLevel FATAL_ERROR = new LogLevel( "fatalError" );
    public final static LogLevel ERROR = new LogLevel( "error" );
    public final static LogLevel WARN = new LogLevel( "warn" );
    public final static LogLevel INFO = new LogLevel( "info" );
    public final static LogLevel DEBUG = new LogLevel( "debug" );

    /**
     * Retrieve the log level for the specified name.
     *
     * @param name the name of the LogLevel object to retrieve
     * @returns The LogLevel for specified name or null
     */
    public static LogLevel getByName( final String name )
    {
        return (LogLevel)c_levels.get( name );
    }

    /**
     * Retrieve the names of all the LogLevels.
     *
     * @returns The names of all the LogLevels
     */
    public static String[] getNames()
    {
        final Set keys = c_levels.keySet();
        return (String[])keys.toArray( new String[ keys.size() ] );
    }

    /**
     * Log a message to the Logger at the specified level.
     */
    public static void log( final Logger logger,
                            final String message,
                            final LogLevel level )
    {
        if( LogLevel.FATAL_ERROR == level )
        {
            logger.fatalError( message );
        }
        else if( LogLevel.ERROR == level )
        {
            logger.error( message );
        }
        else if( LogLevel.WARN == level )
        {
            logger.warn( message );
        }
        else if( LogLevel.INFO == level )
        {
            logger.info( message );
        }
        else
        {
            logger.debug( message );
        }
    }

    /**
     * Log a message to the Logger at the specified level.
     */
    public static void log( final Logger logger,
                            final String message,
                            final Throwable throwable,
                            final LogLevel level )
    {
        if( LogLevel.FATAL_ERROR == level )
        {
            logger.fatalError( message, throwable );
        }
        else if( LogLevel.ERROR == level )
        {
            logger.error( message, throwable );
        }
        else if( LogLevel.WARN == level )
        {
            logger.warn( message, throwable );
        }
        else if( LogLevel.INFO == level )
        {
            logger.info( message, throwable );
        }
        else
        {
            logger.debug( message, throwable );
        }
    }

    /**
     * Private constructor so no instance except here can be defined.
     *
     * @param name the name of Log Level
     */
    private LogLevel( final String name )
    {
        super( name, c_levels );
    }
}
