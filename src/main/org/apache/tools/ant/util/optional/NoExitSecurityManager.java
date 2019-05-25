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
package org.apache.tools.ant.util.optional;

import java.security.Permission;

import org.apache.tools.ant.ExitException;

/**
 * This is intended as a replacement for the default system manager.
 * The goal is to intercept System.exit calls and make it throw an
 * exception instead so that a System.exit in a task does not
 * fully terminate Ant.
 *
 * @see ExitException
 */
public class NoExitSecurityManager extends SecurityManager {

    /**
     * Override SecurityManager#checkExit.
     * This throws an ExitException(status) exception.
     * @param status the exit status
     */
    @Override
    public void checkExit(int status) {
        throw new ExitException(status);
    }

    /**
     * Override SecurityManager#checkPermission.
     * This does nothing.
     * @param perm the requested permission.
     */
    @Override
    public void checkPermission(Permission perm) {
        // no permission here
    }
}
