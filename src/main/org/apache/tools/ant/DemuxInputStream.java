/*
 * Copyright  2003-2004 The Apache Software Foundation
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

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * Passes input requests tot he project object for demuxing into
 * individual tasks and threads.
 *
 * @since Ant 1.6
 */
public class DemuxInputStream extends InputStream {

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

    public int read() throws IOException {
        byte[] buffer = new byte[1];
        if (project.demuxInput(buffer, 0, 1) == -1) {
            return -1;
        }
        return buffer[0];
    }


    public int read(byte[] buffer, int offset, int length) throws IOException {
        return project.demuxInput(buffer, offset, length);
    }

}
