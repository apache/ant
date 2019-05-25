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
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;

/**
 * Renames a file.
 *
 * @deprecated The rename task is deprecated since Ant 1.2.  Use move instead.
 * @since Ant 1.1
 */
@Deprecated
public class Rename extends Task {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private File src;
    private File dest;
    private boolean replace = true;


    /**
     * Sets the file to be renamed.
     * @param src the file to rename
     */
    public void setSrc(File src) {
        this.src = src;
    }

    /**
     * Sets the new name of the file.
     * @param dest the new name of the file.
     */
    public void setDest(File dest) {
        this.dest = dest;
    }

    /**
     * Sets whether an existing file should be replaced.
     * @param replace <code>on</code>, if an existing file should be replaced.
     */
    public void setReplace(String replace) {
        this.replace = Project.toBoolean(replace);
    }


    /**
     * Renames the file <code>src</code> to <code>dest</code>
     *
     * @throws BuildException if the rename operation fails
     */
    public void execute() throws BuildException {
        log("DEPRECATED - The rename task is deprecated.  Use move instead.");

        if (dest == null) {
            throw new BuildException("dest attribute is required", getLocation());
        }

        if (src == null) {
            throw new BuildException("src attribute is required", getLocation());
        }

        if (!replace && dest.exists()) {
            throw new BuildException(dest + " already exists.");
        }

        try {
            FILE_UTILS.rename(src, dest);
        } catch (IOException e) {
            throw new BuildException("Unable to rename " + src + " to "
                + dest, e, getLocation());
        }
    }
}
