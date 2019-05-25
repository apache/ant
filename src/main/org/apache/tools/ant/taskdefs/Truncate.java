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
import java.io.RandomAccessFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;

/**
 * Set the length of one or more files, as the intermittently available
 * <code>truncate</code> Unix utility/function.
 * @since Ant 1.7.1
 */
public class Truncate extends Task {

    private static final int BUFFER_SIZE = 1024;

    private static final Long ZERO = 0L;

    private static final String NO_CHILD = "No files specified.";

    private static final String INVALID_LENGTH = "Cannot truncate to length ";

    private static final String READ_WRITE = "rw";

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private static final byte[] FILL_BUFFER = new byte[BUFFER_SIZE];

    private Path path;
    private boolean create = true;
    private boolean mkdirs = false;

    private Long length;
    private Long adjust;

    /**
     * Set a single target File.
     * @param f the single File
     */
    public void setFile(File f) {
        add(new FileResource(f));
    }

    /**
     * Add a nested (filesystem-only) ResourceCollection.
     * @param rc the ResourceCollection to add.
     */
    public void add(ResourceCollection rc) {
        getPath().add(rc);
    }

    /**
     * Set the amount by which files' lengths should be adjusted.
     * It is permissible to append K / M / G / T / P.
     * @param adjust (positive or negative) adjustment amount.
     */
    public void setAdjust(Long adjust) {
        this.adjust = adjust;
    }

    /**
     * Set the length to which files should be set.
     * It is permissible to append K / M / G / T / P.
     * @param length (positive) adjustment amount.
     */
    public void setLength(Long length) {
        this.length = length;
        if (length != null && length < 0) {
            throw new BuildException(INVALID_LENGTH + length);
        }
    }

    /**
     * Set whether to create nonexistent files.
     * @param create boolean, default <code>true</code>.
     */
    public void setCreate(boolean create) {
        this.create = create;
    }

    /**
     * Set whether, when creating nonexistent files, nonexistent directories
     * should also be created.
     * @param mkdirs boolean, default <code>false</code>.
     */
    public void setMkdirs(boolean mkdirs) {
        this.mkdirs = mkdirs;
    }

    /** {@inheritDoc}. */
    public void execute() {
        if (length != null && adjust != null) {
            throw new BuildException(
                    "length and adjust are mutually exclusive options");
        }
        if (length == null && adjust == null) {
            length = ZERO;
        }
        if (path == null) {
            throw new BuildException(NO_CHILD);
        }
        for (Resource r : path) {
            File f = r.as(FileProvider.class).getFile();
            if (shouldProcess(f)) {
                process(f);
            }
        }
    }

    private boolean shouldProcess(File f) {
        if (f.isFile()) {
            return true;
        }
        if (!create) {
            return false;
        }
        Exception exception = null;
        try {
            if (FILE_UTILS.createNewFile(f, mkdirs)) {
                return true;
            }
        } catch (IOException e) {
            exception = e;
        }
        String msg = "Unable to create " + f;
        if (exception == null) {
            log(msg, Project.MSG_WARN);
            return false;
        }
        throw new BuildException(msg, exception);
    }

    private void process(File f) {
        long len = f.length();
        long newLength = length == null
                ? len + adjust : length;

        if (len == newLength) {
            //nothing to do!
            return;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, READ_WRITE); //NOSONAR
        } catch (Exception e) {
            throw new BuildException("Could not open " + f + " for writing", e);
        }
        try {
            if (newLength > len) {
                long pos = len;
                raf.seek(pos);
                while (pos < newLength) {
                    long writeCount = Math.min(FILL_BUFFER.length,
                            newLength - pos);
                    raf.write(FILL_BUFFER, 0, (int) writeCount);
                    pos += writeCount;
                }
            } else {
                raf.setLength(newLength);
            }
        } catch (IOException e) {
            throw new BuildException("Exception working with " + raf, e);
        } finally {
            try {
                raf.close();
            } catch (IOException e) {
                log("Caught " + e + " closing " + raf, Project.MSG_WARN);
            }
        }
    }

    private synchronized Path getPath() {
        if (path == null) {
            path = new Path(getProject());
        }
        return path;
    }

}
