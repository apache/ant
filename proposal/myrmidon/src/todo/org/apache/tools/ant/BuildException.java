/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;

import org.apache.myrmidon.api.TaskException;

/**
 * Signals an error condition during a build.
 *
 * @author James Duncan Davidson
 */
public class BuildException
    extends TaskException
{
    /**
     * Location in the build file where the exception occured
     */
    private Location location = Location.UNKNOWN_LOCATION;

    /**
     * Constructs an exception with the given descriptive message.
     *
     * @param msg Description of or information about the exception.
     */
    public BuildException( String msg )
    {
        super( msg );
    }

    /**
     * Constructs an exception with the given message and exception as a root
     * cause.
     *
     * @param msg Description of or information about the exception.
     * @param cause Throwable that might have cause this one.
     */
    public BuildException( String msg, Throwable cause )
    {
        super( msg, cause );
    }

    /**
     * Constructs an exception with the given message and exception as a root
     * cause and a location in a file.
     *
     * @param msg Description of or information about the exception.
     * @param cause Exception that might have cause this one.
     * @param location Location in the project file where the error occured.
     */
    public BuildException( String msg, Throwable cause, Location location )
    {
        this( msg, cause );
        this.location = location;
    }

    /**
     * Constructs an exception with the given exception as a root cause.
     *
     * @param cause Exception that might have caused this one.
     */
    public BuildException( Throwable cause )
    {
        super( cause.toString(), cause );
    }

    /**
     * Constructs an exception with the given descriptive message and a location
     * in a file.
     *
     * @param msg Description of or information about the exception.
     * @param location Location in the project file where the error occured.
     */
    public BuildException( String msg, Location location )
    {
        super( msg );
        this.location = location;
    }

    /**
     * Sets the file location where the error occured.
     *
     * @param location The new Location value
     */
    public void setLocation( Location location )
    {
        this.location = location;
    }

    /**
     * Returns the file location where the error occured.
     *
     * @return The Location value
     */
    public Location getLocation()
    {
        return location;
    }
}
