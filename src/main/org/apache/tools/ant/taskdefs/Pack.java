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
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;

/**
 * Abstract Base class for pack tasks.
 *
 * @since Ant 1.5
 */

public abstract class Pack extends Task {
    private static final int BUFFER_SIZE = 8 * 1024;

    // CheckStyle:VisibilityModifier OFF - bc
    protected File zipFile;
    protected File source;
    // CheckStyle:VisibilityModifier ON
    private Resource src;

    /**
     * the required destination file.
     * @param zipFile the destination file
     */
    public void setZipfile(File zipFile) {
        this.zipFile = zipFile;
    }

    /**
     * the required destination file.
     * @param zipFile the destination file
     */
    public void setDestfile(File zipFile) {
        setZipfile(zipFile);
    }

    /**
     * the file to compress; required.
     * @param src the source file
     */
    public void setSrc(File src) {
        setSrcResource(new FileResource(src));
    }

    /**
     * The resource to pack; required.
     * @param src resource to expand
     */
    public void setSrcResource(Resource src) {
        if (src.isDirectory()) {
            throw new BuildException("the source can't be a directory");
        }
        FileProvider fp = src.as(FileProvider.class);
        if (fp != null) {
            source = fp.getFile();
        } else if (!supportsNonFileResources()) {
            throw new BuildException("Only FileSystem resources are supported.");
        }
        this.src = src;
    }

    /**
     * Set the source resource.
     * @param a the resource to pack as a single element Resource collection.
     */
    public void addConfigured(ResourceCollection a) {
        if (a.size() == 0) {
            throw new BuildException(
                "No resource selected, %s needs exactly one resource.",
                getTaskName());
        }
        if (a.size() != 1) {
            throw new BuildException(
                "%s cannot handle multiple resources at once. (%d resources were selected.)",
                getTaskName(), a.size());
        }
        setSrcResource(a.iterator().next());
    }

    /**
     * validation routine
     * @throws BuildException if anything is invalid
     */
    private void validate() throws BuildException {
        if (zipFile == null) {
            throw new BuildException("zipfile attribute is required", getLocation());
        }

        if (zipFile.isDirectory()) {
            throw new BuildException(
                "zipfile attribute must not represent a directory!",
                getLocation());
        }

        if (getSrcResource() == null) {
            throw new BuildException(
                "src attribute or nested resource is required", getLocation());
        }
    }

    /**
     * validate, then hand off to the subclass
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        validate();

        Resource s = getSrcResource();
        if (!s.isExists()) {
            log("Nothing to do: " + s.toString()
                + " doesn't exist.");
        } else if (zipFile.lastModified() < s.getLastModified()) {
            log("Building: " + zipFile.getAbsolutePath());
            pack();
        } else {
            log("Nothing to do: " + zipFile.getAbsolutePath()
                + " is up to date.");
        }
    }

    /**
     * zip a stream to an output stream
     * @param in   the stream to zip
     * @param zOut the output stream
     * @throws IOException if something goes wrong
     */
    private void zipFile(InputStream in, OutputStream zOut)
        throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int count = 0;
        do {
            zOut.write(buffer, 0, count);
            count = in.read(buffer, 0, buffer.length);
        } while (count != -1);
    }

    /**
     * zip a file to an output stream
     * @param file the file to zip
     * @param zOut the output stream
     * @throws IOException on error
     */
    protected void zipFile(File file, OutputStream zOut)
        throws IOException {
        zipResource(new FileResource(file), zOut);
    }

    /**
     * zip a resource to an output stream
     * @param resource the resource to zip
     * @param zOut the output stream
     * @throws IOException on error
     */
    protected void zipResource(Resource resource, OutputStream zOut)
        throws IOException {
        try (InputStream rIn = resource.getInputStream()) {
            zipFile(rIn, zOut);
        }
    }

    /**
     * subclasses must implement this method to do their compression
     */
    protected abstract void pack();

    /**
     * The source resource.
     * @return the source.
     * @since Ant 1.7
     */
    public Resource getSrcResource() {
        return src;
    }

    /**
     * Whether this task can deal with non-file resources.
     *
     * <p>This implementation returns false.</p>
     * @return false.
     * @since Ant 1.7
     */
    protected boolean supportsNonFileResources() {
        return false;
    }
}
