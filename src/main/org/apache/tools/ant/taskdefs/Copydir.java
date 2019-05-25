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
import java.util.Hashtable;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;

/**
 * Copies a directory.
 *
 * @since Ant 1.1
 *
 * @deprecated The copydir task is deprecated since Ant 1.2.  Use copy instead.
 */

@Deprecated
public class Copydir extends MatchingTask {

    private File srcDir;
    private File destDir;
    private boolean filtering = false;
    private boolean flatten = false;
    private boolean forceOverwrite = false;
    private Map<String, String> filecopyList = new Hashtable<>();

    /**
     * The src attribute
     *
     * @param src the source file
     */
    public void setSrc(File src) {
        srcDir = src;
    }

    /**
     * The dest attribute
     *
     * @param dest the destination file
     */
    public void setDest(File dest) {
        destDir = dest;
    }

    /**
     * The filtering attribute.
     * Default  is false.
     * @param filter if true use filtering
     */
    public void setFiltering(boolean filter) {
        filtering = filter;
    }

    /**
     * The flattening attribute.
     * Default  is false.
     * @param flatten if true use flattening
     */
    public void setFlatten(boolean flatten) {
        this.flatten = flatten;
    }

    /**
     * The forceoverwrite attribute.
     * Default  is false.
     * @param force if true overwrite even if the destination file
     *              is newer that the source file
     */
    public void setForceoverwrite(boolean force) {
        forceOverwrite = force;
    }

    /**
     * Execute the task.
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        log("DEPRECATED - The copydir task is deprecated.  Use copy instead.");

        if (srcDir == null) {
            throw new BuildException("src attribute must be set!",
                                     getLocation());
        }

        if (!srcDir.exists()) {
            throw new BuildException("srcdir " + srcDir.toString()
                                     + " does not exist!", getLocation());
        }

        if (destDir == null) {
            throw new BuildException("The dest attribute must be set.",
                                     getLocation());
        }

        if (srcDir.equals(destDir)) {
            log("Warning: src == dest", Project.MSG_WARN);
        }

        DirectoryScanner ds = super.getDirectoryScanner(srcDir);

        try {
            scanDir(srcDir, destDir, ds.getIncludedFiles());
            if (filecopyList.size() > 0) {
                log("Copying " + filecopyList.size() + " file"
                    + (filecopyList.size() == 1 ? "" : "s")
                    + " to " + destDir.getAbsolutePath());
                for (Map.Entry<String, String> e : filecopyList.entrySet()) {
                    String fromFile = e.getKey();
                    String toFile = e.getValue();
                    try {
                        getProject().copyFile(fromFile, toFile, filtering,
                                         forceOverwrite);
                    } catch (IOException ioe) {
                        String msg = "Failed to copy " + fromFile + " to "
                            + toFile + " due to " + ioe.getMessage();
                        throw new BuildException(msg, ioe, getLocation());
                    }
                }
            }
        } finally {
            filecopyList.clear();
        }
    }

    private void scanDir(File from, File to, String[] files) {
        for (String filename : files) {
            File srcFile = new File(from, filename);
            File destFile;
            if (flatten) {
                destFile = new File(to, new File(filename).getName());
            } else {
                destFile = new File(to, filename);
            }
            if (forceOverwrite
                || (srcFile.lastModified() > destFile.lastModified())) {
                filecopyList.put(srcFile.getAbsolutePath(),
                                 destFile.getAbsolutePath());
            }
        }
    }
}
