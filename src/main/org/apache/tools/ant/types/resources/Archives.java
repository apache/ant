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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.TarFileSet;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.util.CollectionUtils;

/**
 * A resource collection that treats all nested resources as archives
 * and returns the contents of the archives as its content.
 *
 * @since Ant 1.8.0
 */
public class Archives extends DataType
    implements ResourceCollection, Cloneable {

    private Union zips = new Union();
    private Union tars = new Union();

    /**
     * Wrapper to identify nested resource collections as ZIP
     * archives.
     */
    public Union createZips() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return zips;
    }

    /**
     * Wrapper to identify nested resource collections as ZIP
     * archives.
     */
    public Union createTars() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return tars;
    }

    /**
     * Iterates through the collections and counts.
     */
    public int size() {
        if (isReference()) {
            return ((Archives) getCheckedRef()).size();
        }
        return grabResources().size();
    }

    /**
     * Merges the nested collections.
     */
    public Iterator iterator() {
        if (isReference()) {
            return ((Archives) getCheckedRef()).iterator();
        }
        return grabResources().iterator();
    }

    /**
     * @return false
     */
    public boolean isFilesystemOnly() {
        if (isReference()) {
            return ((MappedResourceCollection) getCheckedRef())
                .isFilesystemOnly();
        }
        return false;
    }

    /**
     * Implement clone.  The nested resource collections are cloned as
     * well.
     * @return a cloned instance.
     */
    public Object clone() {
        try {
            Archives a = (Archives) super.clone();
            a.zips = (Union) zips.clone();
            a.tars = (Union) tars.clone();
            return a;
        } catch (CloneNotSupportedException e) {
            throw new BuildException(e);
        }
    }

    // TODO this is a pretty expensive operation and so the result is
    // cached.
    /**
     * Performs the job by iterating over all archives, turning them
     * into the correct type of ArchiveFileSet and iterating through
     * their contents.
     */
    protected List grabResources() {
        List l = new LinkedList();
        for (Iterator iter = zips.iterator(); iter.hasNext(); ) {
            ZipFileSet zfs = new ZipFileSet();
            zfs.setProject(getProject());
            zfs.setSrcResource((Resource) iter.next());
            l.addAll(CollectionUtils.asCollection(zfs.iterator()));
        }
        for (Iterator iter = tars.iterator(); iter.hasNext(); ) {
            TarFileSet tfs = new TarFileSet();
            tfs.setProject(getProject());
            tfs.setSrcResource((Resource) iter.next());
            l.addAll(CollectionUtils.asCollection(tfs.iterator()));
        }
        return l;
    }

    /**
     * Overrides the version of DataType to recurse on all DataType
     * child elements that may have been added.
     * @param stk the stack of data types to use (recursively).
     * @param p   the project to use to dereference the references.
     * @throws BuildException on error.
     */
    protected synchronized void dieOnCircularReference(Stack stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            stk.push(zips);
            invokeCircularReferenceCheck(zips, stk, p);
            stk.pop();
            stk.push(tars);
            invokeCircularReferenceCheck(tars, stk, p);
            stk.pop();
            setChecked(true);
        }
    }
}