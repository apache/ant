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
package org.apache.tools.ant.util;

import java.io.IOException;


/**
 * Simple interface for executing a piece of code. Used for writing anonymous inner
 * classes in FTP task for retry-on-IOException behaviour.
 *
 * @see RetryHandler
 */
public interface Retryable {
    /** The value to use to never give up. */
    int RETRY_FOREVER = -1;
    /**
     * Called to execute the code.
     * @throws IOException if there is a problem.
     */
    void execute() throws IOException;

}
