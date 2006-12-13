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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

/**
 * A Resource representation of an entry in a tar archive.
 * @since Ant 1.7
 */
public class TarResource extends ArchiveResource {

    private String userName = "";
    private String groupName = "";
    private int    uid;
    private int    gid;

    /**
     * Default constructor.
     */
    public TarResource() {
    }

    /**
     * Construct a TarResource representing the specified
     * entry in the specified archive.
     * @param a the archive as File.
     * @param e the TarEntry.
     */
    public TarResource(File a, TarEntry e) {
        super(a, true);
        setEntry(e);
    }

    /**
     * Construct a TarResource representing the specified
     * entry in the specified archive.
     * @param a the archive as Resource.
     * @param e the TarEntry.
     */
    public TarResource(Resource a, TarEntry e) {
        super(a, true);
        setEntry(e);
    }

    /**
     * Return an InputStream for reading the contents of this Resource.
     * @return an InputStream object.
     * @throws IOException if the tar file cannot be opened,
     *         or the entry cannot be read.
     */
    public InputStream getInputStream() throws IOException {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getInputStream();
        }
        Resource archive = getArchive();
        final TarInputStream i = new TarInputStream(archive.getInputStream());
        TarEntry te = null;
        while ((te = i.getNextEntry()) != null) {
            if (te.getName().equals(getName())) {
                return i;
            }
        }

        FileUtils.close(i);
        throw new BuildException("no entry " + getName() + " in "
                                 + getArchive());
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
            "Use the tar task for tar output.");
    }

    /**
     * @return the user name for the tar entry
     */
    public String getUserName() {
        if (isReference()) {
            return ((TarResource) getCheckedRef()).getUserName();
        }
        return userName;
    }

    /**
     * @return the group name for the tar entry
     */
    public String getGroup() {
        if (isReference()) {
            return ((TarResource) getCheckedRef()).getGroup();
        }
        return groupName;
    }

    /**
     * @return the uid for the tar entry
     */
    public int getUid() {
        if (isReference()) {
            return ((TarResource) getCheckedRef()).getUid();
        }
        return uid;
    }

    /**
     * @return the uid for the tar entry
     */
    public int getGid() {
        if (isReference()) {
            return ((TarResource) getCheckedRef()).getGid();
        }
        return uid;
    }

    /**
     * fetches information from the named entry inside the archive.
     */
    protected void fetchEntry() {
        Resource archive = getArchive();
        TarInputStream i = null;
        try {
            i = new TarInputStream(archive.getInputStream());
            TarEntry te = null;
            while ((te = i.getNextEntry()) != null) {
                if (te.getName().equals(getName())) {
                    setEntry(te);
                    return;
                }
            }
        } catch (IOException e) {
            log(e.getMessage(), Project.MSG_DEBUG);
            throw new BuildException(e);
        } finally {
            if (i != null) {
                FileUtils.close(i);
            }
        }
        setEntry(null);
    }

    private void setEntry(TarEntry e) {
        if (e == null) {
            setExists(false);
            return;
        }
        setName(e.getName());
        setExists(true);
        setLastModified(e.getModTime().getTime());
        setDirectory(e.isDirectory());
        setSize(e.getSize());
        setMode(e.getMode());
        userName = e.getUserName();
        groupName = e.getGroupName();
        uid = e.getUserId();
        gid = e.getGroupId();
    }

}
