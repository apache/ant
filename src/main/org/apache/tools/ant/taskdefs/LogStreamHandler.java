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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;

/**
 * Logs standard output and error of a subprocess to the log system of ant.
 *
 * @since Ant 1.2
 */
public class LogStreamHandler extends PumpStreamHandler {

    /**
     * Creates log stream handler
     *
     * @param task the task for whom to log
     * @param outlevel the loglevel used to log standard output
     * @param errlevel the loglevel used to log standard error
     */
    public LogStreamHandler(Task task, int outlevel, int errlevel) {
        this((ProjectComponent) task, outlevel, errlevel);
    }

    /**
     * Creates log stream handler
     *
     * @param pc the project component for whom to log
     * @param outlevel the loglevel used to log standard output
     * @param errlevel the loglevel used to log standard error
     */
    public LogStreamHandler(ProjectComponent pc, int outlevel, int errlevel) {
        super(new LogOutputStream(pc, outlevel),
              new LogOutputStream(pc, errlevel));
    }

    /**
     * Stop the log stream handler.
     */
    @Override
    public void stop() {
        super.stop();
        FileUtils.close(getErr());
        FileUtils.close(getOut());
    }
}
