/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Signals an error condition during a build
 */
public class BuildException extends RuntimeException {

    private static final long serialVersionUID = -5419014565354664240L;

    /** Exception that might have caused this one. */
    private Throwable cause;

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
     *            Should not be <code>null</code>.
     */
    public BuildException(String message) {
        super(message);
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
        super(message);
        this.cause = cause;
    }

    /**
     * Constructs an exception with the given message and exception as
     * a root cause and a location in a file.
     *
     * @param msg A description of or information about the exception.
     *            Should not be <code>null</code> unless a cause is specified.
     * @param cause The exception that might have caused this one.
     *              May be <code>null</code>.
     * @param location The location in the project file where the error
     *                 occurred. Must not be <code>null</code>.
     */
    public BuildException(String msg, Throwable cause, Location location) {
        this(msg, cause);
        this.location = location;
    }

    /**
     * Constructs an exception with the given exception as a root cause.
     *
     * @param cause The exception that might have caused this one.
     *              Should not be <code>null</code>.
     */
    public BuildException(Throwable cause) {
        super(cause.toString());
        this.cause = cause;
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
     */
    public Throwable getException() {
        return cause;
    }

    /**
     * Returns the nested exception, if any.
     *
     * @return the nested exception, or <code>null</code> if no
     *         exception is associated with this one
     */
    public Throwable getCause() {
        return getException();
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

    /**
     * Prints the stack trace for this exception and any
     * nested exception to <code>System.err</code>.
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints the stack trace of this exception and any nested
     * exception to the specified PrintStream.
     *
     * @param ps The PrintStream to print the stack trace to.
     *           Must not be <code>null</code>.
     */
    public void printStackTrace(PrintStream ps) {
        synchronized (ps) {
            super.printStackTrace(ps);
            if (cause != null) {
                ps.println("--- Nested Exception ---");
                cause.printStackTrace(ps);
            }
        }
    }

    /**
     * Prints the stack trace of this exception and any nested
     * exception to the specified PrintWriter.
     *
     * @param pw The PrintWriter to print the stack trace to.
     *           Must not be <code>null</code>.
     */
    public void printStackTrace(PrintWriter pw) {
        synchronized (pw) {
            super.printStackTrace(pw);
            if (cause != null) {
                pw.println("--- Nested Exception ---");
                cause.printStackTrace(pw);
            }
        }
    }
}
