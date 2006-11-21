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

    private String encoding = null;

    /** Constructor for ZipFileSet */
    public ZipFileSet() {
        super();
    }

    /**
     * Constructor using a fileset arguement.
     * @param fileset the fileset to use
     */
    protected ZipFileSet(FileSet fileset) {
        super(fileset);
    }

    /**
     * Constructor using a zipfileset arguement.
     * @param fileset the zipfileset to use
     */
    protected ZipFileSet(ZipFileSet fileset) {
        super(fileset);
        encoding = fileset.encoding;
    }

    /**
     * Set the encoding used for this ZipFileSet.
     * @param enc encoding as String.
     * @since Ant 1.7
     */
    public void setEncoding(String enc) {
        checkZipFileSetAttributesAllowed();
        this.encoding = enc;
    }

    /**
     * Get the encoding used for this ZipFileSet.
     * @return String encoding.
     * @since Ant 1.7
     */
    public String getEncoding() {
        if (isReference()) {
            AbstractFileSet ref = getRef(getProject());
            if (ref instanceof ZipFileSet) {
                return ((ZipFileSet) ref).getEncoding();
            } else {
                return null;
            }
        }
        return encoding;
    }

    /**
     * Return a new archive scanner based on this one.
     * @return a new ZipScanner with the same encoding as this one.
     */
    protected ArchiveScanner newArchiveScanner() {
        ZipScanner zs = new ZipScanner();
        zs.setEncoding(encoding);
        return zs;
    }

    /**
     * A ZipFileset accepts another ZipFileSet or a FileSet as reference
     * FileSets are often used by the war task for the lib attribute
     * @param p the project to use
     * @return the abstract fileset instance
     */
    protected AbstractFileSet getRef(Project p) {
        dieOnCircularReference(p);
        Object o = getRefid().getReferencedObject(p);
        if (o instanceof ZipFileSet) {
            return (AbstractFileSet) o;
        } else if (o instanceof FileSet) {
            ZipFileSet zfs = new ZipFileSet((FileSet) o);
            configureFileSet(zfs);
            return zfs;
        } else {
            String msg = getRefid().getRefId() + " doesn\'t denote a zipfileset or a fileset";
            throw new BuildException(msg);
        }
    }

    /**
     * Return a ZipFileSet that has the same properties
     * as this one.
     * @return the cloned zipFileSet
     */
    public Object clone() {
        if (isReference()) {
            return ((ZipFileSet) getRef(getProject())).clone();
        } else {
            return super.clone();
        }
    }

    /**
     * A check attributes for zipFileSet.
     * If there is a reference, and
     * it is a ZipFileSet, the zip fileset attributes
     * cannot be used.
     */
    private void checkZipFileSetAttributesAllowed() {
        if (getProject() == null
            || (isReference()
                && (getRefid().getReferencedObject(
                        getProject())
                    instanceof ZipFileSet))) {
            checkAttributesAllowed();
        }
    }

}
