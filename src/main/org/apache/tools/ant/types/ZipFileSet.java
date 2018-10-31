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
package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.ZipResource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StreamUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * A ZipFileSet is a FileSet with extra attributes useful in the context of
 * Zip/Jar tasks.
 *
 * A ZipFileSet extends FileSets with the ability to extract a subset of the
 * entries of a Zip file for inclusion in another Zip file.  It also includes
 * a prefix attribute which is prepended to each entry in the output Zip file.
 *
 * Since ant 1.6 ZipFileSet can be defined with an id and referenced in packaging tasks
 *
 */
public class ZipFileSet extends ArchiveFileSet {

    /** Constructor for ZipFileSet */
    public ZipFileSet() {
        super();
    }

    /**
     * Constructor using a fileset argument.
     * @param fileset the fileset to use
     */
    protected ZipFileSet(FileSet fileset) {
        super(fileset);
    }

    /**
     * Constructor using a zipfileset argument.
     * @param fileset the zipfileset to use
     */
    protected ZipFileSet(ZipFileSet fileset) {
        super(fileset);
    }

    /**
     * Return a new archive scanner based on this one.
     * @return a new ZipScanner with the same encoding as this one.
     */
    @Override
    protected ArchiveScanner newArchiveScanner() {
        return this.newArchiveScanner(false);
    }

    /**
     * @return Returns a new archive scanner for this {@code ZipFileSet}. The
     * returned archive scanner, may hold on to open resources, if the
     * {@code mayKeepOpenResources} is {@code true}.
     *
     * @param mayKeepOpenResources {@code true} if the archive scanner being
     *                              returned is allowed to hold onto open resources.
     *                             {@code false} otherwise.
     * @since Ant 1.10.6
     */
    @Override
    ArchiveScanner newArchiveScanner(final boolean mayKeepOpenResources) {
        final ZipScanner zs = mayKeepOpenResources ? new LiveZipScanner() : new ZipScanner();
        zs.setEncoding(getEncoding());
        return zs;
    }

    /**
     * A ZipFileset accepts another ZipFileSet or a FileSet as reference
     * FileSets are often used by the war task for the lib attribute
     * @param p the project to use
     * @return the abstract fileset instance
     */
    @Override
    protected AbstractFileSet getRef(Project p) {
        dieOnCircularReference(p);
        Object o = getRefid().getReferencedObject(p);
        if (o instanceof ZipFileSet) {
            return (AbstractFileSet) o;
        }
        if (o instanceof FileSet) {
            ZipFileSet zfs = new ZipFileSet((FileSet) o);
            configureFileSet(zfs);
            return zfs;
        }
        String msg = getRefid().getRefId() + " doesn\'t denote a zipfileset or a fileset";
        throw new BuildException(msg);
    }

    /**
     * Return a ZipFileSet that has the same properties
     * as this one.
     * @return the cloned zipFileSet
     */
    @Override
    public Object clone() {
        if (isReference()) {
            return getRef(getProject()).clone();
        }
        return super.clone();
    }

    /**
     * A {@link ZipScanner} which holds onto an open {@link ZipFile} and uses that
     * {@code ZipFile} for iterating over its entries. The {@link #close()} method
     * is expected to be explicitly called, to release the open {@code ZipFile}
     * resource, when this scanner is no longer needed.
     */
    private static class LiveZipScanner extends ZipScanner implements AutoCloseable {
        private ZipFile zipFile;

        @Override
        protected void fillMapsFromArchive(final Resource src, final String encoding,
                                           final Map<String, Resource> fileEntries, final Map<String, Resource> matchFileEntries,
                                           final Map<String, Resource> dirEntries, final Map<String, Resource> matchDirEntries) {

            final File srcFile = src.asOptional(FileProvider.class)
                    .map(FileProvider::getFile).orElseThrow(() -> new BuildException(
                            "Only file provider resources are supported"));
            // close any previously opened instance of the zip file
            if (this.zipFile != null) {
                FileUtils.close(this.zipFile);
            }
            try {
                this.zipFile = new ZipFile(srcFile, encoding);
            } catch (IOException e) {
                throw new BuildException("Problem reading " + srcFile, e);
            }
            StreamUtils.enumerationAsStream(this.zipFile.getEntries()).forEach(entry -> {
                final Resource r = new LiveZipResource(srcFile, zipFile, encoding, entry);
                String name = entry.getName();
                if (entry.isDirectory()) {
                    name = trimSeparator(name);
                    dirEntries.put(name, r);
                    if (match(name)) {
                        matchDirEntries.put(name, r);
                    }
                } else {
                    fileEntries.put(name, r);
                    if (match(name)) {
                        matchFileEntries.put(name, r);
                    }
                }
            });
        }

        @Override
        public void close() {
            FileUtils.close(this.zipFile);
        }
    }

    /**
     * A {@link ZipResource} which holds on to an open {@link ZipFile}. Unlike the
     * {@link ZipResource#getInputStream()}, the {@link #getInputStream()} of this
     * class does <em>not</em> create a new {@code ZipFile} instance and instead
     * reuses the open {@code ZipFile} to return an {@link InputStream} for
     * a particular entry in the zip file, which this {@code ZipResource} represents.
     */
    private static class LiveZipResource extends ZipResource {

        private final ZipFile zipFile;
        private final ZipEntry zipEntry;

        private LiveZipResource(final File file, final ZipFile zipFile, final String encoding, final ZipEntry entry) {
            super(file, encoding, entry);
            this.zipFile = zipFile;
            this.zipEntry = entry;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (isReference()) {
                return getCheckedRef().getInputStream();
            }
            return new FilterInputStream(this.zipFile.getInputStream(this.zipEntry)) {
                public void close() {
                    // close the inputstream
                    FileUtils.close(in);
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
    }
}
