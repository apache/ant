/*
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

/**
 * BuildException + exit status.
 *
 * @since Ant 1.7
 */
public class ExitStatusException extends BuildException {

    /** Status code */
    private int status;

    /**
     * Constructs an <CODE>ExitStatusException</CODE>.
     * @param status the associated status code
     */
    public ExitStatusException(int status) {
        super();
        this.status = status;
    }

    /**
     * Constructs an <CODE>ExitStatusException</CODE>.
     * @param msg the associated message
     * @param status the associated status code
     */
    public ExitStatusException(String msg, int status) {
        super(msg);
        this.status = status;
    }

    /**
     * Get the status code.
     * @return <CODE>int</CODE>
     */
    public int getStatus() {
        return status;
    }
}
