/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.tasklet;

import java.io.File;
import org.apache.avalon.Context;
import org.apache.avalon.util.Enum;
import org.apache.avalon.util.ValuedEnum;
import org.apache.log.Logger;

/**
 * This represents the *Context* in which a task can be executed.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public interface TaskletContext
    extends Context
{
    //these values are used when setting properties to indicate the scope at
    //which properties are set
    ScopeEnum       CURRENT            = new ScopeEnum( "Current" );
    ScopeEnum       PARENT             = new ScopeEnum( "Parent" );
    ScopeEnum       TOP_LEVEL          = new ScopeEnum( "TopLevel" );

    //these are the names of properties that every TaskContext must contain
    String          JAVA_VERSION       = "ant.java.version";
    String          BASE_DIRECTORY     = "ant.base.directory";
    String          LOGGER             = "ant.logger";
    String          NAME               = "ant.task.name";

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
     * Retrieve Logger associated with task.
     *
     * @return the logger
     */
    Logger getLogger();
    
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
     * @return the resolved filename
     */
    String resolveFilename( String filename );

    /**
     * Resolve property. 
     * This evaluates all property substitutions based on current context.
     *
     * @param property the property to resolve
     * @return the resolved property
     */
    Object resolveValue( String property );

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
    void setProperty( String name, Object value );
    
    /**
     * Set property value.
     *
     * @param name the name of property
     * @param value the value of property
     * @param scope the scope at which to set property
     */
    void setProperty( String name, Object value, ScopeEnum scope  );

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

