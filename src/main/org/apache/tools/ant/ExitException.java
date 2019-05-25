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
 * Used to report exit status of classes which call System.exit().
 *
 * @see org.apache.tools.ant.util.optional.NoExitSecurityManager
 * @see org.apache.tools.ant.types.Permissions
 *
 */
public class ExitException extends SecurityException {

    private static final long serialVersionUID = 2772487854280543363L;

    /** Status code */
    private int status;

    /**
     * Constructs an exit exception.
     * @param status the status code returned via System.exit()
     */
    public ExitException(int status) {
        super("ExitException: status " + status);
        this.status = status;
    }

    /**
     * Constructs an exit exception.
     * @param msg the message to be displayed.
     * @param status the status code returned via System.exit()
     */
    public ExitException(String msg, int status) {
        super(msg);
        this.status = status;
    }

    /**
     * The status code returned by System.exit()
     *
     * @return the status code returned by System.exit()
     */
    public int getStatus() {
        return status;
    }
}
