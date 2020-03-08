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
        ZipScanner zs = new ZipScanner();
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
        String msg = getRefid().getRefId() + " doesn't denote a zipfileset or a fileset";
        throw new BuildException(msg);
    }

    /**
     * A ZipFileset accepts another ZipFileSet or a FileSet as reference
     * FileSets are often used by the war task for the lib attribute
     * @return the abstract fileset instance
     */
    @Override
    protected AbstractFileSet getRef() {
        return getRef(getProject());
    }

    /**
     * Return a ZipFileSet that has the same properties
     * as this one.
     * @return the cloned zipFileSet
     */
    @Override
    public Object clone() {
        if (isReference()) {
            return getRef().clone();
        }
        return super.clone();
    }

}
