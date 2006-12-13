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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *
 *
 * @since Ant 1.1
 *
 * @deprecated The deltree task is deprecated since Ant 1.2.  Use
 * delete instead.
 */

public class Deltree extends Task {

    private File dir;

    /**
     * Set the directory to be deleted
     *
     * @param dir the root of the tree to be removed.
     */
    public void setDir(File dir) {
        this.dir = dir;
    }

    /**
     * Do the work.
     *
     * @exception BuildException if the task is not configured correctly or
     * the tree cannot be removed.
     */
    public void execute() throws BuildException {
        log("DEPRECATED - The deltree task is deprecated.  "
            + "Use delete instead.");

        if (dir == null) {
            throw new BuildException("dir attribute must be set!", getLocation());
        }

        if (dir.exists()) {
            if (!dir.isDirectory()) {
                if (!dir.delete()) {
                    throw new BuildException("Unable to delete directory "
                                             + dir.getAbsolutePath(),
                                             getLocation());
                }
                return;
            }

            log("Deleting: " + dir.getAbsolutePath());

            try {
                removeDir(dir);
            } catch (IOException ioe) {
                String msg = "Unable to delete " + dir.getAbsolutePath();
                throw new BuildException(msg, getLocation());
            }
        }
    }

    private void removeDir(File dir) throws IOException {

        // check to make sure that the given dir isn't a symlink
        // the comparison of absolute path and canonical path
        // catches this

        //        if (dir.getCanonicalPath().equals(dir.getAbsolutePath())) {
        // (costin) It will not work if /home/costin is symlink to
        // /da0/home/costin ( taz for example )
        String[] list = dir.list();
        for (int i = 0; i < list.length; i++) {
            String s = list[i];
            File f = new File(dir, s);
            if (f.isDirectory()) {
                removeDir(f);
            } else {
                if (!f.delete()) {
                    throw new BuildException("Unable to delete file "
                                             + f.getAbsolutePath());
                }
            }
        }
        if (!dir.delete()) {
            throw new BuildException("Unable to delete directory "
                                     + dir.getAbsolutePath());
        }
    }
}

