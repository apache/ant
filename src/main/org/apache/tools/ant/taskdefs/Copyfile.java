/* 
 * Copyright  2000,2002,2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Copies a file.
 *
 * @author duncan@x180.com
 *
 * @since Ant 1.1
 *
 * @deprecated The copyfile task is deprecated since Ant 1.2.  Use
 * copy instead.
 */

public class Copyfile extends Task {

    private File srcFile;
    private File destFile;
    private boolean filtering = false;
    private boolean forceOverwrite = false;

    public void setSrc(File src) {
        srcFile = src;
    }

    public void setForceoverwrite(boolean force) {
        forceOverwrite = force;
    }

    public void setDest(File dest) {
        destFile = dest;
    }

    public void setFiltering(String filter) {
        filtering = Project.toBoolean(filter);
    }

    public void execute() throws BuildException {
        log("DEPRECATED - The copyfile task is deprecated.  Use copy instead.");

        if (srcFile == null) {
            throw new BuildException("The src attribute must be present.",
                                     getLocation());
        }

        if (!srcFile.exists()) {
            throw new BuildException("src " + srcFile.toString()
                                     + " does not exist.", getLocation());
        }

        if (destFile == null) {
            throw new BuildException("The dest attribute must be present.",
                                     getLocation());
        }

        if (srcFile.equals(destFile)) {
            log("Warning: src == dest", Project.MSG_WARN);
        }

        if (forceOverwrite
            || srcFile.lastModified() > destFile.lastModified()) {
            try {
                getProject().copyFile(srcFile, destFile, filtering, forceOverwrite);
            } catch (IOException ioe) {
                String msg = "Error copying file: " + srcFile.getAbsolutePath()
                    + " due to " + ioe.getMessage();
                throw new BuildException(msg);
            }
        }
    }
}
