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
package org.apache.tools.ant.types.resources;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.util.FileUtils;

/**
 * A compressed resource.
 *
 * <p>Wraps around another resource, delegates all queries (except
 * getSize) to that other resource but uncompresses/compresses streams
 * on the fly.</p>
 *
 * @since Ant 1.7
 */
public abstract class CompressedResource extends Resource {

    private static final int BUFFER_SIZE = 8192;

    private Resource resource;

    /** no arg constructor */
    public CompressedResource() {
    }

    /**
     * Constructor with another resource to wrap.
     * @param other the resource to wrap.
     */
    public CompressedResource(ResourceCollection other) {
        addConfigured(other);
    }

    /**
     * Sets the resource to wrap using a single-element collection.
     * @param a the resource to wrap as a single element Resource collection.
     */
    public void addConfigured(ResourceCollection a) {
        checkChildrenAllowed();
        if (resource != null) {
            throw new BuildException("you must not specify more than one"
                                     + " resource");
        }
        if (a.size() != 1) {
            throw new BuildException("only single argument resource collections"
                                     + " are supported");
        }
        resource = (Resource) a.iterator().next();
    }

    /**
     * Get the name of the resource.
     * @return the name of the wrapped resource.
     */
    public String getName() {
        return getResource().getName();
    }


    /**
     * Overridden, not allowed to set the name of the resource.
     * @param name not used.
     * @throws BuildException always.
     */
    public void setName(String name) throws BuildException {
        throw new BuildException("you can't change the name of a compressed"
                                 + " resource");
    }

    /**
     * The exists attribute tells whether a file exists.
     * @return true if this resource exists.
     */
    public boolean isExists() {
        return getResource().isExists();
    }

    /**
     * Set the exists attribute.
     * @param exists if true, this resource exists.
     */
    public void setExists(boolean exists) {
        throw new BuildException("you can't change the exists state of a "
                                 + " compressed resource");
    }

    /**
     * Tells the modification time in milliseconds since 01.01.1970 .
     *
     * @return 0 if the resource does not exist to mirror the behavior
     * of {@link java.io.File File}.
     */
    public long getLastModified() {
        return getResource().getLastModified();
    }

    /**
     * Override setLastModified.
     * @param lastmodified not used.
     * @throws BuildException always.
     */
    public void setLastModified(long lastmodified) throws BuildException {
        throw new BuildException("you can't change the timestamp of a "
                                 + " compressed resource");
    }

    /**
     * Tells if the resource is a directory.
     * @return boolean flag indicating if the resource is a directory.
     */
    public boolean isDirectory() {
        return getResource().isDirectory();
    }

    /**
     * Override setDirectory.
     * @param directory not used.
     * @throws BuildException always.
     */
    public void setDirectory(boolean directory) throws BuildException {
        throw new BuildException("you can't change the directory state of a "
                                 + " compressed resource");
    }

    /**
     * Get the size of this Resource.
     * @return the size, as a long, 0 if the Resource does not exist (for
     *         compatibility with java.io.File), or UNKNOWN_SIZE if not known.
     */
    public long getSize() {
        if (isExists()) {
            InputStream in = null;
            try {
                in = getInputStream();
                byte[] buf = new byte[BUFFER_SIZE];
                int size = 0;
                int readNow;
                while ((readNow = in.read(buf, 0, buf.length)) > 0) {
                    size += readNow;
                }
                return size;
            } catch (IOException ex) {
                throw new BuildException("caught exception while reading "
                                         + getName(), ex);
            } finally {
                FileUtils.close(in);
            }
        } else {
            return 0;
        }
    }

    /**
     * Override setSize.
     * @param size not used.
     * @throws BuildException always.
     */
    public void setSize(long size) throws BuildException {
        throw new BuildException("you can't change the size of a "
                                 + " compressed resource");
    }

    /**
     * Delegates to a comparison of names.
     * @param other the object to compare to.
     * @return a negative integer, zero, or a positive integer as this Resource
     *         is less than, equal to, or greater than the specified Resource.
     */
    public int compareTo(Object other) {
        if (other == this) {
            return 0;
        }
        if (other instanceof CompressedResource) {
            return getResource().compareTo(
                ((CompressedResource) other).getResource());
        }
        return getResource().compareTo(other);
    }

    /**
     * Get the hash code for this Resource.
     * @return hash code as int.
     */
    public int hashCode() {
        return getResource().hashCode();
    }

    /**
     * Get an InputStream for the Resource.
     * @return an InputStream containing this Resource's content.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if InputStreams are not
     *         supported for this Resource type.
     */
    public InputStream getInputStream() throws IOException {
        InputStream in = getResource().getInputStream();
        if (in != null) {
            in = wrapStream(in);
        }
        return in;
    }

    /**
     * Get an OutputStream for the Resource.
     * @return an OutputStream to which content can be written.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if OutputStreams are not
     *         supported for this Resource type.
     */
    public OutputStream getOutputStream() throws IOException {
        OutputStream out = getResource().getOutputStream();
        if (out != null) {
            out = wrapStream(out);
        }
        return out;
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return whether this Resource is a FileResource.
     */
    public boolean isFilesystemOnly() {
        return false;
    }

    /**
     * Get the string representation of this Resource.
     * @return this Resource formatted as a String.
     * @since Ant 1.7
     */
    public String toString() {
        return getCompressionName() + " compressed "
            + getResource().toString();
    }

    /**
     * Overrides the base version.
     * @param r the Reference to set.
     */
    public void setRefid(Reference r) {
        if (resource != null) {
            throw noChildrenAllowed();
        }
        super.setRefid(r);
    }

    /**
     * Is supposed to wrap the stream to allow decompression on the fly.
     *
     * @param in InputStream to wrap, will never be null.
     * @return a compressed inputstream.
     * @throws IOException if there is a problem.
     */
    protected abstract InputStream wrapStream(InputStream in)
        throws IOException;

    /**
     * Is supposed to wrap the stream to allow compression on the fly.
     *
     * @param out OutputStream to wrap, will never be null.
     * @return a compressed outputstream.
     * @throws IOException if there is a problem.
     */
    protected abstract OutputStream wrapStream(OutputStream out)
        throws IOException;

    /**
     * @return the name of the compression method.
     */
    protected abstract String getCompressionName();

    private Resource getResource() {
        if (isReference()) {
            return (Resource) getCheckedRef();
        } else if (resource == null) {
            throw new BuildException("no resource specified");
        }
        return resource;
    }

}
