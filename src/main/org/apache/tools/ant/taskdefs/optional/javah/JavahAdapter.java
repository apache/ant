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

package org.apache.tools.ant.taskdefs.optional.javah;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.Javah;

/**
 * Interface for different backend implementations of the Javah task.
 *
 * @since Ant 1.6.3
 */
public interface JavahAdapter {
    /**
     * Performs the actual compilation.
     * @param javah the calling javah task.
     * @return true if the compilation was successful.
     * @throws BuildException if there is an error.
     * @since Ant 1.6.3
     */
    boolean compile(Javah javah) throws BuildException;
}
