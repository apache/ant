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
import org.apache.myrmidon.api.TaskContext;

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
    private static final HashMap c_levels = new HashMap();

    //standard enums for version of JVM
    public static final LogLevel ERROR = new LogLevel( "error" );
    public static final LogLevel WARN = new LogLevel( "warn" );
    public static final LogLevel INFO = new LogLevel( "info" );
    public static final LogLevel VERBOSE = new LogLevel( "verbose" );
    public static final LogLevel DEBUG = new LogLevel( "debug" );

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
     * Log a message.
     *
     * @param level the level to write the log message at.
     * @param message the message to write.
     */
    public static void log( final TaskContext context,
                            final LogLevel level,
                            final String message )
    {
        if( ERROR == level )
        {
            context.error( message );
        }
        else if( WARN == level )
        {
            context.warn( message );
        }
        else if( INFO == level )
        {
            context.info( message );
        }
        else if( VERBOSE == level )
        {
            context.verbose( message );
        }
        else
        {
            context.debug( message );
        }
    }

    /**
     * Log a message.
     *
     * @param level the level to write the log message at.
     * @param message the message to write.
     * @param throwable the throwable.
     */
    public static void log( final TaskContext context,
                            final LogLevel level,
                            final String message,
                            final Throwable throwable )
    {
        if( ERROR == level )
        {
            context.error( message, throwable );
        }
        else if( WARN == level )
        {
            context.warn( message, throwable );
        }
        else if( INFO == level )
        {
            context.info( message, throwable );
        }
        else if( VERBOSE == level )
        {
            context.verbose( message, throwable );
        }
        else
        {
            context.debug( message, throwable );
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
