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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceFactory;
import org.apache.tools.ant.util.FileUtils;

/**
 * A Resource representation of a File.
 * @since Ant 1.7
 */
public class FileResource extends Resource implements Touchable, FileProvider,
        ResourceFactory, Appendable {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final int NULL_FILE
        = Resource.getMagicNumber("null file".getBytes());

    private File file;
    private File baseDir;

    /**
     * Default constructor.
     */
    public FileResource() {
    }

    /**
     * Construct a new FileResource using the specified basedir and relative name.
     * @param b      the basedir as File.
     * @param name   the relative filename.
     */
    public FileResource(File b, String name) {
        this.baseDir = b;
        this.file = FILE_UTILS.resolveFile(b, name);
    }

    /**
     * Construct a new FileResource from a File.
     * @param f the File represented.
     */
    public FileResource(File f) {
        setFile(f);
    }

    /**
     * Create a new FileResource.
     * @param p Project
     * @param f File represented
     * @since Ant 1.8
     */
    public FileResource(Project p, File f) {
        this(f);
        setProject(p);
    }

    /**
     * Constructor for Ant attribute introspection.
     * @param p the Project against which to resolve <code>s</code>.
     * @param s the absolute or Project-relative filename as a String.
     * @see org.apache.tools.ant.IntrospectionHelper
     */
    public FileResource(Project p, String s) {
        this(p, p.resolveFile(s));
    }

    /**
     * Set the File for this FileResource.
     * @param f the File to be represented.
     */
    public void setFile(File f) {
        checkAttributesAllowed();
        file = f;
        if (f != null && (getBaseDir() == null || !FILE_UTILS.isLeadingPath(getBaseDir(), f))) {
            setBaseDir(f.getParentFile());
        }
    }

    /**
     * Get the file represented by this FileResource.
     * @return the File.
     */
    @Override
    public File getFile() {
        if (isReference()) {
            return getRef().getFile();
        }
        dieOnCircularReference();
        synchronized (this) {
            if (file == null) {
                //try to resolve file set via basedir/name property setters:
                File d = getBaseDir();
                String n = super.getName();
                if (n != null) {
                    setFile(FILE_UTILS.resolveFile(d, n));
                }
            }
        }
        return file;
    }

    /**
     * Set the basedir for this FileResource.
     * @param b the basedir as File.
     */
    public void setBaseDir(File b) {
        checkAttributesAllowed();
        baseDir = b;
    }

    /**
     * Return the basedir to which the name is relative.
     * @return the basedir as File.
     */
    public File getBaseDir() {
        if (isReference()) {
            return getRef().getBaseDir();
        }
        dieOnCircularReference();
        return baseDir;
    }

    /**
     * Overrides the super version.
     * @param r the Reference to set.
     */
    @Override
    public void setRefid(Reference r) {
        if (file != null || baseDir != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Get the name of this FileResource.  If the basedir is set,
     * the name will be relative to that.  Otherwise the basename
     * only will be returned.
     * @return the name of this resource.
     */
    @Override
    public String getName() {
        if (isReference()) {
            return getRef().getName();
        }
        File b = getBaseDir();
        return b == null ? getNotNullFile().getName()
            : FILE_UTILS.removeLeadingPath(b, getNotNullFile());
    }

    /**
     * Learn whether this file exists.
     * @return true if this resource exists.
     */
    @Override
    public boolean isExists() {
        return isReference() ? getRef().isExists()
            : getNotNullFile().exists();
    }

    /**
     * Get the modification time in milliseconds since 01.01.1970 .
     * @return 0 if the resource does not exist.
     */
    @Override
    public long getLastModified() {
        return isReference()
            ? getRef().getLastModified()
            : getNotNullFile().lastModified();
    }

    /**
     * Learn whether the resource is a directory.
     * @return boolean flag indicating if the resource is a directory.
     */
    @Override
    public boolean isDirectory() {
        return isReference() ? getRef().isDirectory()
            : getNotNullFile().isDirectory();
    }

    /**
     * Get the size of this Resource.
     * @return the size, as a long, 0 if the Resource does not exist.
     */
    @Override
    public long getSize() {
        return isReference() ? getRef().getSize()
            : getNotNullFile().length();
    }

    /**
     * Return an InputStream for reading the contents of this Resource.
     * @return an InputStream object.
     * @throws IOException if an error occurs.
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return isReference() ? getRef().getInputStream()
            : Files.newInputStream(getNotNullFile().toPath());
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
        if (isReference()) {
            return getRef().getOutputStream();
        }
        return getOutputStream(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream getAppendOutputStream() throws IOException {
        if (isReference()) {
            return getRef().getAppendOutputStream();
        }
        return getOutputStream(true);
    }

    private OutputStream getOutputStream(boolean append) throws IOException {
        File f = getNotNullFile();
        if (f.exists()) {
            if (Files.isSymbolicLink(f.toPath()) && f.isFile() && !append) {
                // https://bz.apache.org/bugzilla/show_bug.cgi?id=624
                f.delete();
            }
        } else {
            File p = f.getParentFile();
            if (p != null && !(p.exists())) {
                p.mkdirs();
            }
        }
        return FileUtils.newOutputStream(f.toPath(), append);
    }

    /**
     * Compare this FileResource to another Resource.
     * @param another the other Resource against which to compare.
     * @return a negative integer, zero, or a positive integer as this FileResource
     *         is less than, equal to, or greater than the specified Resource.
     */
    @Override
    public int compareTo(Resource another) {
        if (isReference()) {
            return getRef().compareTo(another);
        }
        if (this.equals(another)) {
            return 0;
        }
        FileProvider otherFP = another.as(FileProvider.class);
        if (otherFP != null) {
            File f = getFile();
            if (f == null) {
                return -1;
            }
            File of = otherFP.getFile();
            if (of == null) {
                return 1;
            }
            int compareFiles = f.compareTo(of);
            return compareFiles != 0 ? compareFiles
                : getName().compareTo(another.getName());
        }
        return super.compareTo(another);
    }

    /**
     * Compare another Object to this FileResource for equality.
     * @param another the other Object to compare.
     * @return true if another is a FileResource representing the same file.
     */
    @Override
    public boolean equals(Object another) {
        if (this == another) {
            return true;
        }
        if (isReference()) {
            return getRef().equals(another);
        }
        if (another == null || !(another.getClass().equals(getClass()))) {
            return false;
        }
        FileResource otherfr = (FileResource) another;
        return getFile() == null
            ? otherfr.getFile() == null
            : getFile().equals(otherfr.getFile()) && getName().equals(otherfr.getName());
    }

    /**
     * Get the hash code for this Resource.
     * @return hash code as int.
     */
    @Override
    public int hashCode() {
        if (isReference()) {
            return getRef().hashCode();
        }
        return MAGIC * (getFile() == null ? NULL_FILE : getFile().hashCode());
    }

    /**
     * Get the string representation of this Resource.
     * @return this FileResource formatted as a String.
     */
    @Override
    public String toString() {
        if (isReference()) {
            return getRef().toString();
        }
        if (file == null) {
            return "(unbound file resource)";
        }
        String absolutePath = file.getAbsolutePath();
        return FILE_UTILS.normalize(absolutePath).getAbsolutePath();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return whether this Resource is a FileResource.
     */
    @Override
    public boolean isFilesystemOnly() {
        if (isReference()) {
            return getRef().isFilesystemOnly();
        }
        dieOnCircularReference();
        return true;
    }

    /**
     * Implement the Touchable interface.
     * @param modTime new last modification time.
     */
    @Override
    public void touch(long modTime) {
        if (isReference()) {
            getRef().touch(modTime);
            return;
        }
        if (!getNotNullFile().setLastModified(modTime)) {
            log("Failed to change file modification time", Project.MSG_WARN);
        }
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
        dieOnCircularReference();
        return getFile();
    }

    /**
     * Create a new resource that matches a relative or absolute path.
     * If the current instance has a compatible baseDir attribute, it is copied.
     * @param path relative/absolute path to a resource
     * @return a new resource of type FileResource
     * @throws BuildException if desired
     * @since Ant1.8
     */
    @Override
    public Resource getResource(String path) {
        File newfile = FILE_UTILS.resolveFile(getFile(), path);
        FileResource fileResource = new FileResource(newfile);
        if (FILE_UTILS.isLeadingPath(getBaseDir(), newfile)) {
            fileResource.setBaseDir(getBaseDir());
        }
        return fileResource;
    }

    @Override
    protected FileResource getRef() {
        return getCheckedRef(FileResource.class);
    }
}
