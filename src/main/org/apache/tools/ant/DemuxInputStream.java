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

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * Passes input requests to the project object for demultiplexing into
 * individual tasks and threads.
 *
 * @since Ant 1.6
 */
public class DemuxInputStream extends InputStream {

    private static final int MASK_8BIT = 0xFF;
    /**
     * The project to from which to get input.
     */
    private Project project;

    /**
     * Create a DemuxInputStream for the given project
     *
     * @param project the project instance
     */
    public DemuxInputStream(Project project) {
        this.project = project;
    }

    /**
     * Read a byte from the project's demultiplexed input.
     * @return the next byte
     * @throws IOException on error
     */
    @Override
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        if (project.demuxInput(buffer, 0, 1) == -1) {
            return -1;
        }
        return buffer[0] & MASK_8BIT;
    }


    /**
     * Read bytes from the project's demultiplexed input.
     * @param buffer an array of bytes to read into
     * @param offset the offset in the array of bytes
     * @param length the number of bytes in the array
     * @return the number of bytes read
     * @throws IOException on error
     */
    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return project.demuxInput(buffer, offset, length);
    }

}
