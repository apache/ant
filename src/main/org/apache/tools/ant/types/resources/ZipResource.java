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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipExtraField;
import org.apache.tools.zip.ZipFile;

/**
 * A Resource representation of an entry in a zipfile.
 * @since Ant 1.7
 */
public class ZipResource extends ArchiveResource {

    private String encoding;
    private ZipExtraField[] extras;
    private int method;

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
        super(z, true);
        setEncoding(enc);
        setEntry(e);
    }

    /**
     * Set the zipfile that holds this ZipResource.
     * @param z the zipfile as a File.
     */
    public void setZipfile(File z) {
        setArchive(z);
    }

    /**
     * Get the zipfile that holds this ZipResource.
     * @return the zipfile as a File.
     */
    public File getZipfile() {
        FileProvider fp = getArchive().as(FileProvider.class);
        return fp.getFile();
    }

    /**
     * Sets the archive that holds this as a single element Resource
     * collection.
     * @param a the archive as a single element Resource collection.
     */
    public void addConfigured(ResourceCollection a) {
        super.addConfigured(a);
        if (!a.isFilesystemOnly()) {
            throw new BuildException("only filesystem resources are supported");
        }
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
            ? getRef().getEncoding() : encoding;
    }

    /**
     * Overrides the super version.
     * @param r the Reference to set.
     */
    public void setRefid(Reference r) {
        if (encoding != null) {
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
            return getRef().getInputStream();
        }
        return getZipEntryStream(new ZipFile(getZipfile(), getEncoding()), getName());
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
            return getRef().getOutputStream();
        }
        throw new UnsupportedOperationException(
            "Use the zip task for zip output.");
    }

    /**
     * Retrieves extra fields.
     * @return an array of the extra fields
     * @since Ant 1.8.0
     */
    public ZipExtraField[] getExtraFields() {
        if (isReference()) {
            return getRef().getExtraFields();
        }
        checkEntry();
        if (extras == null) {
            return new ZipExtraField[0];
        }
        return extras;
    }

    /**
     * The compression method that has been used.
     * @return int
     * @since Ant 1.8.0
     */
    public int getMethod() {
        return method;
    }

    /**
     * Return an InputStream for reading the contents of a ZipEntry
     * with autoclose.
     * @param zipFile a org.apache.tools.zip.ZipFile
     * @param zipEntry String a name of a zip entry
     * @return an InputStream object
     * @throws IOException if the entry cannot be read
     */
    public static InputStream getZipEntryStream(ZipFile zipFile,
                                                String zipEntry) throws IOException {
        ZipEntry ze = zipFile.getEntry(zipEntry);
        if (ze == null) {
            zipFile.close();
            throw new BuildException("no entry " + zipEntry + " in " + zipFile.getName());
        }
        return new FilterInputStream(zipFile.getInputStream(ze)) {
            public void close() throws IOException {
                FileUtils.close(in);
                zipFile.close();
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
     * fetches information from the named entry inside the archive.
     */
    protected void fetchEntry() {
        ZipFile z = null;
        try {
            z = new ZipFile(getZipfile(), getEncoding());
            setEntry(z.getEntry(getName()));
        } catch (IOException e) {
            log(e.getMessage(), Project.MSG_DEBUG);
            throw new BuildException(e);
        } finally {
            ZipFile.closeQuietly(z);
        }
    }

    @Override
    protected ZipResource getRef() {
        return getCheckedRef(ZipResource.class);
    }

    private void setEntry(ZipEntry e) {
        if (e == null) {
            setExists(false);
            return;
        }
        setName(e.getName());
        setExists(true);
        setLastModified(e.getTime());
        setDirectory(e.isDirectory());
        setSize(e.getSize());
        setMode(e.getUnixMode());
        extras = e.getExtraFields(true);
        method = e.getMethod();
    }

}
