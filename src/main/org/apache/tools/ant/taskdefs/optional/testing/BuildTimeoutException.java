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

package org.apache.tools.ant.taskdefs.optional.testing;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;

/**
 *
 * This exception is used to indicate timeouts.
 * @since Ant1.8
 *
 */

public class BuildTimeoutException extends BuildException {

    private static final long serialVersionUID = -8057644603246297562L;

    /**
     * Constructs a build exception with no descriptive information.
     */
    public BuildTimeoutException() {
    }

    /**
     * Constructs an exception with the given descriptive message.
     *
     * @param message A description of or information about the exception.
     *            Should not be <code>null</code>.
     */
    public BuildTimeoutException(String message) {
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
    public BuildTimeoutException(String message, Throwable cause) {
        super(message, cause);
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
    public BuildTimeoutException(String msg, Throwable cause, Location location) {
        super(msg, cause, location);
    }

    /**
     * Constructs an exception with the given exception as a root cause.
     *
     * @param cause The exception that might have caused this one.
     *              Should not be <code>null</code>.
     */
    public BuildTimeoutException(Throwable cause) {
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
    public BuildTimeoutException(String message, Location location) {
        super(message, location);
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
    public BuildTimeoutException(Throwable cause, Location location) {
        super(cause, location);
    }
}
