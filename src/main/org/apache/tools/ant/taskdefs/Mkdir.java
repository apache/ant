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

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Creates a given directory.
 * Creates a directory and any non-existent parent directories, when
 * necessary
 *
 * @since Ant 1.1
 *
 * @ant.task category="filesystem"
 */

public class Mkdir extends Task {

    private static final int MKDIR_RETRY_SLEEP_MILLIS = 10;
    /**
     * our little directory
     */
    private File dir;

    /**
     * create the directory and all parents
     * @throws BuildException if dir is somehow invalid, or creation failed.
     */
    @Override
    public void execute() throws BuildException {
        if (dir == null) {
            throw new BuildException("dir attribute is required", getLocation());
        }

        if (dir.isFile()) {
            throw new BuildException(
                "Unable to create directory as a file already exists with that name: %s",
                dir.getAbsolutePath());
        }

        if (!dir.exists()) {
            boolean result = mkdirs(dir);
            if (!result) {
                if (dir.exists()) {
                    log("A different process or task has already created dir "
                        + dir.getAbsolutePath(), Project.MSG_VERBOSE);
                    return;
                }
                throw new BuildException(
                    "Directory " + dir.getAbsolutePath()
                        + " creation was not successful for an unknown reason",
                    getLocation());
            }
            log("Created dir: " + dir.getAbsolutePath());
        } else {
            log("Skipping " + dir.getAbsolutePath()
                + " because it already exists.", Project.MSG_VERBOSE);
        }
    }

    /**
     * the directory to create; required.
     *
     * @param dir the directory to be made.
     */
    public void setDir(File dir) {
        this.dir = dir;
    }

    /**
     * Get the directory to create.
     * @return File
     */
    public File getDir() {
        return dir;
    }

    /**
     * Attempt to fix possible race condition when creating
     * directories on WinXP. If the mkdirs does not work,
     * wait a little and try again.
     */
    private boolean mkdirs(File f) {
        if (!f.mkdirs()) {
            try {
                Thread.sleep(MKDIR_RETRY_SLEEP_MILLIS);
                return f.mkdirs();
            } catch (InterruptedException ex) {
                return f.mkdirs();
            }
        }
        return true;
    }
}
