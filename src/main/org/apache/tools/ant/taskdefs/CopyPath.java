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
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;

/**
 * Copy the contents of a path to a destination, using the mapper of choice
 *
 * @since Ant 1.7.0
 *
 * @ant.task category="filesystem"
 * @deprecated this task should have never been released and was
 * obsoleted by ResourceCollection support in Copy available since Ant
 * 1.7.0.  Don't use it.
 */
@Deprecated
public class CopyPath extends Task {

    // Error messages
    /** No destdir attribute */
    public static final String ERROR_NO_DESTDIR = "No destDir specified";

    /** No path  */
    public static final String ERROR_NO_PATH = "No path specified";

    /** No mapper  */
    public static final String ERROR_NO_MAPPER = "No mapper specified";

    // fileutils
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    // --- Fields --
    private FileNameMapper mapper;

    private Path path;

    private File destDir;

    // TODO not read, yet in a public setter
    @SuppressWarnings("unused")
    private long granularity = FILE_UTILS.getFileTimestampGranularity();

    private boolean preserveLastModified = false;

    /**
     * The dest dir attribute.
     * @param destDir the value of the destdir attribute.
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * add a mapper
     *
     * @param newmapper the mapper to add.
     */
    public void add(FileNameMapper newmapper) {
        if (mapper != null) {
            throw new BuildException("Only one mapper allowed");
        }
        mapper = newmapper;
    }

    /**
     * Set the path to be used when running the Java class.
     *
     * @param s
     *            an Ant Path object containing the path.
     */
    public void setPath(Path s) {
        createPath().append(s);
    }

    /**
     * Set the path to use by reference.
     *
     * @param r
     *            a reference to an existing path.
     */
    public void setPathRef(Reference r) {
        createPath().setRefid(r);
    }

    /**
     * Create a path.
     *
     * @return a path to be configured.
     */
    public Path createPath() {
        if (path == null) {
            path = new Path(getProject());
        }
        return path;
    }

    /**
     * Set the number of milliseconds leeway to give before deciding a
     * target is out of date.
     * TODO: This is not yet used.
     * @param granularity the granularity used to decide if a target is out of
     *                    date.
     */
    public void setGranularity(long granularity) {
        this.granularity = granularity;
    }

    /**
     * Give the copied files the same last modified time as the original files.
     * @param preserveLastModified if true preserve the modified time;
     *                             default is false.
     */
    public void setPreserveLastModified(boolean preserveLastModified) {
        this.preserveLastModified = preserveLastModified;
    }

    /**
     * Ensure we have a consistent and legal set of attributes, and set any
     * internal flags necessary based on different combinations of attributes.
     *
     * @throws BuildException
     *             if an error occurs.
     */
    protected void validateAttributes() throws BuildException {
        if (destDir == null) {
            throw new BuildException(ERROR_NO_DESTDIR);
        }
        if (mapper == null) {
            throw new BuildException(ERROR_NO_MAPPER);
        }
        if (path == null) {
            throw new BuildException(ERROR_NO_PATH);
        }
    }

    /**
     * This is a very minimal derivative of the normal copy logic.
     *
     * @throws BuildException
     *             if something goes wrong with the build.
     */
    public void execute() throws BuildException {
        log("This task should have never been released and was"
            + " obsoleted by ResourceCollection support in <copy> available"
            + " since Ant 1.7.0.  Don't use it.",
            Project.MSG_ERR);

        validateAttributes();
        String[] sourceFiles = path.list();
        if (sourceFiles.length == 0) {
            log("Path is empty", Project.MSG_VERBOSE);
            return;
        }

        for (String sourceFileName : sourceFiles) {

            File sourceFile = new File(sourceFileName);
            String[] toFiles = mapper.mapFileName(sourceFileName);
            if (toFiles == null) {
                continue;
            }

            for (String destFileName : toFiles) {
                File destFile = new File(destDir, destFileName);

                if (sourceFile.equals(destFile)) {
                    log("Skipping self-copy of " + sourceFileName, Project.MSG_VERBOSE);
                    continue;
                }
                if (sourceFile.isDirectory()) {
                    log("Skipping directory " + sourceFileName);
                    continue;
                }
                try {
                    log("Copying " + sourceFile + " to " + destFile, Project.MSG_VERBOSE);

                    FILE_UTILS.copyFile(sourceFile, destFile, null, null, false,
                            preserveLastModified, null, null, getProject());
                } catch (IOException ioe) {
                    String msg = "Failed to copy " + sourceFile + " to " + destFile + " due to "
                            + ioe.getMessage();
                    if (destFile.exists() && !destFile.delete()) {
                        msg += " and I couldn't delete the corrupt " + destFile;
                    }
                    throw new BuildException(msg, ioe, getLocation());
                }
            }
        }
    }
}
