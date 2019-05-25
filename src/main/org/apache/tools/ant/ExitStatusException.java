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
 * BuildException + exit status.
 *
 * @since Ant 1.7
 */
public class ExitStatusException extends BuildException {

    private static final long serialVersionUID = 7760846806886585968L;

    /** Status code */
    private int status;

    /**
     * Constructs an <code>ExitStatusException</code>.
     * @param status the associated status code
     */
    public ExitStatusException(int status) {
        super();
        this.status = status;
    }

    /**
     * Constructs an <code>ExitStatusException</code>.
     * @param msg the associated message
     * @param status the associated status code
     */
    public ExitStatusException(String msg, int status) {
        super(msg);
        this.status = status;
    }

    /**
     * Construct an exit status exception with location information too
     * @param message error message
     * @param status exit status
     * @param location exit location
     */
    public ExitStatusException(String message, int status, Location location) {
        super(message, location);
        this.status = status;
    }

    /**
     * Get the status code.
     * @return <code>int</code>
     */
    public int getStatus() {
        return status;
    }
}
