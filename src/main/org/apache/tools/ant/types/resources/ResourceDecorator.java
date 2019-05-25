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
package org.apache.tools.ant.types.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * Abstract class that delegates all reading methods of Resource to
 * its wrapped resource and deals with reference handling.
 *
 * <p>Overwrites all setters to throw exceptions.</p>
 *
 * @since Ant 1.8.0
 */
public abstract class ResourceDecorator extends Resource {

    private Resource resource;

    /** no arg constructor */
    protected ResourceDecorator() {
    }

    /**
     * Constructor with another resource to wrap.
     * @param other the resource to wrap.
     */
    protected ResourceDecorator(ResourceCollection other) {
        addConfigured(other);
    }

    /**
     * Sets the resource to wrap using a single-element collection.
     * @param a the resource to wrap as a single element Resource collection.
     */
    public final void addConfigured(ResourceCollection a) {
        checkChildrenAllowed();
        if (resource != null) {
            throw new BuildException(
                "you must not specify more than one resource");
        }
        if (a.size() != 1) {
            throw new BuildException(
                "only single argument resource collections are supported");
        }
        setChecked(false);
        resource = a.iterator().next();
    }

    /**
     * Get the name of the resource.
     * @return the name of the wrapped resource.
     */
    @Override
    public String getName() {
        return getResource().getName();
    }

    /**
     * The exists attribute tells whether a file exists.
     * @return true if this resource exists.
     */
    @Override
    public boolean isExists() {
        return getResource().isExists();
    }

    /**
     * Tells the modification time in milliseconds since 01.01.1970 .
     *
     * @return 0 if the resource does not exist to mirror the behavior
     * of {@link java.io.File File}.
     */
    @Override
    public long getLastModified() {
        return getResource().getLastModified();
    }

    /**
     * Tells if the resource is a directory.
     * @return boolean flag indicating if the resource is a directory.
     */
    @Override
    public boolean isDirectory() {
        return getResource().isDirectory();
    }

    /**
     * Get the size of this Resource.
     * @return the size, as a long, 0 if the Resource does not exist (for
     *         compatibility with java.io.File), or UNKNOWN_SIZE if not known.
     */
    @Override
    public long getSize() {
        return getResource().getSize();
    }

    /**
     * Get an InputStream for the Resource.
     * @return an InputStream containing this Resource's content.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if InputStreams are not
     *         supported for this Resource type.
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return getResource().getInputStream();
    }

    /**
     * Get an OutputStream for the Resource.
     * @return an OutputStream to which content can be written.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if OutputStreams are not
     *         supported for this Resource type.
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        return getResource().getOutputStream();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return whether this Resource is a FileProvider.
     */
    @Override
    public boolean isFilesystemOnly() {
        return as(FileProvider.class) != null;
    }

    /**
     * Overrides the base version.
     * @param r the Reference to set.
     */
    @Override
    public void setRefid(Reference r) {
        if (resource != null) {
            throw noChildrenAllowed();
        }
        super.setRefid(r);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T as(Class<T> clazz) {
        return getResource().as(clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Resource other) {
        if (other == this) {
            return 0;
        }
        if (other instanceof ResourceDecorator) {
            return getResource().compareTo(
                ((ResourceDecorator) other).getResource());
        }
        return getResource().compareTo(other);
    }

    /**
     * Get the hash code for this Resource.
     * @return hash code as int.
     */
    @Override
    public int hashCode() { // NOSONAR
        // super.equals + compareTo are consistent with this implementation
        return (getClass().hashCode() << 4) | getResource().hashCode();
    }

    /**
     * De-references refids if any, ensures a wrapped resource has
     * been specified.
     * @return Resource
     */
    protected final Resource getResource() {
        if (isReference()) {
            return getCheckedRef(Resource.class);
        }
        if (resource == null) {
            throw new BuildException("no resource specified");
        }
        dieOnCircularReference();
        return resource;
    }

    /**
     * {@inheritDoc}
     */
    protected void dieOnCircularReference(final Stack<Object> stack,
                                          final Project project)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stack, project);
        } else {
            pushAndInvokeCircularReferenceCheck(resource, stack, project);
            setChecked(true);
        }
    }

    // disable modification

    /**
     * Overridden, not allowed to set the name of the resource.
     * @param name not used.
     * @throws BuildException always.
     */
    @Override
    public void setName(String name) throws BuildException {
        throw new BuildException("you can't change the name of a "
                                 + getDataTypeName());
    }

    /**
     * Set the exists attribute.
     * @param exists if true, this resource exists.
     */
    @Override
    public void setExists(boolean exists) {
        throw new BuildException("you can't change the exists state of a "
                                 + getDataTypeName());
    }

    /**
     * Override setLastModified.
     * @param lastmodified not used.
     * @throws BuildException always.
     */
    @Override
    public void setLastModified(long lastmodified) throws BuildException {
        throw new BuildException("you can't change the timestamp of a "
                                 + getDataTypeName());
    }

    /**
     * Override setDirectory.
     * @param directory not used.
     * @throws BuildException always.
     */
    @Override
    public void setDirectory(boolean directory) throws BuildException {
        throw new BuildException("you can't change the directory state of a "
                                 + getDataTypeName());
    }

    /**
     * Override setSize.
     * @param size not used.
     * @throws BuildException always.
     */
    @Override
    public void setSize(long size) throws BuildException {
        throw new BuildException("you can't change the size of a "
                                 + getDataTypeName());
    }

}
