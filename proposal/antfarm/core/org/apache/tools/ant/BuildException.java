/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

/**
 *  Indicates that an error during the build, such as a compiler error,
 *  a typo in a build file, etc. Errors resulting from coding
 *  errors within ant or a misconfigured setup should use
 *  AntException.
 *
 *  @see AntException
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class BuildException extends Exception {
    private String location;

    /**
     *  Constructs a new exception with the specified message.
     */
    public BuildException(String message) {
        super(message);
    }

    /**
     *  Constructs a new exception with the specified message and location.
     */
    public BuildException(String message, String location) {
        super(message);

        this.location = location;
    }

    /**
     *  Returns the location in the build file where this error.
     *  occured.
     */
    public String getLocation() {
        return location;
    }

    /**
     *  Sets the location in the build file where this error occured.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    public String toString() {
        return (location == null) ? getMessage() : (location + ": " + getMessage());
    }
}