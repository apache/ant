/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.api;

import java.io.File;
import org.apache.avalon.framework.Enum;
import org.apache.avalon.framework.context.Context;

/**
 * This interface represents the <em>Context</em> in which Task is executed.
 * Like other Component APIs the TaskContext represents the communication
 * path between the container and the Task.
 * Unlike other APIs the Logging is provided through another interface (Loggable)
 * as is access to Peer components (via Composable).
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface TaskContext
    extends Context
{
    //these values are used when setting properties to indicate the scope at
    //which properties are set
    ScopeEnum       CURRENT            = new ScopeEnum( "Current" );
    ScopeEnum       PARENT             = new ScopeEnum( "Parent" );
    ScopeEnum       TOP_LEVEL          = new ScopeEnum( "TopLevel" );

    //these are the names of properties that every TaskContext must contain
    String          JAVA_VERSION       = "myrmidon.java.version";
    String          BASE_DIRECTORY     = "myrmidon.base.directory";
    String          NAME               = "myrmidon.task.name";

    /**
     * Retrieve JavaVersion running under.
     *
     * @return the version of JVM
     */
    JavaVersion getJavaVersion();

    /**
     * Retrieve Name of tasklet.
     *
     * @return the name
     */
    String getName();

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
     * Resolve property.
     * This evaluates all property substitutions based on current context.
     *
     * @param property the property to resolve
     * @return the resolved property
     */
    Object resolveValue( String property )
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

