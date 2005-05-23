/*
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.tools.ant.types.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.Reference;

/**
 * A Resource representation of a File.
 * @since Ant 1.7
 */
public class FileResource extends Resource implements Touchable {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final int NULL_FILE
        = Resource.getMagicNumber("null file".getBytes());

    private File file;
    private File base;

    /**
     * Default constructor.
     */
    public FileResource() {
    }

    /**
     * Construct a new FileResource using the specified base File and relative name.
     * @param b      the base File (directory).
     * @param name   the relative filename.
     */
    public FileResource(File b, String name) {
        setFile(FILE_UTILS.resolveFile(b, name));
        setBase(b);
    }

    /**
     * Construct a new FileResource from a File.
     * @param f the File represented.
     */
    public FileResource(File f) {
        setFile(f);
    }

    /**
     * Constructor for Ant attribute introspection.
     * @param p the Project against which to resolve <code>s</code>.
     * @param s the absolute or Project-relative filename as a String.
     * @see org.apache.tools.ant.IntrospectionHelper
     */
    public FileResource(Project p, String s) {
        this(p.resolveFile(s));
        setProject(p);
    }

    /**
     * Set the File for this FileResource.
     * @param f the File to be represented.
     */
    public void setFile(File f) {
        checkAttributesAllowed();
        file = f;
    }

    /**
     * Get the file represented by this FileResource.
     * @return the File.
     */
    public File getFile() {
        return isReference() ? ((FileResource) getCheckedRef()).getFile() : file;
    }

    /**
     * Set the base File for this FileResource.
     * @param b the base File.
     */
    public void setBase(File b) {
        checkAttributesAllowed();
        base = b;
    }

    /**
     * Return the base to which the name is relative.
     * @return the base File.
     */
    public File getBase() {
        return isReference()
            ? ((FileResource) getCheckedRef()).getBase() : base;
    }

    /**
     * Overrides the super version.
     * @param r the Reference to set.
     */
    public void setRefid(Reference r) {
        if (file != null || base != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Get the name of this FileResource relative to its base, if any.
     * @return the name of this resource.
     */
    public String getName() {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getName();
        }
        File b = getBase();
        return b == null ? getNotNullFile().getAbsolutePath()
            : FILE_UTILS.removeLeadingPath(b, getNotNullFile());
    }

    /**
     * Learn whether this file exists.
     * @return true if this resource exists.
     */
    public boolean isExists() {
        return isReference() ? ((Resource) getCheckedRef()).isExists()
            : getNotNullFile().exists();
    }

    /**
     * Get the modification time in milliseconds since 01.01.1970 .
     * @return 0 if the resource does not exist.
     */
    public long getLastModified() {
        return isReference()
            ? ((Resource) getCheckedRef()).getLastModified()
            : getNotNullFile().lastModified();
    }

    /**
     * Learn whether the resource is a directory.
     * @return boolean flag indicating if the resource is a directory.
     */
    public boolean isDirectory() {
        return isReference() ? ((Resource) getCheckedRef()).isDirectory()
            : getNotNullFile().isDirectory();
    }

    /**
     * Get the size of this Resource.
     * @return the size, as a long, 0 if the Resource does not exist.
     */
    public long getSize() {
        return isReference() ? ((Resource) getCheckedRef()).getSize()
            : getNotNullFile().length();
    }

    /**
     * Return an InputStream for reading the contents of this Resource.
     * @return an InputStream object.
     * @throws IOException if an error occurs.
     */
    public InputStream getInputStream() throws IOException {
        return isReference()
            ? ((Resource) getCheckedRef()).getInputStream()
            : new FileInputStream(getNotNullFile());
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
        if (isReference()) {
            return ((Resource) getCheckedRef()).getOutputStream();
        }
        File f = getNotNullFile();
        if (f.exists()) {
            if (f.isFile()) {
                f.delete();
            }
        } else {
            File p = f.getParentFile();
            if (p != null && !(p.exists())) {
                p.mkdirs();
            }
        }
        return new FileOutputStream(f);
    }

    /**
     * Compare this FileResource to another Resource.
     * @param another the other Resource against which to compare.
     * @return a negative integer, zero, or a positive integer as this FileResource
     *         is less than, equal to, or greater than the specified Resource.
     */
    public int compareTo(Object another) {
        if (isReference()) {
            return ((Comparable) getCheckedRef()).compareTo(another);
        }
        return this.equals(another) ? 0 : super.compareTo(another);
    }

    /**
     * Compare another Object to this FileResource for equality.
     * @param another the other Object to compare.
     * @return true if another is a FileResource representing the same file.
     */
    public boolean equals(Object another) {
        if (this == another) {
            return true;
        }
        if (isReference()) {
            return getCheckedRef().equals(another);
        }
        if (!(another.getClass().equals(getClass()))) {
            return false;
        }
        FileResource otherfr = (FileResource) another;
        return getFile() == null
            ? otherfr.getFile() == null
            : getFile().equals(otherfr.getFile());
    }

    /**
     * Get the hash code for this Resource.
     * @return hash code as int.
     */
    public int hashCode() {
        if (isReference()) {
            return getCheckedRef().hashCode();
        }
        return MAGIC * (getFile() == null ? NULL_FILE : getFile().hashCode());
    }

    /**
     * Get the string representation of this Resource.
     * @return this FileResource formatted as a String.
     */
    public String toString() {
        return isReference() ? getCheckedRef().toString()
            : FILE_UTILS.normalize(file.getAbsolutePath()).getAbsolutePath();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return whether this Resource is a FileResource.
     */
    public boolean isFilesystemOnly() {
        return !isReference()
            || ((FileResource) getCheckedRef()).isFilesystemOnly();
    }

    /**
     * Implement the Touchable interface.
     * @param modTime new last modification time.
     */
    public void touch(long modTime) {
        if (isReference()) {
            ((FileResource) getCheckedRef()).touch(modTime);
            return;
        }
        getNotNullFile().setLastModified(modTime);
    }

    /**
     * Get the file represented by this FileResource, ensuring it is not null.
     * @return the not-null File.
     * @throws BuildException if file is null.
     */
    protected File getNotNullFile() {
        if (getFile() == null) {
            throw new BuildException("file attribute is null!");
        }
        return getFile();
    }

}
