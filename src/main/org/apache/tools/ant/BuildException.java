/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant;

import java.io.PrintWriter;
import java.io.PrintStream;

/**
 * Signals an error condition during a build
 *
 * @author James Duncan Davidson
 */
public class BuildException extends RuntimeException {

    /** Exception that might have caused this one. */
    private Throwable cause;

    /** Location in the build file where the exception occured */
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
    public Throwable getCause() {
        return cause;
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
