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

package org.apache.tools.zip;

import java.util.Vector;
import java.util.zip.ZipException;

/**
 * Extension that adds better handling of extra fields and provides
 * access to the internal and external file attributes.
 *
 */
public class ZipEntry extends java.util.zip.ZipEntry implements Cloneable {

    private static final int PLATFORM_UNIX = 3;
    private static final int PLATFORM_FAT  = 0;

    private int internalAttributes = 0;
    private int platform = PLATFORM_FAT;
    private long externalAttributes = 0;
    private Vector/*<ZipExtraField>*/ extraFields = null;
    private String name = null;

    /**
     * Creates a new zip entry with the specified name.
     * @param name the name of the entry
     * @since 1.1
     */
    public ZipEntry(String name) {
        super(name);
    }

    /**
     * Creates a new zip entry with fields taken from the specified zip entry.
     * @param entry the entry to get fields from
     * @since 1.1
     * @throws ZipException on error
     */
    public ZipEntry(java.util.zip.ZipEntry entry) throws ZipException {
        super(entry);
        byte[] extra = entry.getExtra();
        if (extra != null) {
            setExtraFields(ExtraFieldUtils.parse(extra));
        } else {
            // initializes extra data to an empty byte array
            setExtra();
        }
    }

    /**
     * Creates a new zip entry with fields taken from the specified zip entry.
     * @param entry the entry to get fields from
     * @throws ZipException on error
     * @since 1.1
     */
    public ZipEntry(ZipEntry entry) throws ZipException {
        this((java.util.zip.ZipEntry) entry);
        setInternalAttributes(entry.getInternalAttributes());
        setExternalAttributes(entry.getExternalAttributes());
        setExtraFields(entry.getExtraFields());
    }

    /**
     * @since 1.9
     */
    protected ZipEntry() {
        super("");
    }

    /**
     * Overwrite clone.
     * @return a cloned copy of this ZipEntry
     * @since 1.1
     */
    public Object clone() {
        ZipEntry e = (ZipEntry) super.clone();

        e.extraFields = extraFields != null ? (Vector) extraFields.clone() : null;
        e.setInternalAttributes(getInternalAttributes());
        e.setExternalAttributes(getExternalAttributes());
        e.setExtraFields(getExtraFields());
        return e;
    }

    /**
     * Retrieves the internal file attributes.
     *
     * @return the internal file attributes
     * @since 1.1
     */
    public int getInternalAttributes() {
        return internalAttributes;
    }

    /**
     * Sets the internal file attributes.
     * @param value an <code>int</code> value
     * @since 1.1
     */
    public void setInternalAttributes(int value) {
        internalAttributes = value;
    }

    /**
     * Retrieves the external file attributes.
     * @return the external file attributes
     * @since 1.1
     */
    public long getExternalAttributes() {
        return externalAttributes;
    }

    /**
     * Sets the external file attributes.
     * @param value an <code>long</code> value
     * @since 1.1
     */
    public void setExternalAttributes(long value) {
        externalAttributes = value;
    }

    /**
     * Sets Unix permissions in a way that is understood by Info-Zip's
     * unzip command.
     * @param mode an <code>int</code> value
     * @since Ant 1.5.2
     */
    public void setUnixMode(int mode) {
        setExternalAttributes((mode << 16)
                              // MS-DOS read-only attribute
                              | ((mode & 0200) == 0 ? 1 : 0)
                              // MS-DOS directory flag
                              | (isDirectory() ? 0x10 : 0));
        platform = PLATFORM_UNIX;
    }

    /**
     * Unix permission.
     * @return the unix permissions
     * @since Ant 1.6
     */
    public int getUnixMode() {
        return (int) ((getExternalAttributes() >> 16) & 0xFFFF);
    }

    /**
     * Platform specification to put into the &quot;version made
     * by&quot; part of the central file header.
     *
     * @return 0 (MS-DOS FAT) unless {@link #setUnixMode setUnixMode}
     * has been called, in which case 3 (Unix) will be returned.
     *
     * @since Ant 1.5.2
     */
    public int getPlatform() {
        return platform;
    }

    /**
     * Set the platform (UNIX or FAT).
     * @param platform an <code>int</code> value - 0 is FAT, 3 is UNIX
     * @since 1.9
     */
    protected void setPlatform(int platform) {
        this.platform = platform;
    }

    /**
     * Replaces all currently attached extra fields with the new array.
     * @param fields an array of extra fields
     * @since 1.1
     */
    public void setExtraFields(ZipExtraField[] fields) {
        extraFields = new Vector();
        for (int i = 0; i < fields.length; i++) {
            extraFields.addElement(fields[i]);
        }
        setExtra();
    }

    /**
     * Retrieves extra fields.
     * @return an array of the extra fields
     * @since 1.1
     */
    public ZipExtraField[] getExtraFields() {
        if (extraFields == null) {
            return new ZipExtraField[0];
        }
        ZipExtraField[] result = new ZipExtraField[extraFields.size()];
        extraFields.copyInto(result);
        return result;
    }

    /**
     * Adds an extra fields - replacing an already present extra field
     * of the same type.
     * @param ze an extra field
     * @since 1.1
     */
    public void addExtraField(ZipExtraField ze) {
        if (extraFields == null) {
            extraFields = new Vector();
        }
        ZipShort type = ze.getHeaderId();
        boolean done = false;
        for (int i = 0, fieldsSize = extraFields.size(); !done && i < fieldsSize; i++) {
            if (((ZipExtraField) extraFields.elementAt(i)).getHeaderId().equals(type)) {
                extraFields.setElementAt(ze, i);
                done = true;
            }
        }
        if (!done) {
            extraFields.addElement(ze);
        }
        setExtra();
    }

    /**
     * Remove an extra fields.
     * @param type the type of extra field to remove
     * @since 1.1
     */
    public void removeExtraField(ZipShort type) {
        if (extraFields == null) {
            extraFields = new Vector();
        }
        boolean done = false;
        for (int i = 0, fieldsSize = extraFields.size(); !done && i < fieldsSize; i++) {
            if (((ZipExtraField) extraFields.elementAt(i)).getHeaderId().equals(type)) {
                extraFields.removeElementAt(i);
                done = true;
            }
        }
        if (!done) {
            throw new java.util.NoSuchElementException();
        }
        setExtra();
    }

    /**
     * Throws an Exception if extra data cannot be parsed into extra fields.
     * @param extra an array of bytes to be parsed into extra fields
     * @throws RuntimeException if the bytes cannot be parsed
     * @since 1.1
     * @throws RuntimeException on error
     */
    public void setExtra(byte[] extra) throws RuntimeException {
        try {
            setExtraFields(ExtraFieldUtils.parse(extra));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Unfortunately {@link java.util.zip.ZipOutputStream
     * java.util.zip.ZipOutputStream} seems to access the extra data
     * directly, so overriding getExtra doesn't help - we need to
     * modify super's data directly.
     *
     * @since 1.1
     */
    protected void setExtra() {
        super.setExtra(ExtraFieldUtils.mergeLocalFileDataData(getExtraFields()));
    }

    /**
     * Retrieves the extra data for the local file data.
     * @return the extra data for local file
     * @since 1.1
     */
    public byte[] getLocalFileDataExtra() {
        byte[] extra = getExtra();
        return extra != null ? extra : new byte[0];
    }

    /**
     * Retrieves the extra data for the central directory.
     * @return the central directory extra data
     * @since 1.1
     */
    public byte[] getCentralDirectoryExtra() {
        return ExtraFieldUtils.mergeCentralDirectoryData(getExtraFields());
    }

    /**
     * Make this class work in JDK 1.1 like a 1.2 class.
     *
     * <p>This either stores the size for later usage or invokes
     * setCompressedSize via reflection.</p>
     * @param size the size to use
     * @deprecated since 1.7.
     *             Use setCompressedSize directly.
     * @since 1.2
     */
    public void setComprSize(long size) {
        setCompressedSize(size);
    }

    /**
     * Get the name of the entry.
     * @return the entry name
     * @since 1.9
     */
    public String getName() {
        return name == null ? super.getName() : name;
    }

    /**
     * Is this entry a directory?
     * @return true if the entry is a directory
     * @since 1.10
     */
    public boolean isDirectory() {
        return getName().endsWith("/");
    }

    /**
     * Set the name of the entry.
     * @param name the name to use
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * Get the hashCode of the entry.
     * This uses the name as the hashcode.
     * @return a hashcode.
     * @since Ant 1.7
     */
    public int hashCode() {
        // this method has severe consequences on performance. We cannot rely
        // on the super.hashCode() method since super.getName() always return
        // the empty string in the current implemention (there's no setter)
        // so it is basically draining the performance of a hashmap lookup
        return getName().hashCode();
    }

    /**
     * The equality method. In this case, the implementation returns 'this == o'
     * which is basically the equals method of the Object class.
     * @param o the object to compare to
     * @return true if this object is the same as <code>o</code>
     * @since Ant 1.7
     */
    public boolean equals(Object o) {
        return (this == o);
    }

}
