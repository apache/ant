/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.api;

import java.io.File;
import org.apache.avalon.framework.Enum;
import org.apache.avalon.framework.context.Context;

/**
 * This interface represents the <em>Context</em> in which Task is executed.
 * Like other Component APIs the TaskContext represents the communication
 * path between the container and the Task.
 * Unlike other APIs the Logging is provided through another interface (LogEnabled)
 * as is access to Peer components (via Composable).
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public interface TaskContext
    extends Context
{
    //these values are used when setting properties to indicate the scope at
    //which properties are set
    ScopeEnum CURRENT = new ScopeEnum( "Current" );
    ScopeEnum PARENT = new ScopeEnum( "Parent" );
    ScopeEnum TOP_LEVEL = new ScopeEnum( "TopLevel" );

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
     * Resolve a value according to the context.
     * This involves evaluating the string and thus removing
     * ${} sequences according to the rules specified at
     * ............
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
     * @return the value of property
     */
    Object getProperty( String name );

    /**
     * Set property value in current context.
     *
     * @param name the name of property
     * @param value the value of property
     */
    void setProperty( String name, Object value )
        throws TaskException;

    /**
     * Set property value.
     *
     * @param name the name of property
     * @param value the value of property
     * @param scope the scope at which to set property
     */
    void setProperty( String name, Object value, ScopeEnum scope )
        throws TaskException;

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

    /**
     * Safe wrapper class for Scope enums.
     */
    final class ScopeEnum
        extends Enum
    {
        ScopeEnum( final String name )
        {
            super( name );
        }
    }
}

