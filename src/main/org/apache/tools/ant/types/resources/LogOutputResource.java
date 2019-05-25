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
package org.apache.tools.ant.types.resources;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.Resource;

/**
 * Output-only Resource that always appends to Ant's log.
 * @since Ant 1.8
 */
public class LogOutputResource extends Resource implements Appendable {
    private static final String NAME = "[Ant log]";

    private LogOutputStream outputStream;

    /**
     * Create a new LogOutputResource.
     * @param managingComponent ditto
     */
    public LogOutputResource(ProjectComponent managingComponent) {
        super(NAME);
        outputStream = new LogOutputStream(managingComponent);
    }

    /**
     * Create a new LogOutputResource.
     * @param managingComponent owning log content
     * @param level log level
     */
    public LogOutputResource(ProjectComponent managingComponent, int level) {
        super(NAME);
        outputStream = new LogOutputStream(managingComponent, level);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream getAppendOutputStream() throws IOException {
        return outputStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        return outputStream;
    }
}
