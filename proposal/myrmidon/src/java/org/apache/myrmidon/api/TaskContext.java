/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.api;

import java.io.File;
import java.util.Map;

/**
 * This interface represents the <em>Context</em> in which Task is executed.
 * Like other Component APIs the TaskContext represents the communication
 * path between the container and the Task.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface TaskContext
{
    //these are the names of properties that every TaskContext must contain
    String BASE_DIRECTORY = "myrmidon.base.directory";
    String NAME = "myrmidon.task.name";

    /**
     * Retrieve Name of task.
     *
     * @return the name
     */
    String getName();

    /**
     * Resolve a value according to the context.
     * This involves evaluating the string and replacing
     * ${} sequences with property values.
     *
     * @param value the value to resolve
     * @return the resolved value
     */
    Object resolveValue( String value )
        throws TaskException;

    /**
     * Retrieve property for name.
     *
     * @param name the name of property
     * @return the value of property, or null if the property has no value.
     */
    Object getProperty( String name );

    /**
     * Retrieve a copy of all the properties accessible via context.
     *
     * @return the map of all property names to values
     */
    Map getProperties()
        throws TaskException;

    /**
     * Retrieve a service that is offered by the runtime.
     * The actual services registered and in place for the
     * task is determined by the container. The returned service
     * <b>MUST</b> implement the specified interface.
     *
     * @param serviceClass the interface class that defines the service
     * @return an instance of the service implementing interface specified by parameter
     * @exception TaskException is thrown when the service is unavailable or not supported
     */
    Object getService( Class serviceClass )
        throws TaskException;

    /**
     * Retrieve base directory.
     *
     * @return the base directory
     */
    File getBaseDirectory();

    /**
     * Resolve filename.
     * This involves resolving it against baseDirectory and
     * removing ../ and ./ references. It also means formatting
     * it appropriately for the particular OS (ie different OS have
     * different volumes, file conventions etc)
     *
     * @param filename the filename to resolve
     * @return the resolved file
     */
    File resolveFile( String filename )
        throws TaskException;

    /**
     * Set property value in current context.
     *
     * @param name the name of property
     * @param value the value of property
     */
    void setProperty( String name, Object value )
        throws TaskException;

    /**
     * Log a debug message.
     *
     * @param message the message
     */
    void debug( String message );

    /**
     * Log a debug message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    void debug( String message, Throwable throwable );

    /**
     * Determine if messages of priority "debug" will be logged.
     *
     * @return true if "debug" messages will be logged
     */
    boolean isDebugEnabled();

    /**
     * Log a verbose message.
     *
     * @param message the message
     */
    void verbose( String message );

    /**
     * Log a verbose message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    void verbose( String message, Throwable throwable );

    /**
     * Determine if messages of priority "verbose" will be logged.
     *
     * @return true if "verbose" messages will be logged
     */
    boolean isVerboseEnabled();

    /**
     * Log a info message.
     *
     * @param message the message
     */
    void info( String message );

    /**
     * Log a info message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    void info( String message, Throwable throwable );

    /**
     * Determine if messages of priority "info" will be logged.
     *
     * @return true if "info" messages will be logged
     */
    boolean isInfoEnabled();

    /**
     * Log a warn message.
     *
     * @param message the message
     */
    void warn( String message );

    /**
     * Log a warn message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    void warn( String message, Throwable throwable );

    /**
     * Determine if messages of priority "warn" will be logged.
     *
     * @return true if "warn" messages will be logged
     */
    boolean isWarnEnabled();

    /**
     * Log a error message.
     *
     * @param message the message
     */
    void error( String message );

    /**
     * Log a error message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    void error( String message, Throwable throwable );

    /**
     * Determine if messages of priority "error" will be logged.
     *
     * @return true if "error" messages will be logged
     */
    boolean isErrorEnabled();

    /**
     * Create a Child Context.
     * This allows separate hierarchly contexts to be easily constructed.
     *
     * @param name the name of sub-context
     * @return the created TaskContext
     * @exception TaskException if an error occurs
     */
    TaskContext createSubContext( String name )
        throws TaskException;
}

