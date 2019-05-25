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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ArchiveFileSet;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.TarFileSet;
import org.apache.tools.ant.types.ZipFileSet;

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
     *
     * @return Union
     */
    public Union createZips() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        setChecked(false);
        return zips;
    }

    /**
     * Wrapper to identify nested resource collections as ZIP
     * archives.
     *
     * @return Union
     */
    public Union createTars() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        setChecked(false);
        return tars;
    }

    /**
     * Sums the sizes of nested archives.
     *
     * @return int
     */
    @Override
    public int size() {
        if (isReference()) {
            return getRef().size();
        }
        dieOnCircularReference();
        return streamArchives().mapToInt(ArchiveFileSet::size).sum();
    }

    /**
     * Merges the nested collections.
     *
     * @return Iterator&lt;Resource&gt;
     */
    public Iterator<Resource> iterator() {
        if (isReference()) {
            return getRef().iterator();
        }
        dieOnCircularReference();
        return streamArchives().flatMap(ResourceCollection::stream)
            .map(Resource.class::cast).iterator();
    }

    /**
     * @return false
     */
    public boolean isFilesystemOnly() {
        if (isReference()) {
            return getRef().isFilesystemOnly();
        }
        dieOnCircularReference();
        // TODO check each archive in turn?
        return false;
    }

    /**
     * Overrides the base version.
     *
     * @param r the Reference to set.
     */
    @Override
    public void setRefid(final Reference r) {
        if (!zips.getResourceCollections().isEmpty() || !tars.getResourceCollections().isEmpty()) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Implement clone.  The nested resource collections are cloned as
     * well.
     *
     * @return a cloned instance.
     */
    @Override
    public Object clone() {
        try {
            final Archives a = (Archives) super.clone();
            a.zips = (Union) zips.clone();
            a.tars = (Union) tars.clone();
            return a;
        } catch (final CloneNotSupportedException e) {
            throw new BuildException(e);
        }
    }

    // TODO this is a pretty expensive operation and so the result
    // should be cached.
    /**
     * Turns all nested resources into corresponding ArchiveFileSets
     * and returns an iterator over the collected archives.
     *
     * @return Iterator&lt;ArchiveFileSet&gt;
     */
    protected Iterator<ArchiveFileSet> grabArchives() {
        return streamArchives().iterator();
    }

    // TODO this is a pretty expensive operation and so the result
    // should be cached.
    private Stream<ArchiveFileSet> streamArchives() {
        final List<ArchiveFileSet> l = new LinkedList<>();
        for (final Resource r : zips) {
            l.add(configureArchive(new ZipFileSet(), r));
        }
        for (final Resource r : tars) {
            l.add(configureArchive(new TarFileSet(), r));
        }
        return l.stream();
    }

    /**
     * Configures the archivefileset based on this type's settings,
     * set the source.
     *
     * @param afs ArchiveFileSet
     * @param src Resource
     * @return ArchiveFileSet
     */
    protected ArchiveFileSet configureArchive(final ArchiveFileSet afs,
                                              final Resource src) {
        afs.setProject(getProject());
        afs.setSrcResource(src);
        return afs;
    }

    /**
     * Overrides the version of DataType to recurse on all DataType
     * child elements that may have been added.
     *
     * @param stk the stack of data types to use (recursively).
     * @param p   the project to use to dereference the references.
     * @throws BuildException on error.
     */
    @Override
    protected synchronized void dieOnCircularReference(final Stack<Object> stk, final Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            pushAndInvokeCircularReferenceCheck(zips, stk, p);
            pushAndInvokeCircularReferenceCheck(tars, stk, p);
            setChecked(true);
        }
    }

    private Archives getRef() {
        return getCheckedRef(Archives.class);
    }

}
