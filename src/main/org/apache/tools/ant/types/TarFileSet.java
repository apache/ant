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
package org.apache.tools.ant.types;

import java.io.File;
import java.util.Iterator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.zip.UnixStat;

/**
 * A TarFileSet is a FileSet with extra attributes useful in the context of
 * Tar/Jar tasks.
 *
 * A TarFileSet extends FileSets with the ability to extract a subset of the
 * entries of a Tar file for inclusion in another Tar file.  It also includes
 * a prefix attribute which is prepended to each entry in the output Tar file.
 *
 * Since ant 1.6 TarFileSet can be defined with an id and referenced in packaging tasks
 *
 */
public class TarFileSet extends ArchiveFileSet {

    /** Constructor for TarFileSet */
    public TarFileSet() {
        super();
    }

    /**
     * Constructor using a fileset arguement.
     * @param fileset the fileset to use
     */
    protected TarFileSet(FileSet fileset) {
        super(fileset);
    }

    /**
     * Constructor using a tarfileset arguement.
     * @param fileset the tarfileset to use
     */
    protected TarFileSet(TarFileSet fileset) {
        super(fileset);
    }

    protected ArchiveScanner newArchiveScanner() {
        TarScanner zs = new TarScanner();
        return zs;
    }

    /**
     * A TarFileset accepts another TarFileSet or a FileSet as reference
     * FileSets are often used by the war task for the lib attribute
     * @param p the project to use
     * @return the abstract fileset instance
     */
    protected AbstractFileSet getRef(Project p) {
        dieOnCircularReference(p);
        Object o = getRefid().getReferencedObject(p);
        if (o instanceof TarFileSet) {
            return (AbstractFileSet) o;
        } else if (o instanceof FileSet) {
            TarFileSet zfs = new TarFileSet((FileSet) o);
            configureFileSet(zfs);
            return zfs;
        } else {
            String msg = getRefid().getRefId() + " doesn\'t denote a tarfileset or a fileset";
            throw new BuildException(msg);
        }
    }

    /**
     * Return a TarFileSet that has the same properties
     * as this one.
     * @return the cloned tarFileSet
     */
    public Object clone() {
        if (isReference()) {
            return ((TarFileSet) getRef(getProject())).clone();
        } else {
            return super.clone();
        }
    }
}
