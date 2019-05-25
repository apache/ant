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
package org.apache.tools.ant.types;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import org.apache.tools.ant.types.resources.FileProvider;

/**
 * Describes a "File-like" resource (File, ZipEntry, etc.).
 * <p>
 * This class is meant to be used by classes needing to record path
 * and date/time information about a file, a zip entry or some similar
 * resource (URL, archive in a version control repository, ...).
 * </p>
 *
 * @since Ant 1.5.2
 * @see org.apache.tools.ant.types.resources.Touchable
 */
public class Resource extends DataType implements Comparable<Resource>, ResourceCollection {

    /** Constant unknown size */
    public static final long UNKNOWN_SIZE = -1;

    /** Constant unknown datetime for getLastModified */
    public static final long UNKNOWN_DATETIME = 0L;

    /** Magic number */
    protected static final int MAGIC = getMagicNumber("Resource".getBytes());

    private static final int NULL_NAME = getMagicNumber("null name".getBytes());

    /**
     * Create a "magic number" for use in hashCode calculations.
     * @param seed byte[] to seed with.
     * @return a magic number as int.
     */
    protected static int getMagicNumber(byte[] seed) {
        return new BigInteger(seed).intValue();
    }

    private String name = null;
    private Boolean exists = null;
    private Long lastmodified = null;
    private Boolean directory = null;
    private Long size = null;

    /**
     * Default constructor.
     */
    public Resource() {
    }

    /**
     * Only sets the name.
     *
     * <p>This is a dummy, used for not existing resources.</p>
     *
     * @param name relative path of the resource.  Expects
     * &quot;/&quot; to be used as the directory separator.
     */
    public Resource(String name) {
        this(name, false, 0, false);
    }

    /**
     * Sets the name, lastmodified flag, and exists flag.
     *
     * @param name relative path of the resource.  Expects
     * &quot;/&quot; to be used as the directory separator.
     * @param exists if true, this resource exists.
     * @param lastmodified the last modification time of this resource.
     */
    public Resource(String name, boolean exists, long lastmodified) {
        this(name, exists, lastmodified, false);
    }

    /**
     * Sets the name, lastmodified flag, exists flag, and directory flag.
     *
     * @param name relative path of the resource.  Expects
     * &quot;/&quot; to be used as the directory separator.
     * @param exists if true the resource exists
     * @param lastmodified the last modification time of the resource
     * @param directory    if true, this resource is a directory
     */
    public Resource(String name, boolean exists, long lastmodified, boolean directory) {
        this(name, exists, lastmodified, directory, UNKNOWN_SIZE);
    }

    /**
     * Sets the name, lastmodified flag, exists flag, directory flag, and size.
     *
     * @param name relative path of the resource.  Expects
     * &quot;/&quot; to be used as the directory separator.
     * @param exists if true the resource exists
     * @param lastmodified the last modification time of the resource
     * @param directory    if true, this resource is a directory
     * @param size the size of this resource.
     */
    public Resource(String name, boolean exists, long lastmodified, boolean directory, long size) {
        this.name = name;
        setName(name);
        setExists(exists);
        setLastModified(lastmodified);
        setDirectory(directory);
        setSize(size);
    }

    /**
     * Name attribute will contain the path of a file relative to the
     * root directory of its fileset or the recorded path of a zip
     * entry.
     *
     * <p>example for a file with fullpath /var/opt/adm/resource.txt
     * in a file set with root dir /var/opt it will be
     * adm/resource.txt.</p>
     *
     * <p>&quot;/&quot; will be used as the directory separator.</p>
     * @return the name of this resource.
     */
    public String getName() {
        return isReference() ? getRef().getName() : name;
    }

    /**
     * Set the name of this Resource.
     * @param name relative path of the resource.  Expects
     * &quot;/&quot; to be used as the directory separator.
     */
    public void setName(String name) {
        checkAttributesAllowed();
        this.name = name;
    }

    /**
     * The exists attribute tells whether a resource exists.
     * @return true if this resource exists.
     */
    public boolean isExists() {
        if (isReference()) {
            return getRef().isExists();
        }
        //default true:
        return exists == null || exists;
    }

    /**
     * Set the exists attribute.
     * @param exists if true, this resource exists.
     */
    public void setExists(boolean exists) {
        checkAttributesAllowed();
        this.exists = exists ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Tells the modification time in milliseconds since 01.01.1970 (the "epoch").
     *
     * @return the modification time, if that is meaningful
     *            (e.g. for a file resource which exists);
     *         0 if the resource does not exist, to mirror the behavior
     *         of {@link java.io.File#lastModified};
     *         or 0 if the notion of modification time is meaningless for this class
     *           of resource (e.g. an inline string)
     */
    public long getLastModified() {
        if (isReference()) {
            return getRef().getLastModified();
        }
        if (!isExists() || lastmodified == null) {
            return UNKNOWN_DATETIME;
        }
        long result = lastmodified;
        return result < UNKNOWN_DATETIME ? UNKNOWN_DATETIME : result;
    }

    /**
     * Set the last modification attribute.
     * @param lastmodified the modification time in milliseconds since 01.01.1970.
     */
    public void setLastModified(long lastmodified) {
        checkAttributesAllowed();
        this.lastmodified = lastmodified;
    }

    /**
     * Tells if the resource is a directory.
     * @return boolean flag indicating if the resource is a directory.
     */
    public boolean isDirectory() {
        if (isReference()) {
            return getRef().isDirectory();
        }
        //default false:
        return directory != null && directory;
    }

    /**
     * Set the directory attribute.
     * @param directory if true, this resource is a directory.
     */
    public void setDirectory(boolean directory) {
        checkAttributesAllowed();
        this.directory = directory ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Set the size of this Resource.
     * @param size the size, as a long.
     * @since Ant 1.6.3
     */
    public void setSize(long size) {
        checkAttributesAllowed();
        this.size = size > UNKNOWN_SIZE ? size : UNKNOWN_SIZE;
    }

    /**
     * Get the size of this Resource.
     * @return the size, as a long, 0 if the Resource does not exist (for
     *         compatibility with java.io.File), or UNKNOWN_SIZE if not known.
     * @since Ant 1.6.3
     */
    public long getSize() {
        if (isReference()) {
            return getRef().getSize();
        }
        return isExists()
            ? (size != null ? size : UNKNOWN_SIZE)
            : 0L;
    }

    /**
     * Clone this Resource.
     * @return copy of this.
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException(
                "CloneNotSupportedException for a Resource caught. Derived classes must support cloning.");
        }
    }

    /**
     * Delegates to a comparison of names.
     * @param other the object to compare to.
     * @return a negative integer, zero, or a positive integer as this Resource
     *         is less than, equal to, or greater than the specified Resource.
     * @since Ant 1.6
     */
    @Override
    public int compareTo(Resource other) {
        if (isReference()) {
            return getRef().compareTo(other);
        }
        return toString().compareTo(other.toString());
    }

    /**
     * Implement basic Resource equality.
     * @param other the object to check against.
     * @return true if the specified Object is equal to this Resource.
     * @since Ant 1.7
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (isReference()) {
            return getRef().equals(other);
        }
        return other != null && other.getClass().equals(getClass())
            && compareTo((Resource) other) == 0;
    }

    /**
     * Get the hash code for this Resource.
     * @return hash code as int.
     * @since Ant 1.7
     */
    @Override
    public int hashCode() {
        if (isReference()) {
            return getRef().hashCode();
        }
        String name = getName();
        return MAGIC * (name == null ? NULL_NAME : name.hashCode());
    }

    /**
     * Get an InputStream for the Resource.
     * @return an InputStream containing this Resource's content.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if InputStreams are not
     *         supported for this Resource type.
     * @since Ant 1.7
     */
    public InputStream getInputStream() throws IOException {
        if (isReference()) {
            return getRef().getInputStream();
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Get an OutputStream for the Resource.
     * @return an OutputStream to which content can be written.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if OutputStreams are not
     *         supported for this Resource type.
     * @since Ant 1.7
     */
    public OutputStream getOutputStream() throws IOException {
        if (isReference()) {
            return getRef().getOutputStream();
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return an Iterator of Resources.
     * @since Ant 1.7
     */
    @Override
    public Iterator<Resource> iterator() {
        return isReference() ? getRef().iterator()
            : Collections.singleton(this).iterator();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return the size of this ResourceCollection.
     * @since Ant 1.7
     */
    @Override
    public int size() {
        return isReference() ? getRef().size() : 1;
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return whether this Resource is a FileProvider.
     * @since Ant 1.7
     */
    @Override
    public boolean isFilesystemOnly() {
        return (isReference() && getRef().isFilesystemOnly())
            || this.as(FileProvider.class) != null;
    }

    /**
     * Get the string representation of this Resource.
     * @return this Resource formatted as a String.
     * @since Ant 1.7
     */
    @Override
    public String toString() {
        if (isReference()) {
            return getRef().toString();
        }
        String n = getName();
        return n == null ? "(anonymous)" : n;
    }

    /**
     * Get a long String representation of this Resource.
     * This typically should be the value of <code>toString()</code>
     * prefixed by a type description.
     * @return this Resource formatted as a long String.
     * @since Ant 1.7
     */
    public final String toLongString() {
        return isReference() ? getRef().toLongString()
            : getDataTypeName() + " \"" + toString() + '"';
    }

    /**
     * Overrides the base version.
     * @param r the Reference to set.
     */
    @Override
    public void setRefid(Reference r) {
        if (name != null
            || exists != null
            || lastmodified != null
            || directory != null
            || size != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Returns a view of this resource that implements the interface
     * given as the argument or null if there is no such view.
     *
     * <p>This allows extension interfaces to be added to resources
     * without growing the number of permutations of interfaces
     * decorators/adapters need to implement.</p>
     *
     * <p>This implementation of the method will return the current
     * instance itself if it can be assigned to the given class.</p>
     *
     * @param <T> desired type
     * @param clazz a class
     * @return resource of a desired type
     * @since Ant 1.8.0
     */
    public <T> T as(Class<T> clazz) {
        return clazz.isAssignableFrom(getClass()) ? clazz.cast(this) : null;
    }

    /**
     * Return {@link #as(Class)} as an {@link Optional}.
     * @param <T> desired type
     * @param clazz a class
     * @return {@link Optional} resource of a desired type
     * @since Ant 1.10.2
     */
    public <T> Optional<T> asOptional(Class<T> clazz) {
        return Optional.ofNullable(as(clazz));
    }

    /**
     * Perform the check for circular references and return the
     * referenced Resource.
     * @return <code>Resource</code>.
     */
    protected Resource getRef() {
        return getCheckedRef(Resource.class);
    }
}
