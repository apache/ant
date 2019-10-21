/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant;

/**
 * Signals an error condition during a build
 */
public class BuildException extends RuntimeException {

    private static final long serialVersionUID = -5419014565354664240L;

    /** Location in the build file where the exception occurred */
    private Location location = Location.UNKNOWN_LOCATION;

    /**
     * Constructs a build exception with no descriptive information.
     */
    public BuildException() {
        super();
    }

    /**
     * Constructs an exception with the given descriptive message.
     *
     * @param message A description of or information about the exception.
     *            Should not be {@code null}.
     */
    public BuildException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the given format pattern and arguments.
     *
     * @param pattern A description of or information about the exception.
     *            Should not be {@code null}.
     * @param formatArguments ditto
     * @see String#format(String, Object...)
     * @since Ant 1.10.2
     */
    public BuildException(String pattern, Object... formatArguments) {
        super(String.format(pattern, formatArguments));
    }

    /**
     * Constructs an exception with the given message and exception as
     * a root cause.
     *
     * @param message A description of or information about the exception.
     *            Should not be <code>null</code> unless a cause is specified.
     * @param cause The exception that might have caused this one.
     *              May be <code>null</code>.
     */
    public BuildException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an exception with the given message and exception as
     * a root cause and a location in a file.
     *
     * @param message A description of or information about the exception.
     *                Should not be <code>null</code> unless a cause is specified.
     * @param cause The exception that might have caused this one.
     *              May be <code>null</code>.
     * @param location The location in the project file where the error
     *                 occurred. Must not be <code>null</code>.
     */
    public BuildException(String message, Throwable cause, Location location) {
        this(message, cause);
        this.location = location;
    }

    /**
     * Constructs an exception with the given exception as a root cause.
     *
     * @param cause The exception that might have caused this one.
     *              Should not be <code>null</code>.
     */
    public BuildException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an exception with the given descriptive message and a
     * location in a file.
     *
     * @param message A description of or information about the exception.
     *            Should not be <code>null</code>.
     * @param location The location in the project file where the error
     *                 occurred. Must not be <code>null</code>.
     */
    public BuildException(String message, Location location) {
        super(message);
        this.location = location;
    }

    /**
     * Constructs an exception with the given exception as
     * a root cause and a location in a file.
     *
     * @param cause The exception that might have caused this one.
     *              Should not be <code>null</code>.
     * @param location The location in the project file where the error
     *                 occurred. Must not be <code>null</code>.
     */
    public BuildException(Throwable cause, Location location) {
        this(cause);
        this.location = location;
    }

    /**
     * Returns the nested exception, if any.
     *
     * @return the nested exception, or <code>null</code> if no
     *         exception is associated with this one
     * @deprecated Use {@link #getCause} instead.
     */
    @Deprecated
    public Throwable getException() {
        return getCause();
    }

    /**
     * Returns the location of the error and the error message.
     *
     * @return the location of the error and the error message
     */
    public String toString() {
        return location.toString() + getMessage();
    }

    /**
     * Sets the file location where the error occurred.
     *
     * @param location The file location where the error occurred.
     *                 Must not be <code>null</code>.
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Returns the file location where the error occurred.
     *
     * @return the file location where the error occurred.
     */
    public Location getLocation() {
        return location;
    }

}
