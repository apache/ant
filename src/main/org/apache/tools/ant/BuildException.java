/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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


import java.io.*;

/**
 * Signals an error condition during a build.
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
     * @param msg Description of or information about the exception.
     */
    public BuildException(String msg) {
        super(msg);
    }

    /**
     * Constructs an exception with the given message and exception as
     * a root cause.
     * @param msg Description of or information about the exception.
     * @param cause Throwable that might have cause this one.
     */
    public BuildException(String msg, Throwable cause) {
        super(msg);
        this.cause = cause;
    }

    /**
     * Constructs an exception with the given message and exception as
     * a root cause and a location in a file.
     * @param msg Description of or information about the exception.
     * @param cause Exception that might have cause this one.
     * @param location Location in the project file where the error occured.
     */
    public BuildException(String msg, Throwable cause, Location location) {
        this(msg, cause);
        this.location = location;
    }

    /**
     * Constructs an exception with the given exception as a root cause.
     * @param cause Exception that might have caused this one.
     */
    public BuildException(Throwable cause) {
        super(cause.toString());
        this.cause = cause;
    }

    /**
     * Constructs an exception with the given descriptive message and a location
     * in a file.
     * @param msg Description of or information about the exception.
     * @param location Location in the project file where the error occured.
     */
    public BuildException(String msg, Location location) {
        super(msg);
        this.location = location;
    }

    /**
     * Constructs an exception with the given exception as
     * a root cause and a location in a file.
     * @param cause Exception that might have cause this one.
     * @param location Location in the project file where the error occured.
     */
    public BuildException(Throwable cause, Location location) {
        this(cause);
        this.location = location;
    }

    /**
     * Returns the nested exception.
     */
    public Throwable getException() {
        return cause;
    }

    /**
     * Returns the location of the error and the error message.
     */
    public String toString() {
        return location.toString() + getMessage();
    }

    /**
     * Sets the file location where the error occured.
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Returns the file location where the error occured.
     */
    public Location getLocation() {
        return location;
    }

    // Override stack trace methods to show original cause:
    public void printStackTrace() {
        printStackTrace(System.err);
    }
    
    public void printStackTrace(PrintStream ps) {
        synchronized (ps) {
            ps.println(this);
            if (cause != null) {
                ps.println("--- Nested Exception ---");
                cause.printStackTrace(ps);
            }
        }
    }
    
    public void printStackTrace(PrintWriter pw) {
        synchronized (pw) {
            pw.println(this);
            if (cause != null) {
                pw.println("--- Nested Exception ---");
                cause.printStackTrace(pw);
            }
        }
    }
}
