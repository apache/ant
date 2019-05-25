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

import java.io.File;
import java.util.Stack;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * A Resource representation of an entry inside an archive.
 * @since Ant 1.7
 */
public abstract class ArchiveResource extends Resource {
    private static final int NULL_ARCHIVE
        = Resource.getMagicNumber("null archive".getBytes());

    private Resource archive;
    private boolean haveEntry = false;
    private boolean modeSet = false;
    private int mode = 0;

    /**
     * Default constructor.
     */
    protected ArchiveResource() {
    }

    /**
     * Construct a ArchiveResource representing the specified
     * entry in the specified archive.
     * @param a the archive as File.
     */
    protected ArchiveResource(File a) {
        this(a, false);
    }

    /**
     * Construct a ArchiveResource representing the specified
     * entry in the specified archive.
     * @param a the archive as File.
     * @param withEntry if the entry has been specified.
     */
    protected ArchiveResource(File a, boolean withEntry) {
        setArchive(a);
        haveEntry = withEntry;
    }

    /**
     * Construct a ArchiveResource representing the specified
     * entry in the specified archive.
     * @param a the archive as Resource.
     * @param withEntry if the entry has been specified.
     */
    protected ArchiveResource(Resource a, boolean withEntry) {
        addConfigured(a);
        haveEntry = withEntry;
    }

    /**
     * Set the archive that holds this Resource.
     * @param a the archive as a File.
     */
    public void setArchive(File a) {
        checkAttributesAllowed();
        archive = new FileResource(a);
    }

    /**
     * Sets the file or dir mode for this resource.
     * @param mode integer representation of Unix permission mask.
     */
    public void setMode(int mode) {
        checkAttributesAllowed();
        this.mode = mode;
        modeSet = true;
    }

    /**
     * Sets the archive that holds this as a single element Resource
     * collection.
     * @param a the archive as a single element Resource collection.
     */
    public void addConfigured(ResourceCollection a) {
        checkChildrenAllowed();
        if (archive != null) {
            throw new BuildException(
                "you must not specify more than one archive");
        }
        if (a.size() != 1) {
            throw new BuildException(
                "only single argument resource collections are supported as archives");
        }
        archive = a.iterator().next();
    }

    /**
     * Get the archive that holds this Resource.
     * @return the archive as a Resource.
     */
    public Resource getArchive() {
        return isReference() ? getRef().getArchive() : archive;
    }

    /**
     * Get the last modified date of this Resource.
     * @return the last modification date.
     */
    @Override
    public long getLastModified() {
        if (isReference()) {
            return getRef().getLastModified();
        }
        checkEntry();
        return super.getLastModified();
    }

    /**
     * Get the size of this Resource.
     * @return the long size of this Resource.
     */
    @Override
    public long getSize() {
        if (isReference()) {
            return getRef().getSize();
        }
        checkEntry();
        return super.getSize();
    }

    /**
     * Learn whether this Resource represents a directory.
     * @return boolean flag indicating whether the entry is a directory.
     */
    @Override
    public boolean isDirectory() {
        if (isReference()) {
            return getRef().isDirectory();
        }
        checkEntry();
        return super.isDirectory();
    }

    /**
     * Find out whether this Resource represents an existing Resource.
     * @return boolean existence flag.
     */
    @Override
    public boolean isExists() {
        if (isReference()) {
            return getRef().isExists();
        }
        checkEntry();
        return super.isExists();
    }

    /**
     * Get the file or dir mode for this Resource.
     * @return integer representation of Unix permission mask.
     */
    public int getMode() {
        if (isReference()) {
            return getRef().getMode();
        }
        checkEntry();
        return mode;
    }

    /**
     * Overrides the super version.
     * @param r the Reference to set.
     */
    @Override
    public void setRefid(Reference r) {
        if (archive != null || modeSet) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Compare this ArchiveResource to another Resource.
     * @param another the other Resource against which to compare.
     * @return a negative integer, zero, or a positive integer as this Resource
     *         is less than, equal to, or greater than the specified Resource.
     */
    @Override
    public int compareTo(Resource another) {
        return this.equals(another) ? 0 : super.compareTo(another);
    }

    /**
     * Compare another Object to this ArchiveResource for equality.
     * @param another the other Object to compare.
     * @return true if another is a Resource representing
     *              the same entry in the same archive.
     */
    @Override
    public boolean equals(Object another) {
        if (this == another) {
            return true;
        }
        if (isReference()) {
            return getRef().equals(another);
        }
        if (another == null || !another.getClass().equals(getClass())) {
            return false;
        }
        ArchiveResource r = (ArchiveResource) another;
        return getArchive().equals(r.getArchive())
            && getName().equals(r.getName());
    }

    /**
     * Get the hash code for this Resource.
     * @return hash code as int.
     */
    @Override
    public int hashCode() {
        return super.hashCode()
            * (getArchive() == null ? NULL_ARCHIVE : getArchive().hashCode());
    }

    /**
     * Format this Resource as a String.
     * @return String representation of this Resource.
     */
    @Override
    public String toString() {
        return isReference() ? getRef().toString()
            : getArchive().toString() + ':' + getName();
    }

    /**
     * Validate settings and ensure that the represented "archive entry"
     * has been established.
     */
    protected final synchronized void checkEntry() throws BuildException {
        dieOnCircularReference();
        if (haveEntry) {
            return;
        }
        String name = getName();
        if (name == null) {
            throw new BuildException("entry name not set");
        }
        Resource r = getArchive();
        if (r == null) {
            throw new BuildException("archive attribute not set");
        }
        if (!r.isExists()) {
            throw new BuildException("%s does not exist.", r);
        }
        if (r.isDirectory()) {
            throw new BuildException("%s denotes a directory.", r);
        }
        fetchEntry();
        haveEntry = true;
    }

    /**
     * Fetch information from the named entry inside the archive.
     */
    protected abstract void fetchEntry();

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p) {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            if (archive != null) {
                pushAndInvokeCircularReferenceCheck(archive, stk, p);
            }
            setChecked(true);
        }
    }

    @Override
    protected ArchiveResource getRef() {
        return getCheckedRef(ArchiveResource.class);
    }
}
