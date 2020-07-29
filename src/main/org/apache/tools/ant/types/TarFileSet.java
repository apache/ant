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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * A TarFileSet is a FileSet with extra attributes useful in the context of
 * Tar/Jar tasks.
 *
 * A TarFileSet extends FileSets with the ability to extract a subset of the
 * entries of a Tar file for inclusion in another Tar file.  It also includes
 * a prefix attribute which is prepended to each entry in the output Tar file.
 *
 */
public class TarFileSet extends ArchiveFileSet {

    private boolean userNameSet;
    private boolean groupNameSet;
    private boolean userIdSet;
    private boolean groupIdSet;

    private String userName = "";
    private String groupName = "";
    private int    uid;
    private int    gid;

    /** Constructor for TarFileSet */
    public TarFileSet() {
        super();
    }

    /**
     * Constructor using a fileset argument.
     * @param fileset the fileset to use
     */
    protected TarFileSet(FileSet fileset) {
        super(fileset);
    }

    /**
     * Constructor using a tarfileset argument.
     * @param fileset the tarfileset to use
     */
    protected TarFileSet(TarFileSet fileset) {
        super(fileset);
    }

    /**
     * The username for the tar entry
     * This is not the same as the UID.
     * @param userName the user name for the tar entry.
     */
    public void setUserName(String userName) {
        checkTarFileSetAttributesAllowed();
        userNameSet = true;
        this.userName = userName;
    }

    /**
     * @return the user name for the tar entry
     */
    public String getUserName() {
        if (isReference()) {
            return ((TarFileSet) getRef()).getUserName();
        }
        return userName;
    }

    /**
     * @return whether the user name has been explicitly set.
     */
    public boolean hasUserNameBeenSet() {
        return userNameSet;
    }

    /**
     * The uid for the tar entry
     * This is not the same as the User name.
     * @param uid the id of the user for the tar entry.
     */
    public void setUid(int uid) {
        checkTarFileSetAttributesAllowed();
        userIdSet = true;
        this.uid = uid;
    }

    /**
     * @return the uid for the tar entry
     */
    public int getUid() {
        if (isReference()) {
            return ((TarFileSet) getRef()).getUid();
        }
        return uid;
    }

    /**
     * @return whether the user id has been explicitly set.
     */
    public boolean hasUserIdBeenSet() {
        return userIdSet;
    }

    /**
     * The groupname for the tar entry; optional, default=""
     * This is not the same as the GID.
     * @param groupName the group name string.
     */
    public void setGroup(String groupName) {
        checkTarFileSetAttributesAllowed();
        groupNameSet = true;
        this.groupName = groupName;
    }

    /**
     * @return the group name string.
     */
    public String getGroup() {
        if (isReference()) {
            return ((TarFileSet) getRef()).getGroup();
        }
        return groupName;
    }

    /**
     * @return whether the group name has been explicitly set.
     */
    public boolean hasGroupBeenSet() {
        return groupNameSet;
    }

    /**
     * The GID for the tar entry; optional, default="0"
     * This is not the same as the group name.
     * @param gid the group id.
     */
    public void setGid(int gid) {
        checkTarFileSetAttributesAllowed();
        groupIdSet = true;
        this.gid = gid;
    }

    /**
     * @return the group identifier.
     */
    public int getGid() {
        if (isReference()) {
            return ((TarFileSet) getRef()).getGid();
        }
        return gid;
    }

    /**
     * @return whether the group id has been explicitly set.
     */
    public boolean hasGroupIdBeenSet() {
        return groupIdSet;
    }

    /**
     * Create a new scanner.
     * @return the created scanner.
     */
    @Override
    protected ArchiveScanner newArchiveScanner() {
        TarScanner zs = new TarScanner();
        zs.setEncoding(getEncoding());
        return zs;
    }

    /**
     * Makes this instance in effect a reference to another instance.
     *
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     * @param r the <code>Reference</code> to use.
     * @throws BuildException on error
     */
    @Override
    public void setRefid(Reference r) throws BuildException {
        if (userNameSet || userIdSet || groupNameSet || groupIdSet) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * A TarFileset accepts another TarFileSet or a FileSet as reference
     * FileSets are often used by the war task for the lib attribute
     * @param p the project to use
     * @return the abstract fileset instance
     */
    @Override
    protected AbstractFileSet getRef(Project p) {
        dieOnCircularReference(p);
        Object o = getRefid().getReferencedObject(p);
        if (o instanceof TarFileSet) {
            return (AbstractFileSet) o;
        }
        if (o instanceof FileSet) {
            TarFileSet zfs = new TarFileSet((FileSet) o);
            configureFileSet(zfs);
            return zfs;
        }
        String msg = getRefid().getRefId() + " doesn't denote a tarfileset or a fileset";
        throw new BuildException(msg);
    }

    /**
     * A TarFileset accepts another TarFileSet or a FileSet as reference
     * FileSets are often used by the war task for the lib attribute
     * @return the abstract fileset instance
     */
    @Override
    protected AbstractFileSet getRef() {
        return getRef(getProject());
    }

    /**
     * Configure a fileset based on this fileset.
     * If the fileset is a TarFileSet copy in the tarfileset
     * specific attributes.
     * @param zfs the archive fileset to configure.
     */
    @Override
    protected void configureFileSet(ArchiveFileSet zfs) {
        super.configureFileSet(zfs);
        if (zfs instanceof TarFileSet) {
            TarFileSet tfs = (TarFileSet) zfs;
            tfs.setUserName(userName);
            tfs.setGroup(groupName);
            tfs.setUid(uid);
            tfs.setGid(gid);
        }
    }

    /**
     * Return a TarFileSet that has the same properties
     * as this one.
     * @return the cloned tarFileSet
     */
    @Override
    public Object clone() {
        if (isReference()) {
            return getRef().clone();
        }
        return super.clone();
    }

    /**
     * A check attributes for TarFileSet.
     * If there is a reference, and
     * it is a TarFileSet, the tar fileset attributes
     * cannot be used.
     */
    private void checkTarFileSetAttributesAllowed() {
        if (getProject() == null
            || (isReference()
                && (getRefid().getReferencedObject(
                        getProject())
                    instanceof TarFileSet))) {
            checkAttributesAllowed();
        }
    }

}
