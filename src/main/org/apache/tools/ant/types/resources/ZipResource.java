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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FilterInputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipEntry;

/**
 * A Resource representation of an entry in a zipfile.
 * @since Ant 1.7
 */
public class ZipResource extends Resource {
    private static final int NULL_ZIPFILE
        = Resource.getMagicNumber("null zipfile".getBytes());

    private String encoding;
    private File zipfile;
    private boolean haveEntry = false;

    /**
     * Default constructor.
     */
    public ZipResource() {
    }

    /**
     * Construct a ZipResource representing the specified
     * entry in the specified zipfile.
     * @param z the zipfile as File.
     * @param enc the encoding used for filenames.
     * @param e the ZipEntry.
     */
    public ZipResource(File z, String enc, ZipEntry e) {
        setEntry(e);
        setZipfile(z);
        setEncoding(enc);
    }

    /**
     * Set the zipfile that holds this ZipResource.
     * @param z the zipfile as a File.
     */
    public void setZipfile(File z) {
        checkAttributesAllowed();
        zipfile = z;
    }

    /**
     * Get the zipfile that holds this ZipResource.
     * @return the zipfile as a File.
     */
    public File getZipfile() {
        return isReference()
            ? ((ZipResource) getCheckedRef()).getZipfile() : zipfile;
    }

    /**
     * Set the encoding to use with the zipfile.
     * @param enc the String encoding.
     */
    public void setEncoding(String enc) {
        checkAttributesAllowed();
        encoding = enc;
    }

    /**
     * Get the encoding to use with the zipfile.
     * @return String encoding.
     */
    public String getEncoding() {
        return isReference()
            ? ((ZipResource) getCheckedRef()).getEncoding() : encoding;
    }

    /**
     * Get the last modified date of this ZipResource.
     * @return the last modification date.
     */
    public long getLastModified() {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getLastModified();
        }
        checkEntry();
        return super.getLastModified();
    }

    /**
     * Get the size of this ZipResource.
     * @return the long size of this ZipResource.
     */
    public long getSize() {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getSize();
        }
        checkEntry();
        return super.getSize();
    }

    /**
     * Learn whether this ZipResource represents a directory.
     * @return boolean flag indicating whether the zip entry is a directory.
     */
    public boolean isDirectory() {
        if (isReference()) {
            return ((Resource) getCheckedRef()).isDirectory();
        }
        checkEntry();
        return super.isDirectory();
    }

    /**
     * Find out whether this ZipResource represents an existing Resource.
     * @return boolean existence flag.
     */
    public boolean isExists() {
        if (isReference()) {
            return ((Resource) getCheckedRef()).isExists();
        }
        checkEntry();
        return super.isExists();
    }

    /**
     * Overrides the super version.
     * @param r the Reference to set.
     */
    public void setRefid(Reference r) {
        if (encoding != null || zipfile != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Return an InputStream for reading the contents of this Resource.
     * @return an InputStream object.
     * @throws IOException if the zip file cannot be opened,
     *         or the entry cannot be read.
     */
    public InputStream getInputStream() throws IOException {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getInputStream();
        }
        final ZipFile z = new ZipFile(getZipfile(), getEncoding());
        return new FilterInputStream(z.getInputStream(z.getEntry(getName()))) {
            public void close() throws IOException {
                FileUtils.close(in);
                z.close();
            }
            protected void finalize() throws Throwable {
                try {
                    close();
                } finally {
                    super.finalize();
                }
            }
        };
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
        throw new UnsupportedOperationException(
            "Use the zip task for zip output.");
    }

    /**
     * Compare this ZipResource to another Resource.
     * @param another the other Resource against which to compare.
     * @return a negative integer, zero, or a positive integer as this ZipResource
     *         is less than, equal to, or greater than the specified Resource.
     */
    public int compareTo(Object another) {
        return this.equals(another) ? 0 : super.compareTo(another);
    }

    /**
     * Compare another Object to this ZipResource for equality.
     * @param another the other Object to compare.
     * @return true if another is a ZipResource representing
     *              the same entry in the same zipfile.
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
        ZipResource r = (ZipResource) another;
        return getZipfile().equals(r.getZipfile())
            && getName().equals(r.getName());
    }

    /**
     * Get the hash code for this Resource.
     * @return hash code as int.
     */
    public int hashCode() {
        return super.hashCode()
            * (getZipfile() == null ? NULL_ZIPFILE : getZipfile().hashCode());
    }

    /**
     * Format this ZipResource as a String.
     * @return String representatation of this ZipResource.
     */
    public String toString() {
        return isReference() ? getCheckedRef().toString()
            : getZipfile().toString() + ':' + getName();
    }

    private synchronized void checkEntry() throws BuildException {
        if (haveEntry) {
            return;
        }
        String name = getName();
        if (name == null) {
            throw new BuildException("zip entry name not set");
        }
        File f = getZipfile();
        if (f == null) {
            throw new BuildException("zipfile attribute not set");
        }
        if (!f.exists()) {
            throw new BuildException(f.getAbsolutePath() + " does not exist.");
        }
        if (f.isDirectory()) {
            throw new BuildException(f + " denotes a directory.");
        }
        ZipFile z = null;
        try {
            z = new ZipFile(f, getEncoding());
            setEntry(z.getEntry(name));
        } catch (IOException e) {
            log(e.getMessage(), Project.MSG_DEBUG);
            throw new BuildException(e);
        } finally {
            if (z != null) {
                try {
                    z.close();
                } catch (IOException e) {
                    //?
                }
            }
        }
    }

    private synchronized void setEntry(ZipEntry e) {
        haveEntry = true;
        if (e == null) {
            super.setExists(false);
            return;
        }
        super.setName(e.getName());
        super.setExists(true);
        super.setLastModified(e.getTime());
        super.setDirectory(e.isDirectory());
        super.setSize(e.getSize());
    }

}
