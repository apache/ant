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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.tar.TarConstants;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

/**
 * A Resource representation of an entry in a tar archive.
 * @since Ant 1.7
 */
public class TarResource extends ArchiveResource {

    private String userName = "";
    private String groupName = "";
    private long   uid;
    private long   gid;
    private byte   linkFlag = TarConstants.LF_NORMAL;
    private String linkName = "";

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
    @Override
    public InputStream getInputStream() throws IOException {
        if (isReference()) {
            return getRef().getInputStream();
        }
        Resource archive = getArchive();
        final TarInputStream i = new TarInputStream(archive.getInputStream());
        TarEntry te;
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
    @Override
    public OutputStream getOutputStream() throws IOException {
        if (isReference()) {
            return getRef().getOutputStream();
        }
        throw new UnsupportedOperationException(
            "Use the tar task for tar output.");
    }

    /**
     * @return the user name for the tar entry
     */
    public String getUserName() {
        if (isReference()) {
            return getRef().getUserName();
        }
        checkEntry();
        return userName;
    }

    /**
     * @return the group name for the tar entry
     */
    public String getGroup() {
        if (isReference()) {
            return getRef().getGroup();
        }
        checkEntry();
        return groupName;
    }

    /**
     * @return the uid for the tar entry
     * @since 1.10.4
     */
    public long getLongUid() {
        if (isReference()) {
            return getRef().getLongUid();
        }
        checkEntry();
        return uid;
    }

    /**
     * @return the uid for the tar entry
     */
    @Deprecated
    public int getUid() {
        return (int) getLongUid();
    }

    /**
     * @return the gid for the tar entry
     * @since 1.10.4
     */
    public long getLongGid() {
        if (isReference()) {
            return getRef().getLongGid();
        }
        checkEntry();
        return gid;
    }

    /**
     * @return the uid for the tar entry
     */
    @Deprecated
    public int getGid() {
        return (int) getLongGid();
    }

    /**
     * @return the link "name" (=path) of this entry; an empty string if this is no link
     * @since 1.10.12
     */
    public String getLinkName() {
        return linkName;
    }

    /**
     * @return the link "flag" (=type) of this entry
     * @since 1.10.12
     */
    public byte getLinkFlag() {
        return linkFlag;
    }

    /**
     * fetches information from the named entry inside the archive.
     */
    @Override
    protected void fetchEntry() {
        Resource archive = getArchive();
        try (TarInputStream i = new TarInputStream(archive.getInputStream())) {
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
        }
        setEntry(null);
    }

    @Override
    protected TarResource getRef() {
        return getCheckedRef(TarResource.class);
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
        uid = e.getLongUserId();
        gid = e.getLongGroupId();
        linkName = e.getLinkName();
        linkFlag = e.getLinkFlag();
    }

}
