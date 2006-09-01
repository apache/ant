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

package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;

/** Interface for p4 job output stream handler. Classes implementing this interface
 * can be called back by P4Base.execP4Command();
 *
 */
public interface P4Handler extends ExecuteStreamHandler {

    /**
     * processing of one line of stdout or of stderr
     * @param line a line of stdout or stderr that the implementation will process
     * @throws BuildException at the discretion of the implementation.
     */
    void process(String line) throws BuildException;

    /**
     * set any data to be written to P4's stdin
     * @param line the text to write to P4's stdin
     * @throws BuildException if the line cannot be processed.
     */
    void setOutput(String line) throws BuildException;
}
